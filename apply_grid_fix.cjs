const fs = require('fs');
const path = 'src/main/resources/static/index.html';
let html = fs.readFileSync(path, 'utf8');

const target = `const column = document.createElement("div");\n            column.className = "week-day-column";`;
const replacement = `const column = document.createElement("div");
            column.className = "week-day-column";

            // INJECT HORIZONTAL LINES
            for (let bg = startHour; bg <= endHour; bg++) {
              const bgSlot = document.createElement("div");
              bgSlot.className = "week-hour-slot-bg";
              // absolute positioning creates visually perfect alignment
              bgSlot.style.position = "absolute";
              bgSlot.style.left = "0";
              bgSlot.style.right = "0";
              bgSlot.style.top = bg * 44 + "px"; // 44px is hourHeight in week view
              column.appendChild(bgSlot);
            }`;

    html = html.replace(/const column = document\.createElement\("div"\);\s*column\.className = "week-day-column";/g, replacement);
    
    // Replace the huge block of custom CSS at the bottom
    const cssMatch = html.match(/    \/\* Hard border debug refined \*\/[\s\S]+?<\/style>/);
    if (cssMatch) {
        html = html.replace(cssMatch[0], `    /* Hard border debug refined */
    .calendar-hour-row, .hour-row, .day-hour-row, .week-time-label { 
        border-bottom: 1px solid rgba(255, 255, 255, 0.2) !important; 
    }

    .week-hour-slot-bg {
        width: 100% !important;
        height: var(--week-hour-height);
        border-bottom: 1px solid rgba(255, 255, 255, 0.2) !important;
        box-sizing: border-box;
        pointer-events: none;
        z-index: 1;
    }
    body:not(.dark-mode) .week-hour-slot-bg {
        border-bottom: 1px solid rgba(0, 0, 0, 0.15) !important;
    }

    .week-body-grid {
        background: transparent !important;
        z-index: 1;
    }

    .week-day-column, .calendar-day-col, .day-column { 
        border-right: 1px solid rgba(255, 255, 255, 0.2) !important; 
        background: transparent !important;
        position: relative;
        z-index: 2; /* Fica por cima do grid mas vazado no fundo transparente */
    }
</style>`);
        fs.writeFileSync(path, html, 'utf8');
        console.log("Success! File written.");
    } else {
        console.log("Could not find CSS block");
    }
}
