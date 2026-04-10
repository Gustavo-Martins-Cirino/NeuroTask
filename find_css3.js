import fs from 'fs';
const t = fs.readFileSync('src/main/resources/static/index.html', 'utf8');
const match = t.match(/\.day-hour-slot[\s\S]{0,300}/);
console.log(match ? match[0] : 'no .day-hour-slot');

const m2 = t.match(/\.calendar-event-space[\s\S]{0,300}/);
console.log(m2 ? m2[0] : 'no .calendar-event-space');
