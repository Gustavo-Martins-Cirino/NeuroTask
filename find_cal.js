const fs = require('fs');
const text = fs.readFileSync('src/main/resources/static/index.html', 'utf8');

const match = text.match(/function _renderCalendarViews[\s\S]{0,1000}/);
if (match) {
    console.log(match[0]);
} else {
    console.log('Not found _renderCalendarViews');
}

const match2 = text.match(/function renderWeekView[\s\S]{0,1000}/);
if (match2) {
    console.log("\n-----\n" + match2[0]);
}
