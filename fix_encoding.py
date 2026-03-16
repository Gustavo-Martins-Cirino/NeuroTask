import sys

path = r'src\main\resources\static\index.html'
with open(path, 'r', encoding='utf-8') as f:
    content = f.read()

before = len(content)
result = []
i = 0
fixed_spans = 0

while i < len(content):
    if ord(content[i]) < 128:
        result.append(content[i])
        i += 1
    else:
        j = i
        while j < len(content) and ord(content[j]) >= 128:
            j += 1
        span = content[i:j]
        fixed = None
        try:
            fixed = span.encode('cp1252').decode('utf-8')
            fixed_spans += 1
        except Exception:
            pass
        if fixed is None:
            try:
                fixed = span.encode('latin-1').decode('utf-8')
                fixed_spans += 1
            except Exception:
                fixed = span
        result.append(fixed)
        i = j

out = ''.join(result)
with open(path, 'w', encoding='utf-8') as f:
    f.write(out)

remaining = sum(1 for c in out if 0xC0 <= ord(c) <= 0xFF)
print('Fixed spans:', fixed_spans)
print('Remaining latin-ext chars (expect 0):', remaining)
print('Chars before:', before, '-> after:', len(out))
