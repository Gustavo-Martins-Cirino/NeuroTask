with open('src/main/resources/static/js/app.js', 'r', encoding='utf-8') as f:
    lines = f.readlines()
    print(f"Total lines: {len(lines)}")
    # print some key lines or imports
    for i, line in enumerate(lines[:30]):
        if 'import' in line:
            print(f"L{i+1}: {line.strip()}")
