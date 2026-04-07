def update_colors(file_path):
    with open(file_path, 'r', encoding='utf-8') as f:
        content = f.read()

    color_map = {
        '#ea4335': '#ffb3ba', # Vermelho
        '#fbbc04': '#ffdfba', # Amarelo
        '#34a853': '#baffc9', # Verde
        '#1a73e8': '#bae1ff', # Azul
        '#9334e6': '#e5c8fa', # Roxo
        '#e67c73': '#ffc4a3', # Coral
        '#33b679': '#a3eedb', # Turquesa
        '#8e8e93': '#dcdcdc', # Cinza
    }

    for old, new in color_map.items():
        content = content.replace(old, new)
        content = content.replace(old.upper(), new.upper())

    with open(file_path, 'w', encoding='utf-8') as f:
        f.write(content)

update_colors('src/main/resources/static/index.html')
update_colors('src/main/resources/static/js/modules/calendarView.js')
print("Cores aplicadas com sucesso")
