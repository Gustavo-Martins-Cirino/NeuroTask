import fs from 'fs';
const text = fs.readFileSync('src/main/resources/static/index.html', 'utf8');
const match = text.match(/\/\* Hard border debug refined \*\/[\s\S]{0,500}/);
console.log(match ? match[0] : 'no debug block');
