"""
Correcao definitiva de mojibake: encoder hibrido cp1252 + latin-1.
Resolve spans mixtos que falharam nos scripts anteriores.
"""
path = r'src\main\resources\static\index.html'

# Monta mapa invertido: codepoint unicode -> byte cp1252
cp1252_to_byte = {}
for byte_val in range(256):
    try:
        ch = bytes([byte_val]).decode('cp1252')
        cp1252_to_byte[ord(ch)] = byte_val
    except Exception:
        # Bytes indefinidos no cp1252 (0x81,0x8D,0x8F,0x90,0x9D): usa o proprio valor
        cp1252_to_byte[byte_val] = byte_val

def encode_hybrid(span):
    """Codifica cada char usando cp1252 se possivel, senao usa ord() se < 256."""
    buf = bytearray()
    for ch in span:
        o = ord(ch)
        if o in cp1252_to_byte:
            buf.append(cp1252_to_byte[o])
        elif o < 256:
            buf.append(o)
        else:
            raise ValueError('fora do range: U+{:04X}'.format(o))
    return bytes(buf)

with open(path, 'r', encoding='utf-8') as f:
    content = f.read()

before = len(content)
result = []
i = 0
fixed = 0
kept = 0

while i < len(content):
    if ord(content[i]) < 128:
        result.append(content[i])
        i += 1
    else:
        j = i
        while j < len(content) and ord(content[j]) >= 128:
            j += 1
        span = content[i:j]

        new_span = None
        try:
            raw = encode_hybrid(span)
            decoded = raw.decode('utf-8')
            # Verifica se realmente mudou algo (evita noop)
            if decoded != span:
                new_span = decoded
                fixed += 1
        except Exception:
            pass

        result.append(new_span if new_span is not None else span)
        if new_span is None:
            kept += 1
        i = j

out = ''.join(result)

with open(path, 'w', encoding='utf-8') as f:
    f.write(out)

# Verifica residual
residual = sum(1 for c in out if ord(c) in range(0xC0, 0x100) and
               out[out.index(c)-1:out.index(c)] not in ('<', '>', ' ', '"', "'"))
print('Spans corrigidos:', fixed)
print('Spans mantidos (ja corretos / mistos):', kept)
print('Chars antes:', before, '-> depois:', len(out))

# Conta mojibake residual especifico
import re
still_broken = len(re.findall(r'[\xc3][\xa1-\xbf\x80-\xa0]|[\xf0][\x80-\xbf\u0178]', out))
print('Mojibake residual detectado:', still_broken)
