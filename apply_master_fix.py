import re

def update_file(filepath):
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()

    # 1. Colors
    content = re.sub(
        r'--primary:\s*#818cf8;',
        r'--primary: #4b926c;',
        content
    )
    content = re.sub(
        r'--primary-light:\s*#a5b4fc;',
        r'--primary-light: #6bc091;',
        content
    )
    content = re.sub(
        r'--primary-dark:\s*#6366f1;',
        r'--primary-dark: #377051;',
        content
    )
    content = re.sub(
        r'--accent:\s*#6366f1;',
        r'--accent: #4b926c;',
        content
    )
    content = re.sub(
        r'--accent-soft:\s*rgba\(99, 102, 241, 0\.15\);',
        r'--accent-soft: rgba(75, 146, 108, 0.15);',
        content
    )
    content = re.sub(
        r'--bg-main:\s*#10141C;',
        r'--bg-main: #18191A;',
        content
    )
    content = re.sub(
        r'--bg-sidebar:\s*#10141C;',
        r'--bg-sidebar: #202123;',
        content
    )
    content = re.sub(
        r'--bg-card:\s*#10141C;',
        r'--bg-card: #27282B;',
        content
    )
    content = re.sub(
        r'--bg-glass:\s*#10141C;',
        r'--bg-glass: #27282B;',
        content
    )

    # Light mode colors
    content = re.sub(
        r'--bg-main:\s*#f8fafc;',
        r'--bg-main: #f5f6f8;',
        content
    )
    content = re.sub(
        r'--bg-sidebar:\s*rgba\(255, 255, 255, 0\.9\);',
        r'--bg-sidebar: #ffffff;',
        content
    )
    content = re.sub(
        r'--bg-card:\s*rgba\(255, 255, 255, 0\.82\);',
        r'--bg-card: #ffffff;',
        content
    )

    # 4. Remove Gamification from Nav
    gamif_nav = r'<div\s+class="nav-item"\s+data-view="gamification"\s+onclick="showView\(\'gamification\'\)">.*?<span>Gamificação</span>\s*</div>'
    content = re.sub(gamif_nav, '', content, flags=re.DOTALL)

    # 3. Modify Main Content to add Header
    header_html = """
    <!-- GLOBAL HEADER (Inside Main) -->
    <div class="global-header" style="display:flex; justify-content: space-between; align-items: center; padding: 16px 24px; border-bottom: 1px solid var(--border-color); background: var(--bg-card); position: sticky; top:0; z-index:100;">
      <div style="display:flex; align-items:center; gap:16px;">
        <button class="icon-btn" onclick="toggleSidebar()" title="Menu" style="width:36px;height:36px; background:var(--hover-bg); border:none;">
          <svg width="20" height="20" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><line x1="3" y1="12" x2="21" y2="12"></line><line x1="3" y1="6" x2="21" y2="6"></line><line x1="3" y1="18" x2="21" y2="18"></line></svg>
        </button>
        <span style="font-family:'Google Sans'; font-size:18px; font-weight:600; color:var(--text-primary);">Meu Espaço</span>
      </div>
      
      <div style="display:flex; align-items:center; gap:16px;">
        <!-- Level & XP -->
        <div style="display:flex; flex-direction:column; align-items:flex-end;">
          <span style="font-size:12px; color:var(--text-secondary); font-weight:600;">Nível 5</span>
          <div style="width:100px; height:6px; background:var(--border-color); border-radius:4px; margin-top:4px; overflow:hidden;">
            <div style="width:65%; height:100%; background:linear-gradient(90deg, var(--accent), var(--primary-light)); border-radius:4px;"></div>
          </div>
        </div>
        
        <!-- Profile Picture -->
        <div class="profile-photo-wrap" onclick="avatarMenuUpload()" title="Minha Conta" style="width:36px;height:36px;background:var(--hover-bg);border-radius:50%;display:flex;align-items:center;justify-content:center;cursor:pointer;overflow:hidden;">
            <img src="/img/brain.svg" id="globalHeaderAvatar" style="width:24px;height:24px;" alt="Avatar"/>
        </div>
      </div>
    </div>
"""
    content = content.replace('<!-- TASKS VIEW -->', header_html + '\n      <!-- TASKS VIEW -->')

    # Replace Right Panel
    new_right_panel = """
      <aside class="right-panel" style="display:flex; flex-direction:column; height: 100vh; border-left: 1px solid var(--border-color); background: var(--bg-sidebar); width:320px; flex-shrink:0;">
        
        <!-- QUEST MAP MINIMALISTA -->
        <div class="right-panel-section" style="border-bottom: 1px solid var(--border-color);">
          <div class="right-panel-header" onclick="this.nextElementSibling.style.display = this.nextElementSibling.style.display === 'none' ? 'block' : 'none';" style="padding: 16px 20px; display: flex; align-items: center; justify-content: space-between; cursor:pointer; background:var(--bg-card);">
             <div style="display:flex; align-items:center; gap:10px;">
               <svg width="18" height="18" stroke="var(--accent)" stroke-width="2" fill="none" viewBox="0 0 24 24"><path d="M12 2L2 7l10 5 10-5-10-5zM2 17l10 5 10-5M2 12l10 5 10-5"></path></svg>
               <span style="font-family:'Google Sans';font-weight:600;font-size:14px;color:var(--text-primary)">Quest Map (Atividades)</span>
             </div>
             <svg width="16" height="16" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><polyline points="6 9 12 15 18 9"></polyline></svg>
          </div>
          <div style="padding: 16px 20px; display:block;">
            <div style="font-size:12px; color:var(--text-secondary); margin-bottom:12px;">Complete tarefas diárias para ganhar XP.</div>
            <label style="display:flex; align-items:center; gap:10px; font-size:13px; color:var(--text-primary); margin-bottom:8px; cursor:pointer;">
              <input type="checkbox" style="accent-color:var(--accent);"> Primeira tarefa do dia (+10 XP)
            </label>
            <label style="display:flex; align-items:center; gap:10px; font-size:13px; color:var(--text-primary); margin-bottom:8px; cursor:pointer;">
              <input type="checkbox" style="accent-color:var(--accent);"> Bloco de foco 30m (+15 XP)
            </label>
            <label style="display:flex; align-items:center; gap:10px; font-size:13px; color:var(--text-primary); cursor:pointer;">
              <input type="checkbox" style="accent-color:var(--accent);"> Revisão noturna (+5 XP)
            </label>
          </div>
        </div>

        <!-- BLOCO DE NOTAS -->
        <div class="right-panel-section" style="flex:1; display:flex; flex-direction:column; border-bottom: 1px solid var(--border-color);">
          <div style="padding: 16px 20px; display: flex; align-items: center; gap:10px; background:var(--bg-card); border-bottom:1px solid var(--border-color);">
             <svg width="18" height="18" stroke="currentColor" stroke-width="2" fill="none" viewBox="0 0 24 24"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"></path><polyline points="14 2 14 8 20 8"></polyline><line x1="16" y1="13" x2="8" y2="13"></line><line x1="16" y1="17" x2="8" y2="17"></line><polyline points="10 9 9 9 8 9"></polyline></svg>
             <span style="font-family:'Google Sans';font-weight:600;font-size:14px;color:var(--text-primary)">Notas (Espaço Livre)</span>
          </div>
          <div style="flex:1; padding:12px; background:var(--bg-main);">
             <textarea placeholder="Faça anotações soltas aqui. A NeuroIA lerá isso..." style="width:100%; height:100%; resize:none; background:transparent; border:none; color:var(--text-primary); font-family:inherit; font-size:13px; outline:none; padding:8px;"></textarea>
          </div>
        </div>

        <!-- NEUROIA CHAT -->
        <div class="zen-chat-panel" id="zenChatPanel" style="flex-shrink:0;">
          <div class="zen-chat-header" style="cursor: pointer; padding:16px 20px; border-top:1px solid var(--border-color); background:var(--bg-card);" onclick="toggleZenChat()">
            <div style="display:flex; align-items:center; gap:10px;">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="var(--primary)" stroke-width="2"><circle cx="12" cy="12" r="10"></circle><path d="M12 16v-4"></path><path d="M12 8h.01"></path></svg>
              <span style="font-family:'Google Sans';font-weight:600;font-size:14px;">NeuroIA</span>
            </div>
          </div>
          <div class="zen-chat-body" style="padding:16px 20px;">
            <div class="zen-chat-input-wrapper" style="position:relative; display:flex; align-items:center;">
               <input type="text" placeholder="Pergunte à NeuroIA..." style="width:100%; padding:10px 45px 10px 14px; border-radius:8px; border:1px solid var(--border-color); background:var(--bg-main); color:var(--text-primary); font-size:13px; outline:none;" />
               <button class="icon-btn" style="position:absolute; right:4px; width:30px;height:30px;background:var(--primary);color:white;border:none;border-radius:6px;display:flex;align-items:center;justify-content:center;"><svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="22" y1="2" x2="11" y2="13"></line><polygon points="22 2 15 22 11 13 2 9 22 2"></polygon></svg></button>
            </div>
          </div>
        </div>
      </aside>
"""
    # Use regex to find and replace the whole <aside class="right-panel">
    panel_pattern = r'<aside class="right-panel">.*?</aside>'
    content = re.sub(panel_pattern, new_right_panel, content, flags=re.DOTALL)
    
    # 6. Change all texts from Zen-AI Assistente to NeuroIA
    content = content.replace('Zen-AI Assistente', 'NeuroIA')

    # Fix Javascript for sidebar toggle
    # Right now toggleSidebar toggles .open on sidebar. We need to make sure the app-container adapts if it doesn't already.
    # The layout is usually: .app-container { display: flex } .sidebar { width: 280px; }
    
    with open(filepath, 'w', encoding='utf-8') as f:
        f.write(content)

    print("Success!")

update_file('src/main/resources/static/index.html')
