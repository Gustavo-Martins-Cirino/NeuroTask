import sys

html_path = 'src/main/resources/static/index.html'
js_path = 'src/main/resources/static/js/inline_fix.js'
log_path = 'extract_log.txt'

try:
    with open(html_path, 'r', encoding='utf-8') as f:
        lines = f.readlines()

    start_idx = 24276 - 1
    end_idx = 24347 - 1

    with open(log_path, 'w', encoding='utf-8') as log:
        if "<script" in lines[start_idx]:
            js_content = "".join(lines[start_idx + 1: end_idx])
            
            with open(js_path, 'w', encoding='utf-8') as f:
                f.write(js_content)
                
            lines[start_idx] = '    <script src="/js/inline_fix.js"></script>\n'
            # remove the original script body and closing tag
            del lines[start_idx + 1: end_idx + 1]
            
            with open(html_path, 'w', encoding='utf-8') as f:
                f.writelines(lines)
            log.write("Script extracted and replaced successfully.\n")
        else:
            log.write("Mismatch in lines!! Line is: " + lines[start_idx] + "\n")
except Exception as e:
    with open(log_path, 'w', encoding='utf-8') as log:
        log.write(str(e))
