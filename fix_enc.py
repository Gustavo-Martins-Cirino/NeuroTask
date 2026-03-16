import re, sys

FILE = "src/main/resources/static/index.html"

with open(FILE, "r", encoding="utf-8") as f:
    content = f.read()

print("Loaded %d chars" % len(content))

def fix_latin1(text):
    count = [0]
    def rep(m):
        s = m.group(0)
        try:
            r = s.encode("latin-1").decode("utf-8")
            if r != s:
                count[0] += 1
                return r
        except Exception:
            pass
        return s

    # Pass 1: Ã + 1 char (2-byte UTF-8 sequences for Latin Extended chars)
    t = re.sub(r"\xc3[\x80-\xbf]", rep, text)
    # Pass 2: each 3-byte UTF-8 sequence that got mangled as 3 latin-1 chars
    t = re.sub(r"\xe2[\x80-\xbf][\x80-\xbf]", rep, t)
    # Pass 3: 4-byte emoji sequences (ð + 3 more bytes)
    t = re.sub(r"\xf0[\x9f-\xbf][\x80-\xbf][\x80-\xbf]", rep, t)
    # Pass 4: other 3-byte sequences starting with \xe2
    t = re.sub(r"\xe2[\x80-\xbf][\x80-\xbf]", rep, t)
    print("Fixed %d sequences" % count[0])
    return t

content = fix_latin1(content)

count_remaining = len(re.findall(r"Ã[^\s]|â€.", content))
print("Estimated remaining patterns: ~%d" % count_remaining)

with open(FILE, "w", encoding="utf-8") as f:
    f.write(content)

print("Saved.")
