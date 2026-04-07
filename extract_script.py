import re

html_path = 'src/main/resources/static/index.html'
js_path = 'src/main/resources/static/js/inline_fix.js'

with open(html_path, 'r', encoding='utf-8') as f:
    lines = f.readlines()

# Extract from 24276 to 24347
start_idx = 24276 - 1
end_idx = 24347 - 1

if "<script>\n" in lines[start_idx] or "<script>" in lines[start_idx]:
    js_content = "".join(lines[start_idx + 1: end_idx])
    
    with open(js_path, 'w', encoding='utf-8') as f:
        f.write(js_content)
        
    lines[start_idx] = '    <script src="/js/inline_fix.js"></script>\n'
    # remove the original script body and closing tag
    del lines[start_idx + 1: end_idx + 1]
    
    with open(html_path, 'w', encoding='utf-8') as f:
        f.writelines(lines)
    print("Script extracted and replaced.")
else:
    print("Mismatch in lines!!")
