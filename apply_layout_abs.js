const fs = require('fs');
const file = 'c:\\Users\\User\\OneDrive\\Desktop\\Dev\\Programação\\Projetos - Pessoais\\myday-productivity\\src\\main\\resources\\static\\index.html';
let content = fs.readFileSync(file, 'utf8');

console.log("Reading index.html...");

// 1. CSS Colors Update
content = content.replace(/--bg-main:\s*#0f1117;/g, '--bg-main: #10141C;');
content = content.replace(/--bg-sidebar:\s*rgba\(15, 17, 23, 0\.92\);/g, '--bg-sidebar: #10141C;');
content = content.replace(/--bg-card:\s*rgba\(24, 27, 38, 0\.82\);/g, '--bg-card: #10141C;');

// 2. Insert CSS variables and layout modifications
const cssInjection = `
      .app-container {
        display: flex;
        height: 100vh;
        overflow: hidden;
        background: var(--bg-main);
      }
      .right-panel {
        width: 320px;
        flex-shrink: 0;
        background: var(--bg-main);
        border-left: 1px solid var(--border-color);
        display: flex;
        flex-direction: column;
        overflow-y: auto;
      }
      .center-panel {
        flex: 1;
        display: flex;
        flex-direction: column;
        overflow-y: auto;
      }
      
      /* Zen AI Chat styling */
      .zen-chat-panel {
        border-top: 1px solid var(--border-color);
        display: flex;
        flex-direction: column;
        margin-top: auto;
      }
      .zen-chat-header {
        padding: 16px;
        font-weight: 600;
        display: flex;
        justify-content: space-between;
        align-items: center;
        border-bottom: 1px solid color-mix(in srgb, var(--border-color) 40%, transparent);
        transition: background 0.2s;
      }
      .zen-chat-header:hover {
        background: var(--hover-bg);
      }
      .zen-chat-body {
        padding: 16px;
        display: none;
        flex-direction: column;
        gap: 12px;
      }
      .zen-chat-panel.expanded .zen-chat-body {
        display: flex;
      }
      .zen-chat-quick-btns {
        display: flex;
        flex-direction: column;
        gap: 8px;
      }
      .zen-quick-btn {
        background: var(--hover-bg);
        border: 1px solid var(--border-color);
        color: var(--text-primary);
        padding: 10px 14px;
        border-radius: 10px;
        text-align: left;
        cursor: pointer;
        font-size: 13px;
        transition: all 0.2s;
        display: flex;
        align-items: center;
        gap: 8px;
        font-weight: 500;
      }
      .zen-quick-btn:hover { background: var(--bg-card); border-color: var(--primary); transform: translateY(-1px); }
      .zen-chat-input-wrapper {
        display: flex;
        gap: 8px;
        margin-top: 8px;
      }
      .zen-chat-input-wrapper input {
        flex: 1;
        background: var(--bg-main);
        border: 1px solid var(--border-color);
        color: white;
        padding: 10px 14px;
        border-radius: 20px;
        outline: none;
        font-family: inherit;
        font-size: 13px;
      }
      .zen-chat-input-wrapper input:focus {
        border-color: var(--primary);
      }
      
      /* Daily Notes Modal styling */
      .daily-notes-modal-overlay {
        position: fixed; top:0; left:0; width:100vw; height:100vh;
        background: rgba(0,0,0,0.6); backdrop-filter: blur(8px);
        display: none; justify-content: center; align-items: center; z-index: 10000;
        animation: fadeIn 0.3s ease;
      }
      @keyframes fadeIn { from { opacity: 0; } to { opacity: 1; } }
      .daily-notes-modal {
        background: #10141C; width: 640px; max-width: 90vw;
        border-radius: 20px; border: 1px solid color-mix(in srgb, var(--border-color) 80%, transparent);
        padding: 30px; box-shadow: 0 24px 48px rgba(0,0,0,0.5);
      }
      .daily-notes-header {
        display: flex; justify-content: space-between; align-items: center;
        margin-bottom: 24px; font-size: 20px; font-weight: 600; font-family: 'Google Sans';
        color: #fff;
      }
      .daily-notes-content textarea {
        width: 100%; height: 160px; background: rgba(255,255,255,0.03);
        border: 1px solid var(--border-color); color: #fff; border-radius: 12px;
        padding: 18px; margin-bottom: 24px; resize: none; font-family: 'Inter', sans-serif;
        font-size: 14px; outline: none; transition: border-color 0.2s;
      }
      .daily-notes-content textarea:focus { border-color: var(--primary); }
      .reminders-list {
        background: rgba(255,255,255,0.02); border: 1px solid var(--border-color);
        border-radius: 12px; padding: 20px;
      }
      .reminder-item {
        padding: 10px 0; border-bottom: 1px solid rgba(255,255,255,0.05);
        display: flex; align-items: center; gap: 10px; font-size: 14px;
        color: var(--text-primary);
      }
      .reminder-item:last-child { border-bottom: none; }
`;
if(!content.includes('app-container')) {
  content = content.replace("</style>", cssInjection + "\n    </style>");

  // Sidebar CSS update (width 280px left -280px to relative flex)
  content = content.replace(
        /\.sidebar {\s*position: fixed;\s*left: -280px;\s*top: 64px;\s*width: 280px;\s*height: calc\(100vh - 64px\);\s*background: var\(--bg-sidebar\);\s*backdrop-filter: blur\(\d+px\);\s*-webkit-backdrop-filter: blur\(\d+px\);\s*border-right: 1px solid var\(--border-color\);\s*z-index: 100;\s*display: flex;\s*flex-direction: column;\s*transition: transform 0\.2s,\s*left 0\.3s cubic-bezier\(0\.4, 0, 0\.2, 1\);\s*}/,
        `.sidebar {
        width: 280px;
        flex-shrink: 0;
        background: var(--bg-sidebar);
        border-right: 1px solid var(--border-color);
        display: flex;
        flex-direction: column;
        height: 100vh;
        z-index: 10;
        transition: none;
        left: 0;
        position: relative;
        top: 0;
      }`
  );

  content = content.replace(/left: -280px;/g, 'left: 0;');
  content = content.replace(/height: calc\(100vh - 64px\);/g, 'height: 100vh;');

  // Main content CSS
  content = content.replace(
        /\.main-content {[\s\S]*?position: relative;\s*}/,
        `.main-content {
        flex: 1;
        height: 100vh;
        overflow-y: auto;
        position: relative;
      }`
  );

  // 3. HTML Layout Mod
  content = content.replace(/<div class="header">([\s\S]*?)<\/div>\s*<!-- SIDEBAR -->/, '<!-- Header removed for redesign -->\n    <div class="app-container">\n    <!-- SIDEBAR -->');

  const rightPanelHtml = `
      <!-- RIGHT PANEL -->
      <aside class="right-panel">
        <div class="right-panel-header" style="padding: 24px; display: flex; align-items: center; justify-content: space-between; border-bottom: 1px solid var(--border-color);">
           <span style="font-family:'Google Sans';font-weight:600;font-size:15px;color:var(--text-primary)">Painel Contextual</span>
           <button class="icon-btn" onclick="openDailyNotesModal()" title="Caderno Diário" style="background:var(--hover-bg);border-color:transparent;">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M4 19.5v-15A2.5 2.5 0 0 1 6.5 2H20v20H6.5a2.5 2.5 0 0 1 0-5H20"></path></svg>
           </button>
        </div>
        
        <div style="padding: 24px; flex: 1;">
          <div style="color:var(--text-secondary);font-size:13px;line-height:1.5;text-align:center;margin-top:20px;">
            Este espaço é dinâmico. Anotações para a tarefa atual, integrações ou contexto auxiliar aparecerão aqui.
          </div>
        </div>

        <!-- Zen AI Chat Panel -->
        <div class="zen-chat-panel" id="zenChatPanel">
          <div class="zen-chat-header" style="cursor: pointer;" onclick="toggleZenChat()">
            <div style="display:flex; align-items:center; gap:10px;">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="var(--primary)" stroke-width="2.5"><circle cx="12" cy="12" r="10"></circle><path d="M12 16v-4"></path><path d="M12 8h.01"></path></svg>
              <span>Zen-AI Assistente</span>
            </div>
            <svg id="zenChatChevron" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" style="transition:transform 0.2s;"><polyline points="6 9 12 15 18 9"></polyline></svg>
          </div>
          <div class="zen-chat-body">
            <div class="zen-chat-quick-btns">
               <button class="zen-quick-btn"><span>✨</span> Otimize meu dia</button>
               <button class="zen-quick-btn"><span>☕</span> Sugira um intervalo</button>
               <button class="zen-quick-btn"><span>📝</span> Resuma minhas notas</button>
            </div>
            <div class="zen-chat-input-wrapper">
               <input type="text" placeholder="Pergunte ao assistente..." />
               <button class="icon-btn" style="width:38px;height:38px;background:var(--primary);color:white;border:none;"><svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="22" y1="2" x2="11" y2="13"></line><polygon points="22 2 15 22 11 13 2 9 22 2"></polygon></svg></button>
            </div>
          </div>
        </div>
      </aside>
    </div> <!-- .app-container fecho -->

    <!-- Daily Notes Modal -->
    <div class="daily-notes-modal-overlay" id="dailyNotesOverlay" onclick="if(event.target===this) closeDailyNotesModal()">
      <div class="daily-notes-modal">
         <div class="daily-notes-header">
            <div style="display:flex;align-items:center;gap:12px;">
              <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="var(--accent)" stroke-width="2"><path d="M4 19.5v-15A2.5 2.5 0 0 1 6.5 2H20v20H6.5a2.5 2.5 0 0 1 0-5H20"></path></svg>
              <span>Notas e Reflexões Diárias</span>
            </div>
            <button class="icon-btn" style="border:none;background:transparent;" onclick="closeDailyNotesModal()" title="Fechar">×</button>
         </div>
         <div class="daily-notes-content">
            <textarea placeholder="Para interpretação por IA: Reflexões de Hoje... Como tem sido seu foco? Alguma ideia solta?"></textarea>
            <div class="reminders-list">
               <div style="font-size:11px; color:var(--text-secondary); margin-bottom:12px; font-weight:700; letter-spacing:0.04em;">LEMBRETES DO DIA</div>
               <div class="reminder-item"><div style="width:10px;height:10px;border-radius:50%;background:#34d399;box-shadow:0 0 8px rgba(52,211,153,0.4);"></div> Planejamento Estratégico</div>
               <div class="reminder-item"><div style="width:10px;height:10px;border-radius:50%;background:#a78bfa;box-shadow:0 0 8px rgba(167,139,250,0.4);"></div> Sessão de Protótipo de UI</div>
               <div class="reminder-item"><div style="width:10px;height:10px;border-radius:50%;background:#fbbf24;box-shadow:0 0 8px rgba(251,191,36,0.4);"></div> Chamada Cliente</div>
            </div>
         </div>
         <div style="display:flex; justify-content:flex-end; margin-top:24px;">
           <button class="modal-btn modal-btn-primary" onclick="closeDailyNotesModal(); if(typeof showToast === 'function') showToast('Notas salvas com sucesso!')" style="padding:12px 24px;font-size:14px;border-radius:8px;">Salvar Progresso</button>
         </div>
      </div>
    </div>
`;
  content = content.replace(/<!-- AUTH OVERLAY/, rightPanelHtml + '\n    <!-- AUTH OVERLAY');

  const createBtnMatch = content.match(/<button class="create-btn" id="dashboardNewTaskBtn">([\s\S]*?)<\/button>/);
  if(createBtnMatch) {
     content = content.replace(createBtnMatch[0], ''); 
     const navMenuEnd = content.indexOf('</div>\n    </div>\n\n    <!-- MAIN CONTENT -->');
     if(navMenuEnd !== -1) {
        content = content.substring(0, navMenuEnd) + '\n        ' + createBtnMatch[0] + content.substring(navMenuEnd);
     }
  }

  const jsLogic = `
window.toggleZenChat = function() {
  const panel = document.getElementById('zenChatPanel');
  const chevron = document.getElementById('zenChatChevron');
  if (panel.classList.contains('expanded')) {
    panel.classList.remove('expanded');
    chevron.style.transform = 'rotate(0deg)';
  } else {
    panel.classList.add('expanded');
    chevron.style.transform = 'rotate(180deg)';
  }
};
window.openDailyNotesModal = function() {
  document.getElementById('dailyNotesOverlay').style.display = 'flex';
};
window.closeDailyNotesModal = function() {
  document.getElementById('dailyNotesOverlay').style.display = 'none';
};

// Injeta o evento de prototipo na matriz de schedule existente
setTimeout(() => {
  if (typeof calendarEvents !== 'undefined') {
     const today = new Date();
     const thurs = new Date(today);
     thurs.setDate(today.getDate() - today.getDay() + 4);
     const dateStr = window.formatDateLocal ? window.formatDateLocal(thurs) : thurs.toISOString().split('T')[0];
     
     calendarEvents.push({
       id: 'ev-prototype-ui-' + Date.now(),
       title: 'Sessão Protótipo UI',
       type: 'focus',
       date: dateStr,
       startTime: '09:00',
       endTime: '10:00',
       color: '#a78bfa'
     });
     if (typeof renderCalendarGrid === 'function') renderCalendarGrid();
  }
}, 1500);
`;
  content = content.replace('</script>\n  </body>', jsLogic + '\n</script>\n  </body>');
}

fs.writeFileSync(file, content);
console.log('Update complete!!!');
