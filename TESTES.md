# 🧪 GUIA DE TESTES - NeuroTask

## ⚡ Teste Rápido (5 minutos)

### 1. Obter API Key do Gemini (2 min)
```
1. Acesse: https://aistudio.google.com/app/apikey
2. Faça login com conta Google
3. Clique em "Create API Key"
4. Copie a chave (formato: AIzaSy...)
```

### 2. Configurar Variável de Ambiente (30 seg)
```cmd
# Windows CMD
set GEMINI_API_KEY=AIzaSy...sua-chave-aqui...

# PowerShell
$env:GEMINI_API_KEY="AIzaSy...sua-chave-aqui..."
```

### 3. Iniciar Backend (1 min)
```cmd
cd myday-productivity
mvnw spring-boot:run
```

Aguarde ver:
```
Started MydayProductivityApplication in X.XX seconds
```

### 4. Testar Interface (2 min)
```
Abra: http://localhost:8080
```

**Criar tarefas:**
1. Digite "Estudar Java"
2. Descrição: "Revisar Spring Boot"
3. Prioridade: Alta
4. Clique "+ Adicionar"

Repita para criar 3-5 tarefas diferentes.

**Testar Time-Blocking:**
1. Arraste uma tarefa do backlog
2. Solte em um horário (ex: 14:00)
3. Veja a tarefa aparecer no calendário

**Testar IA:**
1. Clique "✨ Analisar com IA"
2. Aguarde processamento (~3-5 segundos)
3. Leia insights e recomendações
4. Digite no chat: "Como organizar meu dia?"
5. Veja resposta personalizada

---

## 🧪 Testes Detalhados

### A. Teste de Tarefas (CRUD Completo)

#### Criar Tarefa
```
Entrada:
- Título: "Implementar feature X"
- Descrição: "Adicionar endpoint REST"
- Prioridade: ALTA

Resultado esperado:
✅ Tarefa aparece no Backlog
✅ Status: PENDING
✅ Badge vermelha (alta prioridade)
```

#### Agendar Tarefa (Time-Block)
```
Ação:
- Arrastar tarefa para 15:00

Resultado esperado:
✅ Tarefa sai do Backlog
✅ Aparece no slot 15:00
✅ Status: SCHEDULED
✅ Cor de fundo azul/roxa
```

#### Completar Tarefa
```
Ação:
- Clicar na tarefa agendada

Resultado esperado:
✅ Modal/confirmação
✅ Status: DONE
✅ Estatísticas atualizam
✅ Gráfico atualiza
```

---

### B. Teste de IA (3 Análises)

#### 1. Análise de Produtividade
```http
POST http://localhost:8080/api/ai/analyze
Content-Type: application/json

{
  "analysisType": "productivity",
  "timeRange": "today"
}
```

**Resposta esperada:**
```json
{
  "summary": "Você tem X tarefas, com taxa de conclusão de Y%...",
  "insights": [
    "Alto volume de tarefas de alta prioridade",
    "Distribuição equilibrada no calendário",
    "..."
  ],
  "recommendations": [
    "Priorize as 3 tarefas mais importantes",
    "Agrupe tarefas similares",
    "..."
  ],
  "score": 75,
  "timestamp": "2025-11-25T..."
}
```

#### 2. Análise de Padrões
```
Frontend:
1. Selecionar "🔍 Padrões"
2. Clicar "Analisar com IA"

Insights esperados:
- Identificação de horários produtivos
- Padrões de procrastinação
- Sobrecarga cognitiva em períodos
```

#### 3. Recomendações
```
Frontend:
1. Selecionar "💡 Dicas"
2. Clicar "Analisar com IA"

Recomendações esperadas:
- Ações específicas e acionáveis
- Otimização de time-blocking
- Gestão de energia mental
```

---

### C. Teste de Chat Interativo

#### Perguntas Sugeridas
```
1. "Tenho 5 tarefas urgentes, como organizar?"
2. "Qual o melhor horário para deep work?"
3. "Estou procrastinando, por quê?"
4. "Como evitar burnout?"
5. "Devo fazer pausas? Quando?"
```

**Comportamento esperado:**
```
✅ Resposta em 3-5 segundos
✅ Contextualizada com suas tarefas
✅ Prática e acionável
✅ Tom empático e profissional
```

---

### D. Teste de Gráficos

#### Verificar Atualização em Tempo Real
```
Cenário:
1. Criar 2 tarefas → Gráfico mostra 2 Pendentes
2. Agendar 1 tarefa → Gráfico mostra 1 Pendente, 1 Agendada
3. Completar 1 tarefa → Gráfico mostra 1 Concluída
```

#### Verificar Tooltips
```
Ação:
- Passar mouse sobre fatias do gráfico

Resultado:
✅ Mostra nome da categoria
✅ Mostra quantidade absoluta
✅ Mostra percentual
```

---

### E. Teste de Estatísticas

