import re
with open('src/main/resources/static/index.html', 'r', encoding='utf-8') as f:
    text = f.read()
text = re.sub(
    r'window\.openDailyNotesModal = function\(\) \{[\s\S]*?window\.closeDailyNotesModal = function\(\) \{.*?\}\;',
    'window.openDailyNotesModal = function() {};\nwindow.closeDailyNotesModal = function() {};',
    text,
    flags=re.DOTALL
)
with open('src/main/resources/static/index.html', 'w', encoding='utf-8') as f:
    f.write(text)
print("Modal JS replaced")
