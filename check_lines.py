import sys

try:
    with open('src/main/resources/static/index.html', 'r', encoding='utf-8') as f:
        lines = f.readlines()
        
    with open('lines_output.txt', 'w', encoding='utf-8') as out:
        out.write(f"Total lines: {len(lines)}\n")
        
        out.write("\nCONTEXT 24270-24360:\n")
        for idx in range(24269, 24360):
            if 0 <= idx < len(lines):
                out.write(f"{idx+1}: {lines[idx]}")
except Exception as e:
    with open('lines_output.txt', 'w', encoding='utf-8') as out:
        out.write(str(e))
