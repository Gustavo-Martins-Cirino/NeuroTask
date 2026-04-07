import re

with open('src/main/resources/static/index.html', 'r', encoding='utf-8') as f:
    html = f.read()

# Replace the sidebar menu
replacement = """      <div class="nav-menu" style="display: flex; flex-direction: column; gap: 8px; flex: 1; padding: 16px;">
        <button class="create-btn" id="dashboardNewTaskBtn" style="margin-bottom: 24px; padding: 12px; border-radius: 8px; width: 100%; display: flex; justify-content: center; align-items: center; background: #ededed; color: #111; border: none; font-weight: 600; cursor: pointer; transition: background 0.2s;">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" style="margin-right: 8px;">
            <line x1="12" y1="5" x2="12" y2="19"></line>
            <line x1="5" y1="12" x2="19" y2="12"></line>
          </svg>
          <span>Nova Tarefa</span>
        </button>

        <div class="nav-item active" data-view="calendar" onclick="showView('calendar')">
          <span class="nav-icon">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="4" width="18" height="18" rx="2" ry="2"></rect><line x1="16" y1="2" x2="16" y2="6"></line><line x1="8" y1="2" x2="8" y2="6"></line><line x1="3" y1="10" x2="21" y2="10"></line></svg>
          </span>
          <span>Calendário</span>
        </div>

        <div class="nav-item" data-view="todo" onclick="showView('todo')">
          <span class="nav-icon">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2"></path></svg>
          </span>
          <span>To-Do List</span>
        </div>

        <div class="nav-item" data-view="dashboard" onclick="showView('dashboard')">
          <span class="nav-icon">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="22 12 18 12 15 21 9 3 6 12 2 12"></polyline></svg>
          </span>
          <span>Dashboard</span>
        </div>

        <div class="nav-item" data-view="gamification" onclick="showView('gamification')">
          <span class="nav-icon">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2"></polygon></svg>
          </span>
          <span>Gamificação</span>
        </div>

        <div class="nav-item" data-view="weeklyReview" onclick="showView('weeklyReview')">
          <span class="nav-icon">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"></path><polyline points="14 2 14 8 20 8"></polyline><line x1="16" y1="13" x2="8" y2="13"></line><line x1="16" y1="17" x2="8" y2="17"></line></svg>
          </span>
          <span>Revisão Semanal</span>
        </div>

        <div style="flex: 1;"></div>

        <div class="nav-item" data-view="settings" onclick="showView('settings')" style="margin-top: auto;">
          <span class="nav-icon">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="3"></circle><path d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 0 1 0 2.83 2 2 0 0 1-2.83 0l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-2 2 2 2 0 0 1-2-2v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 0 1-2.83 0 2 2 0 0 1 0-2.83l.06-.06a1.65 1.65 0 0 0 .33-1.82 1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1-2-2 2 2 0 0 1 2-2h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 0 1 0-2.83 2 2 0 0 1 2.83 0l.06.06a1.65 1.65 0 0 0 1.82.33H9a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 2-2 2 2 0 0 1 2 2v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 0 1 2.83 0 2 2 0 0 1 0 2.83l-.06.06a1.65 1.65 0 0 0-.33 1.82V9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 2 2 2 2 0 0 1-2 2h-.09a1.65 1.65 0 0 0-1.51 1z"></path></svg>
          </span>
          <span>Configurações</span>
        </div>
      </div>"""

# Find where nav-menu starts and sidebar ends
pattern = r'<div class="nav-menu">.*?</aside>'
# Just replace until the closing div of nav-menu in the original logic.. wait, the original logic had extra nav items outside nav-menu.
html = re.sub(r'<div class="nav-menu">.*?</div>\n    </div>', replacement + '\n    </div>', html, flags=re.DOTALL)

# Header update
header_replacement = r"""      <div class="calendar-header">
          <div class="calendar-header-top" style="display: flex; justify-content: space-between; align-items: flex-end; margin-bottom: 2rem;">
            <div>
              <h1 style="font-weight: 600; color: #ededed; font-size: 2rem; margin-bottom: 4px;">Bom dia, Gustavo</h1>
              <p style="color: #a1a1a1; font-size: 1rem;">Você tem 3 tarefas importantes hoje.</p>
            </div>
            <div class="user-profile" style="display: flex; align-items: center; gap: 1rem;">
              <div class="xp-bar" style="display: flex; align-items: center; gap: 8px; background: #1c1c1c; padding: 6px 12px; border-radius: 20px;">
                <span style="color: #ededed; font-weight: 500; font-size: 0.9rem;">Level 12</span>
                <div style="width: 100px; height: 6px; background: #2a2a2a; border-radius: 4px; overflow: hidden;">
                  <div style="width: 70%; height: 100%; background: #ededed; border-radius: 4px;"></div>
                </div>
              </div>
              <div class="avatar" style="width: 40px; height: 40px; border-radius: 50%; background: #4a4a4a; display: flex; align-items: center; justify-content: center; color: white; font-weight: 600;">G</div>
            </div>
          </div>"""

