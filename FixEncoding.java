import java.io.*;
import java.nio.*;
import java.nio.charset.*;
import java.nio.file.*;

/**
 * Corrige mojibake no index.html.
 * O problema: bytes UTF-8 foram decodificados incorretamente como cp1252,
 * resultando em sequencias como "Ã©" no lugar de "e com acento" e "ðŸ'¡" no lugar de emojis.
 * Solucao: para cada span de caracteres "suspeitos" (range alto de Latin-1 + especiais cp1252),
 * tenta codificar como cp1252 e decodificar como UTF-8.
 */
public class FixEncoding {

    static final Charset UTF8   = StandardCharsets.UTF_8;
    static final Charset LATIN1 = StandardCharsets.ISO_8859_1;
    static Charset CP1252;

    static {
        try {
            CP1252 = Charset.forName("windows-1252");
        } catch (Exception e) {
            CP1252 = LATIN1;
        }
    }

    public static void main(String[] args) throws Exception {
        String path = args.length > 0 ? args[0]
                : "src/main/resources/static/index.html";
        Path filePath = Paths.get(path);

        String original = Files.readString(filePath, UTF8);
        String fixed    = fixMojibake(original);

        long changed = 0;
        for (int i = 0; i < Math.min(original.length(), fixed.length()); i++) {
            if (original.charAt(i) != fixed.charAt(i)) changed++;
        }
        changed += Math.abs(original.length() - fixed.length());

        Files.writeString(filePath, fixed, UTF8);

        // Verifica residuos
        long residual = countResidual(fixed);

        System.out.println("Concluido!");
        System.out.println("  Caracteres alterados : " + changed);
        System.out.println("  Chars Latin-ext rest.: " + residual);
    }

    static String fixMojibake(String input) {
        StringBuilder result = new StringBuilder(input.length());
        StringBuilder span   = new StringBuilder();

        for (int i = 0; i < input.length(); ) {
            char c  = input.charAt(i);
            int  cp = Character.codePointAt(input, i);

            if (isHighByte(c)) {
                span.append(c);
                i++;
            } else {
                if (span.length() > 0) {
                    result.append(fixSpan(span.toString()));
                    span.setLength(0);
                }
                result.appendCodePoint(cp);
                i += Character.charCount(cp);
            }
        }
        if (span.length() > 0) {
            result.append(fixSpan(span.toString()));
        }
        return result.toString();
    }

    /**
     * Caracteres que podem ser bytes altos de cp1252 mal-decodificados.
     * Inclui: Latin-1 range 0x80-0xFF + especiais cp1252 (0x80-0x9F mapeados).
     */
    static boolean isHighByte(char c) {
        if (c >= 0x80 && c <= 0xFF) return true;
        // cp1252 specials (bytes 0x80-0x9F)
        if (c == 0x20AC) return true; // EUR sign  (0x80)
        if (c == 0x201A) return true; // ,, quote  (0x82)
        if (c == 0x0192) return true; // f hook    (0x83)
        if (c == 0x201E) return true; // ,, dquote (0x84)
        if (c == 0x2026) return true; // ellipsis  (0x85)
        if (c == 0x2020) return true; // dagger    (0x86)
        if (c == 0x2021) return true; // ddagger   (0x87)
        if (c == 0x02C6) return true; // circ      (0x88)
        if (c == 0x2030) return true; // permille  (0x89)
        if (c == 0x0160) return true; // S caron   (0x8A)
        if (c == 0x2039) return true; // < angle   (0x8B)
        if (c == 0x0152) return true; // OE        (0x8C)
        if (c == 0x017D) return true; // Z caron   (0x8E)
        if (c == 0x2018) return true; // left '    (0x91)
        if (c == 0x2019) return true; // right '   (0x92)
        if (c == 0x201C) return true; // left "    (0x93)
        if (c == 0x201D) return true; // right "   (0x94)
        if (c == 0x2022) return true; // bullet    (0x95)
        if (c == 0x2013) return true; // en dash   (0x96)
        if (c == 0x2014) return true; // em dash   (0x97)
        if (c == 0x02DC) return true; // tilde     (0x98)
        if (c == 0x2122) return true; // TM        (0x99)
        if (c == 0x0161) return true; // s caron   (0x9A)
        if (c == 0x203A) return true; // > angle   (0x9B)
        if (c == 0x0153) return true; // oe        (0x9C)
        if (c == 0x017E) return true; // z caron   (0x9E)
        if (c == 0x0178) return true; // Y umlaut  (0x9F) - chave para emojis!
        return false;
    }

    static String fixSpan(String span) {
        // Tenta cp1252 -> UTF-8
        try {
            byte[]  bytes   = span.getBytes(CP1252);
            CharsetDecoder dec = UTF8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT);
            return dec.decode(ByteBuffer.wrap(bytes)).toString();
        } catch (Exception ignored) {}

        // Fallback: Latin-1 -> UTF-8
        try {
            byte[]  bytes   = span.getBytes(LATIN1);
            CharsetDecoder dec = UTF8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT);
            return dec.decode(ByteBuffer.wrap(bytes)).toString();
        } catch (Exception ignored) {}

        // Nao conseguiu corrigir, retorna original
        return span;
    }

    static long countResidual(String s) {
        long count = 0;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c >= 0x00C0 && c <= 0x00FF) count++;    // Latin Extended-A common mojibake range
            if (c == 0x0178 || c == 0x2019 || c == 0x201C || c == 0x201D) count++;
        }
        return count;
    }
}
