import fs from 'fs';
const t = fs.readFileSync('src/main/resources/static/index.html', 'utf8');
const match = t.match(/body:not\(\.light-mode\) \.day-hour-row[\s\S]{0,300}/);
console.log(match ? match[0] : 'not found');
