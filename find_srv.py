import re

with open('src/main/resources/static/js/api/taskService.js', 'r', encoding='utf-8') as f:
    js = f.read()

for line in js.splitlines():
    if 'taskService' in line:
        print(line.strip())
