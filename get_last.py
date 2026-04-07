with open('src/main/resources/static/js/modules/calendarView.js', 'r', encoding='utf-8') as f:
    lines = f.readlines()

with open('taskService_last.txt', 'w', encoding='utf-8') as out:
    for line in lines[-20:]:
        out.write(line)
