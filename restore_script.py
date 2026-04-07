import os

html_path = 'src/main/resources/static/index.html'
js_path = 'src/main/resources/static/js/inline_fix.js'

with open(html_path, 'r', encoding='utf-8') as f:
    lines = f.readlines()

new_lines = []
for line in lines:
    if '<script src="/js/inline_fix.js"></script>' in line:
        pass # ignore it for now
    new_lines.append(line)

# Let's see where </body> is.
body_idx = -1
for i, line in enumerate(new_lines):
    if '</body>' in line:
        body_idx = i
        break

if body_idx != -1:
    # insert the correct JS here
    script_content = """    <script>
    window.toggleZenChat = function() {
      const panel = document.getElementById('zenChatPanel');
      const chevron = document.getElementById('zenChatChevron');
      if (panel && panel.classList.contains('expanded')) {
        panel.classList.remove('expanded');
        if (chevron) chevron.style.transform = 'rotate(0deg)';
      } else {
        if (panel) panel.classList.add('expanded');
        if (chevron) chevron.style.transform = 'rotate(180deg)';
      }
    };
    
    window.openDailyNotesModal = function() {
      let modal = document.getElementById('dailyNotesOverlay');
      if (!modal) {
        const html = `
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
        </div>`;
        document.body.insertAdjacentHTML('beforeend', html);
        modal = document.getElementById('dailyNotesOverlay');
      }
      if (modal) modal.style.display = 'flex';
    };
    
    window.closeDailyNotesModal = function() {
      const overlay = document.getElementById('dailyNotesOverlay');
      if (overlay) overlay.style.display = 'none';
    };

    setTimeout(() => {
      if (typeof window.calendarEvents !== 'undefined') {
         const today = new Date();
         const thurs = new Date(today);
         thurs.setDate(today.getDate() - today.getDay() + 4);
         const dateStr = window.formatDateLocal ? window.formatDateLocal(thurs) : thurs.toISOString().split('T')[0];
         
         window.calendarEvents.push({
           id: 'ev-prototype-ui-' + Date.now(),
           title: 'Sessão Protótipo UI',
           type: 'focus',
           date: dateStr,
           startTime: '09:00',
           endTime: '10:00',
           color: '#a78bfa'
         });
         if (typeof window.renderCalendarGrid === 'function') window.renderCalendarGrid();
      }
    }, 1500);
    </script>
"""
    new_lines.insert(body_idx, script_content)
    
with open('debug_html.txt', 'w', encoding='utf-8') as f:
    f.writelines(new_lines)
