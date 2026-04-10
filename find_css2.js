import fs from 'fs';
const css = fs.readFileSync('src/main/resources/static/index.html', 'utf8');

const match1 = css.match(/\.week-body-grid[\s\S]{0,500}/);
const match2 = css.match(/\.week-day-column[\s\S]{0,500}/);
const match3 = css.match(/\.time-slot[\s\S]{0,500}/);

console.log(match1 ? match1[0] : 'no week-body-grid');
console.log(match2 ? match2[0] : 'no week-day-column');
console.log(match3 ? match3[0] : 'no time-slot');
