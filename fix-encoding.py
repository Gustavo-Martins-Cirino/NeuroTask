#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Fix mojibake in index.html - converts incorrectly decoded UTF-8 back to proper chars"""

import re

FILE = "src/main/resources/static/index.html"

# Read file as bytes, then decode as UTF-8
with open(FILE, "r", encoding="utf-8") as f:
    content = f.read()

print(f"Loaded {len(content)} chars")

# Strategy: fix latin1-reinterpreted UTF-8 sequences
# These patterns appear because UTF-8 bytes were read as if they were latin-1
# e.g. "é" (UTF-8: 0xC3 0xA9) was read as "Ã©" (latin-1 for 0xC3=Ã, 0xA9=©)

REPLACEMENTS = [
    # ---- Portuguese lower case accented ----
    ("Ã¡", "á"),
    ("Ã©", "é"),
    ("Ãª", "ê"),
    ("Ã£", "ã"),
    ("Ã³", "ó"),
    ("Ã§", "ç"),
    ("Ã­", "í"),
    ("Ãµ", "õ"),
    ("Ãº", "ú"),
    ("Ã¢", "â"),
    ("Ã´", "ô"),
    ("Ã®", "î"),
    ("Ã ", "à"),

    # ---- Portuguese upper case accented ----
    ("Ã‰", "É"),
    ("ÃŠ", "Ê"),
    ("Ã‡", "Ç"),
    ("Ãš", "Ú"),
    ("Ã•", "Õ"),
    ("Ã‚", "Â"),
    ("Ã€", "À"),
    ("Ã"", "Ó"),
    ("Ã­", "í"),

    # ---- Ã alone (but only as the standalone letter Ã, not a prefix) ----
    # We do this last to avoid double-replacing already-fixed sequences
    # ("Ãƒ", "Ã"),  # skip - rarely needed

    # ---- Typographic quotes ----
    ("\u00e2\u0080\u009c", "\u201c"),  # â€œ -> "
    ("\u00e2\u0080\u009d", "\u201d"),  # â€ -> "
    ("\u00e2\u0080\u0099", "\u2019"),  # â€™ -> '
    ("\u00e2\u0080\u0098", "\u2018"),  # â€˜ -> '
    ("\u00e2\u0080\u00a6", "\u2026"),  # â€¦ -> …
    ("\u00e2\u0080\u00a2", "\u2022"),  # â€¢ -> •
    ("\u00e2\u0080\u0093", "\u2013"),  # â€" -> –
    ("\u00e2\u0080\u0094", "\u2014"),  # â€" -> —

    # ---- Arrows and symbols ----
    ("\u00e2\u009c\u0093", "✓"),
    ("\u00e2\u009c\u0085", "✅"),
    ("\u00e2\u009c\u0096", "✖"),
    ("\u00e2\u009c\u00a8", "✨"),
    ("\u00e2\u008c\u0083", "❌"),
    ("\u00e2\u009a\u00a1", "⚡"),
    ("\u00e2\u009a\u00a0\u00ef\u00b8\u008f", "⚠️"),
    ("\u00e2\u009a\u00a0", "⚠"),
    ("\u00e2\u00ad\u0090", "⭐"),
    ("\u00e2\u0086\u0093", "↓"),
    ("\u00e2\u0086\u0092", "→"),
    ("\u00e2\u0086\u0091", "↑"),
    ("\u00e2\u0086\u0094", "↔"),

    # ---- Common emojis (4-byte emoji sequences corrupted) ----
    # These appear as ð (0xF0 latin-1) + 3 more bytes
    # The pattern is: ðŸXX where XX are latin-1 chars
    ("\u00f0\u009f\u0092\u00a1", "💡"),
    ("\u00f0\u009f\u008e\u00af", "🎯"),
    ("\u00f0\u009f\u0093\u008a", "📊"),
    ("\u00f0\u009f\u00a7\u00a0", "🧠"),
    ("\u00f0\u009f\u0094\u00a5", "🔥"),
    ("\u00f0\u009f\u008c\u0099", "🌙"),
    ("\u00f0\u009f\u008e\u00ae", "🎮"),
    ("\u00f0\u009f\u008f\u0086", "🏆"),
    ("\u00f0\u009f\u0092\u00aa", "💪"),
    ("\u00f0\u009f\u008e\u0089", "🎉"),
    ("\u00f0\u009f\u0091\u008d", "👍"),
    ("\u00f0\u009f\u0091\u0088", "👈"),
    ("\u00f0\u009f\u0091\u0089", "👉"),
    ("\u00f0\u009f\u0091\u0086", "👆"),
    ("\u00f0\u009f\u0091\u0087", "👇"),
    ("\u00f0\u009f\u0091\u008a", "👊"),
    ("\u00f0\u009f\u0093\u0085", "📅"),
    ("\u00f0\u009f\u0094\u008e", "🔎"),
    ("\u00f0\u009f\u0094\u008d", "🔍"),
    ("\u00f0\u009f\u0093\u0088", "📈"),
    ("\u00f0\u009f\u0093\u0089", "📉"),
    ("\u00f0\u009f\u0093\u008b", "📋"),
    ("\u00f0\u009f\u0098\u0093", "😓"),
    ("\u00f0\u009f\u0098\u008a", "😊"),
    ("\u00f0\u009f\u0098\u0085", "😅"),
    ("\u00f0\u009f\u0098\u0084", "😄"),
    ("\u00f0\u009f\u0098\u0080", "😀"),
    ("\u00f0\u009f\u0098\u0088", "😈"),
    ("\u00f0\u009f\u0098\u00b1", "😱"),
    ("\u00f0\u009f\u0098\u00ac", "😬"),
    ("\u00f0\u009f\u0098\u00a4", "😤"),
    ("\u00f0\u009f\u008c\u009f", "🌟"),
    ("\u00f0\u009f\u008c\u0088", "🌈"),
    ("\u00f0\u009f\u008c\u00b4", "🌴"),
    ("\u00f0\u009f\u008c\u008a", "🌊"),
    ("\u00f0\u009f\u008c\u008c", "🌌"),
    ("\u00f0\u009f\u008c\u0085", "🌅"),
    ("\u00f0\u009f\u008c\u0086", "🌆"),
    ("\u00f0\u009f\u008c\u00b1", "🌱"),
    ("\u00f0\u009f\u008c\u00b3", "🌳"),
    ("\u00f0\u009f\u0094\u0084", "🔄"),
    ("\u00f0\u009f\u0092\u00bb", "💻"),
    ("\u00f0\u009f\u0093\u009d", "📝"),
    ("\u00f0\u009f\u0092\u00ac", "💬"),
    ("\u00f0\u009f\u0093\u008c", "📌"),
    ("\u00f0\u009f\u0093\u0096", "📖"),
    ("\u00f0\u009f\u0099\u008f", "🙏"),
    ("\u00f0\u009f\u00a7\u0098", "🧘"),
    ("\u00f0\u009f\u00a4\u0096", "🤖"),
    ("\u00f0\u009f\u00a4\u0094", "🤔"),
    ("\u00f0\u009f\u0092\u008e", "💎"),
    ("\u00f0\u009f\u0092\u0080", "💀"),
    ("\u00f0\u009f\u0092\u00bd", "👽"),
    ("\u00f0\u009f\u0092\u0096", "💖"),
    ("\u00f0\u009f\u0092\u0097", "💗"),
    ("\u00f0\u009f\u0092\u0098", "💘"),
    ("\u00f0\u009f\u009a\u0080", "🚀"),
    ("\u00f0\u009f\u008e\u0088", "🎈"),
    ("\u00f0\u009f\u008e\u00b5", "🎵"),
    ("\u00f0\u009f\u008e\u0096", "🎖"),
    ("\u00f0\u009f\u008e\u0093", "🎓"),
    ("\u00f0\u009f\u008f\u0085", "🏅"),
    ("\u00f0\u009f\u0092\u0090", "💐"),
    ("\u00f0\u009f\u008e\u00a4", "🎤"),
    ("\u00f0\u009f\u0097\u0093", "🗓"),
    ("\u00f0\u009f\u0093\u00a3", "📣"),
    ("\u00f0\u009f\u0093\u009e", "📞"),
    ("\u00f0\u009f\u0094\u0094", "🔔"),
    ("\u00f0\u009f\u0094\u00a0", "🔠"),
    ("\u00f0\u009f\u0094\u00aa", "🔪"),
    ("\u00f0\u009f\u0094\u00a7", "🔧"),
    ("\u00f0\u009f\u0094\u00a8", "🔨"),
    ("\u00f0\u009f\u0094\u00b0", "🔰"),
    ("\u00f0\u009f\u0094\u00b4", "🔴"),
    ("\u00f0\u009f\u0094\u00b5", "🔵"),
    ("\u00f0\u009f\u0094\u00b6", "🔶"),
    ("\u00f0\u009f\u0094\u00b7", "🔷"),
    ("\u00f0\u009f\u0094\u00b8", "🔸"),
    ("\u00f0\u009f\u0094\u00b9", "🔹"),
    ("\u00f0\u009f\u0094\u00ba", "🔺"),
    ("\u00f0\u009f\u0094\u00bb", "🔻"),
    ("\u00f0\u009f\u0094\u00bc", "🔼"),
    ("\u00f0\u009f\u0094\u00bd", "🔽"),
    ("\u00f0\u009f\u00a7\u00b9", "🧹"),
    ("\u00f0\u009f\u00a7\u00ba", "🧺"),
    ("\u00f0\u009f\u00a7\u00bb", "🧻"),
    ("\u00f0\u009f\u00a7\u00bc", "🧼"),
    ("\u00f0\u009f\u0092\u00a3", "💣"),
    ("\u00f0\u009f\u0091\u00be", "👾"),
    ("\u00f0\u009f\u0091\u00bf", "👿"),
    ("\u00f0\u009f\u0091\u00b8", "👸"),
    ("\u00f0\u009f\u0091\u00b9", "👹"),
    ("\u00f0\u009f\u0091\u00ba", "👺"),
    ("\u00f0\u009f\u0091\u00bb", "👻"),
    ("\u00f0\u009f\u0091\u00bc", "👼"),
    ("\u00f0\u009f\u0091\u00bd", "👽"),
    ("\u00f0\u009f\u0092\u00af", "💯"),
    ("\u00f0\u009f\u0093\u00a2", "📢"),
    ("\u00f0\u009f\u008d\u00b4", "🍴"),
    ("\u00f0\u009f\u008d\u00b5", "🍵"),
    ("\u00f0\u009f\u008d\u00b6", "🍶"),
    ("\u00f0\u009f\u008d\u00bb", "🍻"),
    ("\u00f0\u009f\u008d\u00ba", "🍺"),
    ("\u00f0\u009f\u008d\u00a3", "🍣"),
    ("\u00f0\u009f\u00a5\u00b3", "🥳"),
    ("\u00f0\u009f\u00a5\u00b0", "🥰"),
    ("\u00f0\u009f\u00a5\u00b4", "🥴"),
    ("\u00f0\u009f\u00a5\u00b5", "🥵"),
    ("\u00f0\u009f\u00a5\u00b6", "🥶"),
    ("\u00f0\u009f\u00a5\u00b7", "🥷"),
    ("\u00f0\u009f\u00a4\u00a3", "🤣"),
    ("\u00f0\u009f\u00a4\u00a9", "🤩"),
    ("\u00f0\u009f\u00a4\u00af", "🤯"),
    ("\u00f0\u009f\u00a4\u00b0", "🤰"),
    ("\u00f0\u009f\u00a4\u00b3", "🤳"),
    ("\u00f0\u009f\u00a4\u00b7", "🤷"),
    ("\u00f0\u009f\u00a5\u0081", "🥁"),
    ("\u00f0\u009f\u00a6\u00b4", "🦴"),
    ("\u00f0\u009f\u00a7\u008d", "🧍"),
    ("\u00f0\u009f\u00a7\u008e", "🧎"),
    ("\u00f0\u009f\u00a7\u0094", "🧔"),
    # calendar icon
    ("\u00f0\u009f\u0097\u0082", "🗂"),
    ("\u00f0\u009f\u0097\u0093", "🗓"),
    # stopwatch
    ("\u00f0\u009f\u0093\u00b7", "📷"),
    ("\u00f0\u009f\u0095\u0090", "🕐"),
    ("\u00f0\u009f\u0095\u009b", "🕛"),
    ("\u00f0\u009f\u0095\u009a", "🕚"),
    # timer
    ("\u00f0\u009f\u0098\u00a2", "😢"),
    ("\u00f0\u009f\u0098\u00a3", "😣"),
    ("\u00f0\u009f\u0098\u00a4", "😤"),
    ("\u00f0\u009f\u0098\u00a5", "😥"),
]

count = 0
for old, new in REPLACEMENTS:
    if old in content:
        n = content.count(old)
        content = content.replace(old, new)
        count += n
        print(f"  {repr(old)} -> {repr(new)} ({n}x)")

# Final pass: use bytes-based approach for any remaining patterns
# Try to decode any remaining Ã sequences by treating them as latin-1 bytes
# This handles edge cases not covered above
import re

def fix_remaining_latin1(text):
    # Find sequences of chars that look like latin-1 encoded UTF-8
    # Pattern: sequences starting with Ã (0xC3) followed by various chars
    total_fixed = [0]
    
    def replace_match(m):
        seq = m.group(0)
        try:
            # Try to encode as latin-1 bytes, then decode as UTF-8
            fixed = seq.encode('latin-1').decode('utf-8')
            if fixed != seq:
                total_fixed[0] += 1
                return fixed
        except (UnicodeEncodeError, UnicodeDecodeError):
            pass
        return seq
    
    # Match Ã + next char pairs
    result = re.sub(r'Ã.', replace_match, text)
    # Match â€ + next char triplets (typographic quotes/dashes)
    result = re.sub(r'â€.', replace_match, result)
    # Match ð (0xF0) + 3 following chars (4-byte emoji prefix)
    result = re.sub(r'ð\x9f..', replace_match, result)
    
    print(f"  Regex Latin-1 auto-fix: {total_fixed[0]} sequences")
    return result

content = fix_remaining_latin1(content)

print(f"\nTotal explicit replacements: {count}")

with open(FILE, "w", encoding="utf-8") as f:
    f.write(content)

print(f"Saved {FILE}")

# Count remaining patterns
remaining = len(re.findall(r'Ã[^\s]|â€[^\s]|ðŸ', content))
print(f"Estimated remaining mojibake patterns: {remaining}")
