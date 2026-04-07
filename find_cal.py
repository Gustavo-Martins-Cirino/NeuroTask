import re

with open('src/main/resources/static/index.html', 'r', encoding='utf-8') as f:
    html = f.read()

out = []
for i, line in enumerate(html.splitlines()):
    if 'calendar' in line.lower() or 'day-view' in line.lower() or 'time-blocking' in line.lower():
        out.append(f"{i+1}: {line.strip()}")

with open('html_cal.txt', 'w', encoding='utf-8') as f:
    f.write("\n".join(out))
