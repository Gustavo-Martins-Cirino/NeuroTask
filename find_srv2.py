import re

with open('src/main/resources/static/js/api/taskService.js', 'r', encoding='utf-8') as f:
    js = f.read()

out = []
for line in js.splitlines():
    if 'taskService' in line:
        out.append(line.strip())

with open('task_srv.txt', 'w', encoding='utf-8') as f:
    f.write("\n".join(out))
