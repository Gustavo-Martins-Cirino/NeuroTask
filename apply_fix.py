import os
import shutil

html_path = 'src/main/resources/static/index.html'
debug_path = 'debug_html.txt'

with open(debug_path, 'r', encoding='utf-8') as f:
    html_content = f.read()

# Let's fix the inline script directly
script_replacement = """    <script>
    // Correções de lint e funções globais
    function toggleZenChat() {
      const panel = document.getElementById('zenChatPanel');
      const chevron = document.getElementById('zenChatChevron');
      if (panel && panel.classList.contains('expanded')) {
        panel.classList.remove('expanded');
        if (chevron) chevron.style.transform = 'rotate(0deg)';
      } else {
        if (panel) panel.classList.add('expanded');
        if (chevron) chevron.style.transform = 'rotate(180deg)';
      }
    }
    window.toggleZenChat = toggleZenChat;
    
    function openDailyNotesModal() {
      let modal = document.getElementById('dailyNotesOverlay');
      if (!modal) {
        const htmlStr = `
        <!-- Daily Notes Modal -->
        <div class="daily-notes-modal-overlay" id="dailyNotesOverlay" onclick="if(event.target===this) window.closeDailyNotesModal()">
          <div class="daily-notes-modal">
             <div class="daily-notes-header">
                <div style="display:flex;align-items:center;gap:12px;">
                  <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="var(--accent)" stroke-width="2"><path d="M4 19.5v-15A2.5 2.5 0 0 1 6.5 2H20v20H6.5a2.5 2.5 0 0 1 0-5H20"></path></svg>
                  <span>Notas e Reflexões Diárias</span>
                </div>
                <button class="icon-btn" style="border:none;background:transparent;" onclick="window.closeDailyNotesModal()" title="Fechar">×</button>
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
               <button class="modal-btn modal-btn-primary" onclick="window.closeDailyNotesModal(); if(typeof window.showToast === 'function') window.showToast('Notas salvas com sucesso!')" style="padding:12px 24px;font-size:14px;border-radius:8px;">Salvar Progresso</button>
             </div>
          </div>
        </div>`;
        document.body.insertAdjacentHTML('beforeend', htmlStr);
        modal = document.getElementById('dailyNotesOverlay');
      }
      if (modal) {
        modal.style.display = 'flex';
      }
    }
    window.openDailyNotesModal = openDailyNotesModal;
    
    function closeDailyNotesModal() {
      const overlay = document.getElementById('dailyNotesOverlay');
      if (overlay) {
        overlay.style.display = 'none';
      }
    }
    window.closeDailyNotesModal = closeDailyNotesModal;

    setTimeout(function() {
      if (window.calendarEvents !== undefined && window.calendarEvents !== null) {
         const today = new Date();
         const thurs = new Date(today);
         thurs.setDate(today.getDate() - today.getDay() + 4);
         const dateStr = (typeof window.formatDateLocal === 'function') 
             ? window.formatDateLocal(thurs) 
             : thurs.toISOString().split('T')[0];
         
         window.calendarEvents.push({
           id: 'ev-prototype-ui-' + Date.now(),
           title: 'Sessão Protótipo UI',
           type: 'focus_session', // fixed 'type' and value to avoid clashes
           date: dateStr,
           startTime: '09:00',
           endTime: '10:00',
           color: '#a78bfa'
         });
         if (typeof window.renderCalendarGrid === 'function') {
             window.renderCalendarGrid();
         }
      }
    }, 1500);
    </script>"""

# Replace the old scripts with the corrected script
import re
new_html = re.sub(r'<script>\s*window\.toggleZenChat[\s\S]*?</script>', script_replacement, html_content)
# If substitution didn't happen because of indentation:
if "<script>" in new_html and "window.toggleZenChat = function" in html_content:
    print("Trying substring replace")
    start_tag = '    <script>\n    window.toggleZenChat = function() {'
    idx = html_content.find(start_tag)
    if idx != -1:
        end_tag = '    </script>\n'
        end_idx = html_content.find(end_tag, idx)
        if end_idx != -1:
            new_html = html_content[:idx] + script_replacement + html_content[end_idx + len(end_tag):]

with open(html_path, 'w', encoding='utf-8') as f:
    f.write(new_html)

print("Updated index.html successfully with clean JS.")
