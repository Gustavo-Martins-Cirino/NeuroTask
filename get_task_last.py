with open('src/main/resources/static/js/api/taskService.js', 'r', encoding='utf-8') as f:
    lines = f.readlines()

with open('task_last.txt', 'w', encoding='utf-8') as out:
    for line in lines[-30:]:
        out.write(line)
