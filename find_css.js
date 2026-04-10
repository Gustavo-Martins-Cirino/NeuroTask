const fs = require('fs');
const t = fs.readFileSync('src/main/resources/static/index.html', 'utf8');

const c1 = t.match(/\.week-day-column[^}]+}/);
const c2 = t.match(/\.week-body-timeline[^}]+}/);
const c3 = t.match(/\.calendar-hour-row[^}]+}/);

console.log(c1 ? c1[0] : 'not found');
console.log(c2 ? c2[0] : 'not found');
console.log(c3 ? c3[0] : 'not found');
