$filePath = "src\main\resources\static\index.html"
if (-not (Test-Path $filePath)) { Write-Error "File not found: $filePath"; exit 1 }
$content = [System.IO.File]::ReadAllText($filePath, [System.Text.Encoding]::UTF8)
$before = $content.Length

# === Letras portuguesas minúsculas ===
$content = $content.Replace("Ã¡", "á")
$content = $content.Replace("Ã©", "é")
$content = $content.Replace("Ãª", "ê")
$content = $content.Replace("Ã£", "ã")
$content = $content.Replace("Ã³", "ó")
$content = $content.Replace("Ã§", "ç")
$content = $content.Replace("Ã­", "í")
$content = $content.Replace("Ãµ", "õ")
$content = $content.Replace("Ãº", "ú")
$content = $content.Replace("Ã¢", "â")
$content = $content.Replace("Ã´", "ô")
$content = $content.Replace("Ã¼", "ü")
$content = $content.Replace("Ã®", "î")
$content = $content.Replace("Ã ", "à")

# === Letras portuguesas maiúsculas ===
$content = $content.Replace("Ã‰", "É")
$content = $content.Replace("ÃŠ", "Ê")
$content = $content.Replace("Ã‡", "Ç")
$content = $content.Replace("Ãš", "Ú")
$content = $content.Replace("Ã•", "Õ")
$content = $content.Replace("Ã‚", "Â")
$content = $content.Replace("Ã€", "À")
$content = $content.Replace("Ã†", "Æ")
$content = $content.Replace("Ã†", "Æ")

# Ã sozinho (ex: "Gamificação" aparecia como "GamificaÃ§Ã£o" - já tratado acima)
# Mas "Ã" + "ƒ" = "Ã" (letra A tilde maiúscula)
$content = $content.Replace("Ãƒ", "Ã")

# === Caracteres tipográficos ===
$content = $content.Replace("â€œ", [char]0x201C)   # " aspa esquerda
$content = $content.Replace("â€™", [char]0x2019)   # ' apóstrofe direito
$content = $content.Replace("â€˜", [char]0x2018)   # ' apóstrofe esquerdo
$content = $content.Replace("â€¦", [char]0x2026)   # … reticências
$content = $content.Replace("â€¢", [char]0x2022)   # • bullet

# En dash e em dash (último char é aspa tipográfica)
$endash  = "â€" + [char]0x201C
$emdash  = "â€" + [char]0x201D
$content = $content.Replace($endash,  [char]0x2013)   # – en dash
$content = $content.Replace($emdash,  [char]0x2014)   # — em dash

# Aspa direita residual
$rdq = "â€" + [char]0x009D
$content = $content.Replace($rdq, [char]0x201D)        # "

