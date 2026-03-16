"""
Corrige mojibake em index.html.
Estrategia: para cada span nao-ASCII, tenta multiplas combinacoes
de encodings ate obter UTF-8 valido.
"""
import re, sys

path = r'src\main\resources\static\index.html'
with open(path, 'r', encoding='utf-8') as f:
    content = f.read()

before = len(content)
fixed_spans = 0
kept_spans = 0


def try_fix(span):
    """Tenta reverter mojibake em um span nao-ASCII."""
    global fixed_spans, kept_spans

    # Estrategia 1: todos os chars < 256 -> tenta latin-1 PRIMEIRO (mais seguro para emojis)
    if all(ord(c) < 256 for c in span):
        try:
            b = bytes(ord(c) for c in span)
            decoded = b.decode('utf-8')
            fixed_spans += 1
            return decoded
        except Exception:
            pass

    # Estrategia 2: tenta cp1252 (para aspas tipograficas, travessao, etc.)
    try:
        b = span.encode('cp1252')
        decoded = b.decode('utf-8')
        fixed_spans += 1
        return decoded
    except Exception:
        pass

    # Estrategia 3: misto - chars < 256 via latin-1, resto mantido
    # Para spans com mistura de latim e unicode (ex: emoji correto + texto mojibake)
    # Nao tenta corrigir - pode estragar emojis ja corretos
    kept_spans += 1
    return span


result = []
i = 0

while i < len(content):
    if ord(content[i]) < 128:
        result.append(content[i])
        i += 1
    else:
        j = i
        while j < len(content) and ord(content[j]) >= 128:
            j += 1
        span = content[i:j]
        result.append(try_fix(span))
        i = j

out = ''.join(result)

with open(path, 'w', encoding='utf-8') as f:
    f.write(out)

print('Fixed spans:', fixed_spans)
print('Kept as-is (mixed/unfixable):', kept_spans)
print('Chars before:', before, '-> after:', len(out))

# Verificar emojis ainda quebrados
broken = [(m.start(), m.group()) for m in re.finditer(r'\xf0[\x80-\xbf\u0178\u0192-\u02ff]', out)]
print('Emojis ainda quebrados:', len(broken))
if broken:
    for pos, s in broken[:5]:
        ctx = out[max(0,pos-20):pos+40]
        print('  Sample:', repr(ctx))
