with open('src/main/resources/static/js/app.js', 'r', encoding='utf-8') as f:
    lines = f.readlines()

with open('app_search.txt', 'w', encoding='utf-8') as f:
    for line in lines[200:300]:
        f.write(line)
