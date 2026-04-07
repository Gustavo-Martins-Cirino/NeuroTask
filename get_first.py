with open('src/main/resources/static/js/modules/calendarView.js', 'r', encoding='utf-8') as f:
    lines = f.readlines()

with open('calendar_first.txt', 'w', encoding='utf-8') as out:
    for line in lines[40:60]:
        out.write(line)