html = re.sub(r'      <div class="calendar-header">\s*<div class="calendar-header-top">\s*<h1.*?<\/h1>', header_replacement, html, flags=re.DOTALL)


# Replace right panel logic
right_panel_replacement = r"""      <aside class="right-panel expanded" id="rightPanel" style="width: 380px; background: #161616; border-left: 1px solid #2a2a2a; display: flex; flex-direction: column; overflow: hidden;">
        
        <!-- NeuroIA Panel -->
        <div class="panel-section" style="flex: 1; display: flex; flex-direction: column; border-bottom: 1px solid #2a2a2a;">
            <div class="panel-header" style="padding: 16px 20px; border-bottom: 1px solid #2a2a2a; display: flex; align-items: center; gap: 8px;">
                <span class="material-icons-outlined" style="color: #ededed;">auto_awesome</span>
                <h3 style="margin: 0; font-size: 14px; font-weight: 600; color: #ededed;">NeuroIA Copilot</h3>
            </div>
            
            <div class="chat-messages" id="feedbackContent" style="flex: 1; padding: 16px 20px; overflow-y: auto; display: flex; flex-direction: column; gap: 12px; font-size: 13px;">
                <!-- Chat messages go here -->
                <div class="message ai" style="display: flex; gap: 10px;">
                    <div class="avatar" style="width: 24px; height: 24px; border-radius: 4px; background: #2a2a2a; display: flex; align-items: center; justify-content: center;"><span class="material-icons-outlined" style="font-size: 14px;">auto_awesome</span></div>
                    <div class="bubble" style="background: #1c1c1c; padding: 12px; border-radius: 0 8px 8px 8px; color: #ededed; line-height: 1.5;">Olá Gustavo! Você tem 2 horas consecutivas livres esta tarde. Quer que eu agende seu projeto prioritário?</div>
                </div>
            </div>
            
            <div class="chat-input" style="padding: 16px; border-top: 1px solid #2a2a2a;">
                <div class="input-wrapper" style="display: flex; gap: 8px; background: #1c1c1c; border-radius: 6px; padding: 6px; border: 1px solid #2a2a2a;">
                    <input type="text" placeholder="Pergunte à NeuroIA..." style="flex: 1; background: transparent; border: none; outline: none; color: #ededed; font-size: 13px; padding-left: 8px; font-family: 'Inter', sans-serif;">
                    <button style="background: #ededed; color: #111; border: none; width: 28px; height: 28px; border-radius: 4px; display: flex; align-items: center; justify-content: center; cursor: pointer;"><span class="material-icons-outlined" style="font-size: 16px;">send</span></button>
                </div>
            </div>
        </div>

        <!-- Notes Panel -->
        <div class="panel-section" style="flex: 1; display: flex; flex-direction: column;">
            <div class="panel-header" style="padding: 16px 20px; border-bottom: 1px solid #2a2a2a; display: flex; align-items: center; justify-content: space-between;">
                <div style="display: flex; align-items: center; gap: 8px;">
                    <span class="material-icons-outlined" style="color: #ededed;">notes</span>
                    <h3 style="margin: 0; font-size: 14px; font-weight: 600; color: #ededed;">Notas e Anotações</h3>
                </div>
            </div>
            
            <div class="notes-content" style="flex: 1; padding: 16px 20px;">
                <textarea placeholder="Faça suas anotações livres aqui..." style="width: 100%; height: 100%; background: transparent; border: none; resize: none; color: #a1a1a1; font-family: 'Inter', sans-serif; font-size: 13px; line-height: 1.6; outline: none;"></textarea>
            </div>
        </div>
      </aside>"""

html = re.sub(r'      <aside class="right-panel".*?</aside>', right_panel_replacement, html, flags=re.DOTALL)

with open('src/main/resources/static/index.html', 'w', encoding='utf-8') as f:
    f.write(html)
