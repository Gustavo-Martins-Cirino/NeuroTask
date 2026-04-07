try:
    with open('src/main/resources/static/js/app.js', 'r', encoding='utf-8') as f:
        lines = f.readlines()
    
    with open('app_info.txt', 'w', encoding='utf-8') as out:
        out.write(f"Total lines: {len(lines)}\n")
        out.write("Imports / setup:\n")
        for i, line in enumerate(lines[:100]):
            if 'import ' in line or 'fetch' in line or 'EventL' in line:
                out.write(f"L{i+1}: {line.strip()}\n")
except Exception as e:
    with open('app_info.txt', 'w', encoding='utf-8') as out:
        out.write(str(e))
