import re
import os

path = 'src/main/resources/static/js/app.js'
with open(path, 'r', encoding='utf-8') as f:
    text = f.read()

# Add import for dragController if not present
if 'import { dragController }' not in text:
    text = text.replace('import { calendarView } from', 'import { dragController } from "./modules/dragController.js";\nimport { calendarView } from')

new_func = """async function refreshTasksAndCalendar() {
  const updatedTasks = await taskService.getAllTasks();
  if (typeof window !== "undefined") {
    window.tasks = updatedTasks;
  }
  
  if (typeof window.loadTasks === "function") {
    await window.loadTasks();
  }
  
  // ============================================
  // INTEGRACAO VIEW/MODULES
  // ============================================
  try {
    const mainTimeline = document.querySelector("#mainCalendarTimeline");
    const dashTimeline = document.querySelector("#calendarTimeline");
    
    // Tornar tarefas do backlog/todo arrastaveis usando dragController
    document.querySelectorAll('.task-item:not([draggable]), .task-card:not([draggable])').forEach(taskEl => {
      const taskId = taskEl.dataset.taskId || taskEl.dataset.taskid || taskEl.id.replace('task-', '');
      if (taskId && dragController && dragController.makeDraggable) {
        dragController.makeDraggable(taskEl, taskId);
      }
    });
    
    // Inicializar drop zones do calendario
    // Nota: O HTML já está gerando as zonas, mas calendarView pode sobrepor.
    if (mainTimeline && calendarView && calendarView.render) {
        calendarView.render(updatedTasks, "#mainCalendarTimeline");
    }
    if (dashTimeline && calendarView && calendarView.render) {
        calendarView.render(updatedTasks, "#calendarTimeline");
    }

  } catch(e) {
    console.error("Erro ao integrar moduli ES6:", e);
  }
}"""

# O texto antigo está usando await window.loadTasks() etc
text = re.sub(r'async function refreshTasksAndCalendar\(\)\s*\{[\s\S]*?\}\s*(?=// Expor apenas)', new_func + '\n\n', text)

with open(path, 'w', encoding='utf-8') as f:
    f.write(text)

print("app.js updated successfully!")
