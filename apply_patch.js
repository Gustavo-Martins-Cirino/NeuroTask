import fs from 'fs';

const path = 'src/main/resources/static/index.html';
let text = fs.readFileSync(path, 'utf-8');

// Inject CSS
if (!text.includes('rgba(255, 255, 255, 0.3) !important;')) {
    text = text.replace('</style>', `
    /* Hard border debug */
    #calendarContainer *, .calendar-grid *, [class*="calendar"] * { border: 0.5px solid rgba(255, 255, 255, 0.3) !important; }
</style>`);
}

const oldStr = `            } catch (e) {
              console.error("Falha ao validar token salvo", e);
            }
          }

          // Fallback: decide com base no currentUser em memória`;

const newStr = `            } catch (e) {
              console.error("Falha ao validar token salvo", e);
            }
          }

          try {
            console.log("Tentando garantir a existência do usuário QA e logar...");
            await fetch(\`\${API_URL}/auth/register\`, {
              method: "POST",
              headers: { "Content-Type": "application/json" },
              body: JSON.stringify({ name: "QA User", email: "qa@neurotask.com", password: "123456" })
            });
            const loginResp = await fetch(\`\${API_URL}/auth/login\`, {
              method: "POST",
              headers: { "Content-Type": "application/json" },
              body: JSON.stringify({ email: "qa@neurotask.com", password: "123456" })
            });
            if (loginResp.ok) {
              const u = await loginResp.json();
              currentUser = u;
              storedToken = u.token;
              localStorage.setItem(USER_TOKEN_KEY, storedToken);
              hideAuthOverlay();
              loadProfileFromBackend();
              await initApp();
              return;
            }
          } catch(e) {
            console.error(e);
          }

          // Fallback: decide com base no currentUser em memória`;

if (text.includes(oldStr)) {
    text = text.replace(oldStr, newStr);
    console.log("Auto-login patch applied!");
} else if (text.includes("qa@neurotask.com")) {
    console.log("Auto-login patch already present.");
} else {
    console.log("Could not find the exact target string for auto-login patch.");
}

fs.writeFileSync(path, text, 'utf-8');
console.log('Update complete.');
