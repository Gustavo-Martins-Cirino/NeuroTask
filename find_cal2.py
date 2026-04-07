import re
with open('src/main/resources/static/index.html', 'r', encoding='utf-8') as f:
    lines = f.readlines()

out = []
for i in range(23800, min(24250, len(lines))):
    if 'calendar' in lines[i].lower() or 'timeline' in lines[i].lower():
        out.append(f"{i+1}: {lines[i]}")

# Wait, the line numbers above were around 3600. Let's just output around 1490 and 2620
out2 = lines[1485:1500]

with open('html_cal2.txt', 'w', encoding='utf-8') as f:
    f.writelines(out)
    f.writelines(out2)
