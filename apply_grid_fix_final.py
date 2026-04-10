import re

with open('src/main/resources/static/index.html', 'r', encoding='utf-8') as f:
    content = f.read()

# Pattern to find the section to replace: from /* ==================== FIX: COR DAS LINHAS DE GRADE DO CALENDARIO (MODO ESCURO) ==================== */ until </style>
pattern = re.compile(r'      /\* ==================== FIX: COR DAS LINHAS DE GRADE DO CALENDARIO \(MODO ESCURO\) ==================== \*/.*?</style>', re.DOTALL)

replacement = '''      /* RESET FINAL DE BORDAS NEUROTASK */
      body:not(.light-mode) .week-hour-slot-bg, 
      body:not(.light-mode) .calendar-cell, 
      body:not(.light-mode) .hour-row, 
      body:not(.light-mode) .time-slot,
      body:not(.light-mode) .day-hour-row,
      body:not(.light-mode) .calendar-hour-row,
      body:not(.light-mode) .week-time-label,
      body:not(.light-mode) .calendar-time-label {
         border-bottom: 1px solid rgba(255, 255, 255, 0.12) !important;
         border-top: none !important;
         border-left: none !important;
      }
      
      body.light-mode .week-hour-slot-bg, 
      body.light-mode .calendar-cell, 
      body.light-mode .hour-row, 
      body.light-mode .time-slot,
      body.light-mode .day-hour-row,
      body.light-mode .calendar-hour-row,
      body.light-mode .week-time-label,
      body.light-mode .calendar-time-label {
         border-bottom: 1px solid rgba(0, 0, 0, 0.05) !important;
         border-top: none !important;
         border-left: none !important;
      }
      
      body:not(.light-mode) .day-column, 
      body:not(.light-mode) .calendar-day-col,
      body:not(.light-mode) .week-day-column,
      body:not(.light-mode) .calendar-day {
         border-right: 1px solid rgba(255, 255, 255, 0.12) !important;
         background: transparent !important;
      }
      
      body.light-mode .day-column, 
      body.light-mode .calendar-day-col,
      body.light-mode .week-day-column,
      body.light-mode .calendar-day {
         border-right: 1px solid rgba(0, 0, 0, 0.05) !important;
         background: transparent !important;
      }

      /* Linha da hora atual vibrante (#ff4500) */
      body:not(.light-mode) .current-time-line {
        background: #ff4500 !important;
        box-shadow: 0 0 10px rgba(255, 69, 0, 0.7) !important;
      }
      body:not(.light-mode) .current-time-line::before {
        background: #ff4500 !important;
        border-color: transparent !important;
      }
      
      /* week layout defaults that shouldn't mess up borders */
      .week-hour-slot-bg {
          width: 100% !important;
          height: var(--week-hour-height);
          box-sizing: border-box;
          pointer-events: none;
          z-index: 1;
          box-shadow: none !important;
          background: transparent !important;
      }
      .week-body-grid {
          background: transparent !important;
      }
</style>'''

new_content = pattern.sub(replacement, content)

with open('src/main/resources/static/index.html', 'w', encoding='utf-8') as f:
    f.write(new_content)
    
print("Updated successfully")
