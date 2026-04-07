const fs = require('fs');
const content = fs.readFileSync('index.html', 'utf8');

// Encontrar todos os blocos script
const scriptRegex = /<script[^>]*>([\s\S]*?)<\/script>/g;
let match;
let scriptIndex = 0;

while ((match = scriptRegex.exec(content)) !== null) {
  scriptIndex++;
  const scriptContent = match[1];
  
  // Pular scripts vazios ou de módulos externos
  if (!scriptContent.trim() || scriptContent.includes('src=') || scriptContent.includes('type="module"')) {
    continue;
  }
  
  try {
    // Tentar validar o JavaScript
    new Function(scriptContent);
    console.log(`Script ${scriptIndex}: OK`);
  } catch (error) {
    console.log(`Script ${scriptIndex}: ERRO - ${error.message}`);
    
    // Tentar encontrar a linha aproximada do erro
    const lines = content.substring(0, match.index).split('\n');
    console.log(`Posição aproximada: linha ${lines.length}`);
  }
}

console.log('Verificação concluída.');