# === Símbolos e setas ===
$content = $content.Replace("âœ"", "✓")
$content = $content.Replace("âœ…", "✅")
$content = $content.Replace("âœ–", "✖")
$content = $content.Replace("âœ¨", "✨")
$content = $content.Replace("âœï¸", "✏️")
$content = $content.Replace("âŒ", "❌")
$content = $content.Replace("âš¡", "⚡")
$content = $content.Replace("âš ï¸", "⚠️")
$content = $content.Replace("âš ", "⚠")
$content = $content.Replace("â­", "⭐")
$content = $content.Replace("â†"", "↓")
$content = $content.Replace("â†'", "→")
$content = $content.Replace("â†'", "↑")
$content = $content.Replace("â†"", "↔")
$content = $content.Replace("â±", "ⱽ")

# === Emojis 4-byte ===
$content = $content.Replace("ðŸ'¡", "💡")
$content = $content.Replace("ðŸŽ¯", "🎯")
$content = $content.Replace("ðŸ"Š", "📊")
$content = $content.Replace("ðŸ§ ", "🧠")
$content = $content.Replace("ðŸ"¥", "🔥")
$content = $content.Replace("ðŸŒ™", "🌙")
$content = $content.Replace("ðŸŽ®", "🎮")
$content = $content.Replace("ðŸ†", "🏆")
$content = $content.Replace("ðŸ'ª", "💪")
$content = $content.Replace("ðŸŽ‰", "🎉")
$content = $content.Replace("ðŸ'", "👍")
$content = $content.Replace("ðŸ'ˆ", "👈")
$content = $content.Replace("ðŸ"…", "📅")
$content = $content.Replace("ðŸ"Ž", "🔎")
$content = $content.Replace("ðŸ"", "🔍")
$content = $content.Replace("ðŸ"ˆ", "📈")
$content = $content.Replace("ðŸ"‰", "📉")
$content = $content.Replace("ðŸ"‹", "📋")
$content = $content.Replace("ðŸ˜"", "😓")
$content = $content.Replace("ðŸ˜Š", "😊")
$content = $content.Replace("ðŸ˜…", "😅")
$content = $content.Replace("ðŸ˜„", "😄")
$content = $content.Replace("ðŸ˜€", "😀")
$content = $content.Replace("ðŸ˜ˆ", "😈")
$content = $content.Replace("ðŸ˜±", "😱")
$content = $content.Replace("ðŸ˜¬", "😬")
$content = $content.Replace("ðŸ˜¤", "😤")
$content = $content.Replace("ðŸŒŸ", "🌟")
$content = $content.Replace("ðŸŒˆ", "🌈")
$content = $content.Replace("ðŸŒ´", "🌴")
$content = $content.Replace("ðŸŒŠ", "🌊")
$content = $content.Replace("ðŸŒŒ", "🌌")
$content = $content.Replace("ðŸŒ…", "🌅")
$content = $content.Replace("ðŸŒ†", "🌆")
$content = $content.Replace("ðŸŒ±", "🌱")
$content = $content.Replace("ðŸŒ³", "🌳")
$content = $content.Replace("ðŸ"„", "🔄")
$content = $content.Replace("ðŸ'»", "💻")
$content = $content.Replace("ðŸ"", "📝")
$content = $content.Replace("ðŸ'¬", "💬")
$content = $content.Replace("ðŸ"Œ", "📌")
$content = $content.Replace("ðŸ"–", "📖")
$content = $content.Replace("ðŸ"—", "📗")
$content = $content.Replace("ðŸ™", "🙏")
$content = $content.Replace("ðŸ§˜", "🧘")
$content = $content.Replace("ðŸ§ ", "🧠")
$content = $content.Replace("ðŸ¤–", "🤖")
$content = $content.Replace("ðŸ¤"", "🤔")
$content = $content.Replace("ðŸ'Ž", "💎")
$content = $content.Replace("ðŸ'€", "💀")
$content = $content.Replace("ðŸ'½", "👽")
$content = $content.Replace("ðŸ'–", "💖")
$content = $content.Replace("ðŸ'—", "💗")
$content = $content.Replace("ðŸ'˜", "💘")
$content = $content.Replace("ðŸš€", "🚀")
$content = $content.Replace("ðŸŽˆ", "🎈")
$content = $content.Replace("ðŸŽ€", "🎀")
$content = $content.Replace("ðŸŽ", "🎵".Substring(0,0) + "🎀")
$content = $content.Replace("ðŸŽµ", "🎵")
$content = $content.Replace("ðŸŽ–", "🎖")
$content = $content.Replace("ðŸŽ"", "🎓")
$content = $content.Replace("ðŸ…", "🏅")
$content = $content.Replace("ðŸ'", "💐")
$content = $content.Replace("ðŸ'†", "👆")
$content = $content.Replace("ðŸ'‡", "👇")
$content = $content.Replace("ðŸ'‰", "👉")
$content = $content.Replace("ðŸ'ˆ", "👈")

[System.IO.File]::WriteAllText($filePath, $content, [System.Text.Encoding]::UTF8)
$after = $content.Length
Write-Host "Concluído! Bytes antes: $before → depois: $after (diff: $($before - $after))"

# Verificar se ainda há padrões residuais
$remaining = Select-String -Path $filePath -Pattern "Ã[¡©ªµ³§­ºÃ¢´]|â€[œ™˜¦¢]|ðŸ" -AllMatches | Measure-Object
Write-Host "Padrões mojibake restantes: $($remaining.Count)"