#### Endpoint REST
```http
GET http://localhost:8080/api/tasks/stats
```

**Resposta esperada:**
```json
{
  "totalTasks": 10,
  "pendingTasks": 3,
  "scheduledTasks": 4,
  "doneTasks": 2,
  "lateTasks": 1,
  "completionRate": 20.0
}
```

#### Interface
```
Verificar:
✅ Tarefas Concluídas: número correto
✅ Tarefas Pendentes: número correto
✅ Taxa de conclusão no console (F12)
```

---

### F. Teste de Tags (Backend)

#### Criar Tag
```http
POST http://localhost:8080/api/tags
Content-Type: application/json

{
  "name": "Trabalho",
  "color": "#FF5733"
}
```

**Resposta esperada:**
```json
{
  "id": 1,
  "name": "Trabalho",
  "color": "#FF5733",
  "taskCount": 0
}
```

#### Listar Tags
```http
GET http://localhost:8080/api/tags
```

**Resposta esperada:**
```json
[
  {
    "id": 1,
    "name": "Trabalho",
    "color": "#FF5733",
    "taskCount": 0
  },
  ...
]
```

---

## 🐛 Testes de Erro

### A. IA sem API Key
```
Cenário:
- Não configurar GEMINI_API_KEY
- Clicar "Analisar com IA"

Resultado esperado:
⚠️ "Erro ao conectar com a IA. Verifique se a API key está configurada."
```

### B. Backend Offline
```
Cenário:
- Parar o Spring Boot
- Tentar criar tarefa no frontend

Resultado esperado:
❌ "Erro ao salvar no Java. Veja o console."
✅ Modo offline ativa (tarefa de teste aparece)
```

### C. Validação de Inputs
```
Cenário:
- Criar tarefa sem título

Resultado esperado:
⚠️ "Digite um título!"
```

### D. Tag Duplicada
```http
POST /api/tags
{
  "name": "Trabalho"  // Já existe
}
```

**Resposta esperada:**
```json
{
  "status": 400,
  "message": "Tag com nome 'Trabalho' já existe"
}
```

---

## 📊 Checklist de Testes

### Backend (Spring Boot)
- [ ] Aplicação inicia sem erros
- [ ] Banco PostgreSQL conecta
- [ ] Hibernate cria tabelas (tb_tasks, tags, task_tags)
- [ ] 16 endpoints respondem (200 OK)
- [ ] IA retorna respostas coerentes
- [ ] Logs aparecem no console

### Frontend (Interface)
- [ ] Página carrega corretamente
- [ ] Dark/Light theme funciona
- [ ] Criar tarefa funciona
- [ ] Drag & Drop funciona
- [ ] Estatísticas atualizam
- [ ] Gráfico renderiza
- [ ] IA analisa e responde
- [ ] Chat funciona
- [ ] Nenhum erro no console (F12)

### Integração
- [ ] Frontend ↔ Backend comunicam
- [ ] Dados persistem no PostgreSQL
- [ ] IA recebe contexto correto
- [ ] Atualizações refletem em tempo real

---

## 🎯 Testes de Performance

### Teste de Carga de IA
```
Cenário:
- Enviar 5 análises consecutivas

Resultado esperado:
✅ Todas respondem em <10 segundos
✅ Sem timeout
⚠️ Possível rate limit do Gemini (15 req/min)
```

### Teste de Muitas Tarefas
```
Cenário:
- Criar 50+ tarefas

Resultado esperado:
✅ Lista renderiza corretamente
✅ Drag & Drop continua funcional
✅ Gráfico atualiza
✅ Sem lag perceptível
```

---

## 🔍 Ferramentas de Teste

### Postman/Insomnia
```
Importar coleção:
- 10 endpoints de tarefas
- 2 endpoints de IA
- 4 endpoints de tags
```

### Console do Navegador (F12)
```javascript
// Testar API diretamente
fetch('http://localhost:8080/api/tasks')
  .then(r => r.json())
  .then(console.log);

// Forçar análise de IA
analyzeWithAI();

// Ver estado das tarefas
console.log(tasks);
```

### PostgreSQL Client
```sql
-- Ver tarefas criadas
SELECT * FROM tb_tasks;

-- Ver tags
SELECT * FROM tags;

-- Ver relacionamentos
SELECT * FROM task_tags;
```

---

## ✅ Critérios de Sucesso

**MVP (Mínimo Viável):**
- [x] Criar, listar, agendar tarefas
- [x] IA retorna análises coerentes
- [x] Gráfico renderiza
- [x] Sem erros críticos

**Produção:**
- [x] IA contextualizada com tarefas reais
- [x] Chat funcional e útil
- [x] Interface responsiva
- [x] Error handling robusto
- [x] Documentação completa

---

**🎉 Todos os testes passando = Sistema pronto para uso!**

**Tempo estimado de testes:** 15-20 minutos  
**Prioridade:** Teste de IA primeiro (core feature)
