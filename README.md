# 🧠 NeuroTask - Cognitive Productivity Manager

## 🎯 Visão Geral

**NeuroTask** é um sistema de produtividade cognitiva que combina **time-blocking**, **IA generativa** e **análise comportamental** para otimizar sua gestão de tarefas e energia mental.

### 📚 Documentação

- Consulte a documentação completa do projeto em `DOCUMENTACAO_LONGA.md`.

### 🌟 Diferenciais

- 🤖 **Coach de IA com Google Gemini**: Análises reais de produtividade, padrões e recomendações personalizadas
- 📅 **Time-Blocking Visual**: Drag & Drop intuitivo para agendar tarefas
- 📊 **Análises Cognitivas**: Identifica sobrecarga mental e otimiza distribuição de tarefas
- 💬 **Chat Interativo**: Converse com um coach especializado em produtividade
- 📈 **Gráficos em Tempo Real**: Visualize sua evolução com Chart.js
- 🏷️ **Sistema de Tags**: Organize tarefas por contextos (trabalho, pessoal, estudos)

---

## 🛠️ Stack Tecnológica

### Backend

- **Java 21** (LTS)
- **Spring Boot 3.5.7**
- **MySQL**
- **LangChain4j 0.36.2** (Integração com IA)
- **Google Gemini 1.5 Flash** (Modelo de IA)
- **JPA/Hibernate**
- **Lombok**

### Frontend

- **Vanilla JavaScript ES6+** (Zero frameworks)
- **Chart.js 4.4.0** (Gráficos)
- **CSS Grid/Flexbox**
- **Drag & Drop API nativa**
- **Dark/Light theme**

---

## 🚀 Início Rápido

### 1️⃣ Pré-requisitos

```bash
✅ Java 21
✅ MySQL (localhost)
✅ Maven (incluído via mvnw)
✅ Google Gemini API Key (grátis)
```

### 2️⃣ Configurar Banco de Dados

O projeto está configurado para conectar no MySQL em `localhost:3306` e criar o database automaticamente (se não existir).

### 3️⃣ Obter API Key do Gemini

1. Acesse: https://aistudio.google.com/app/apikey
2. Clique em "Create API Key"
3. Copie a chave gerada

### 4️⃣ Configurar Variável de Ambiente

```cmd
# Windows (CMD)
set GEMINI_API_KEY=SuaAPIKeyAqui

# Windows (PowerShell)
$env:GEMINI_API_KEY="SuaAPIKeyAqui"

# Linux/Mac
export GEMINI_API_KEY=SuaAPIKeyAqui
```

### 5️⃣ Executar Aplicação

```bash
cd myday-productivity
mvnw spring-boot:run
```

### 6️⃣ Acessar

Abra: **http://localhost:8080**

---

## 📖 Como Usar

### 1. **Adicionar Tarefas ao Backlog**

- Digite título, descrição e prioridade
- Clique em "+ Adicionar"
- As tarefas ficam no status `PENDING`

### 2. **Time-Blocking (Drag & Drop)**

- Arraste tarefas do backlog para slots de horário
- Tarefas mudam para status `SCHEDULED`
- Clique na tarefa no calendário para completar

### 3. **Análise com IA**

- Clique em "Analisar com IA"
- Escolha o tipo:
  - **📊 Produtividade**: Análise geral
  - **🔍 Padrões**: Identifica comportamentos
  - **💡 Dicas**: Recomendações acionáveis

### 4. **Chat com Coach de IA**

- Digite perguntas no campo de chat
- Exemplos:
  - "Como organizar melhor meu dia?"
  - "Tenho muitas tarefas atrasadas, o que fazer?"
  - "Qual o melhor horário para tarefas de alta prioridade?"

### 5. **Visualizar Estatísticas**

- Gráfico de rosca mostra distribuição de tarefas
- Estatísticas atualizadas em tempo real

---

## 🎨 Funcionalidades Implementadas

### ✅ Gestão de Tarefas

- [x] CRUD completo de tarefas
- [x] Backlog com prioridades (HIGH, MEDIUM, LOW)
- [x] Time-blocking visual (8h-20h)
- [x] Drag & Drop nativo
- [x] Status: PENDING → SCHEDULED → DONE
- [x] Mover tarefas de volta ao backlog

### ✅ Inteligência Artificial

- [x] Integração com Google Gemini via LangChain4j
- [x] 3 tipos de análise (Produtividade, Padrões, Recomendações)
- [x] Chat interativo com contexto das tarefas
- [x] Prompts otimizados para análise cognitiva
- [x] Parse estruturado de respostas da IA

### ✅ Visualizações

- [x] Gráfico de rosca (Chart.js)
- [x] Estatísticas em tempo real
- [x] Taxa de conclusão
- [x] Distribuição por status

### ✅ Sistema de Tags

- [x] Criar tags com cores customizadas
- [x] Relacionamento Many-to-Many com tarefas
- [x] Endpoints REST completos

### ✅ UX/UI

- [x] Dark/Light theme toggle
- [x] Design glassmorphism
- [x] Responsivo
- [x] Feedback visual de ações

---

## 🔌 API Endpoints

### 📋 Tarefas (`/api/tasks`)

```http
GET    /api/tasks                  # Listar todas
POST   /api/tasks                  # Criar tarefa
PATCH  /api/tasks/{id}/move        # Mover para calendário
PATCH  /api/tasks/{id}/complete    # Marcar como concluída
PUT    /api/tasks/{id}             # Atualizar tarefa
DELETE /api/tasks/{id}             # Remover tarefa
GET    /api/tasks/stats            # Estatísticas
GET    /api/tasks/status/{status}  # Filtrar por status
PATCH  /api/tasks/{id}/backlog     # Voltar ao backlog
```

### 🤖 IA (`/api/ai`)

```http
POST /api/ai/analyze  # Análise de produtividade
POST /api/ai/chat     # Chat com coach
```

**Request Body (analyze):**

```json
{
  "analysisType": "productivity",
  "timeRange": "today"
}
```

**Request Body (chat):**

```json
{
  "message": "Como posso melhorar minha produtividade?"
}
```

### 🏷️ Tags (`/api/tags`)

```http
GET    /api/tags       # Listar todas
POST   /api/tags       # Criar tag
GET    /api/tags/{id}  # Buscar por ID
DELETE /api/tags/{id}  # Remover tag
```

---

## 🧪 Testando a IA

### Exemplo 1: Análise de Produtividade

```bash
curl -X POST http://localhost:8080/api/ai/analyze \
  -H "Content-Type: application/json" \
  -d '{
    "analysisType": "productivity",
    "timeRange": "today"
  }'
```

### Exemplo 2: Chat

```bash
curl -X POST http://localhost:8080/api/ai/chat \
  -H "Content-Type: application/json" \
  -d '{
    "message": "Tenho 5 tarefas de alta prioridade, como organizar?"
  }'
```

---

## 📊 Arquitetura

```
┌─────────────────────────────────────────────────────┐
│                   Frontend (HTML/JS)                 │
│  • Drag & Drop • Chart.js • Fetch API               │
└────────────────┬────────────────────────────────────┘
                 │ REST API
┌────────────────▼────────────────────────────────────┐
│              Controller Layer                        │
│  TaskController • AIController • TagController       │
└────────────────┬────────────────────────────────────┘
                 │
┌────────────────▼────────────────────────────────────┐
│               Service Layer                          │
│  TaskService • ProductivityAnalysisService           │
│  TagService                                          │
└────────────────┬────────────────────────────────────┘
                 │
┌────────────────▼────────────────────────────────────┐
│          Repository Layer (JPA)                      │
│  TaskRepository • TagRepository                      │
└────────────────┬────────────────────────────────────┘
                 │
┌────────────────▼────────────────────────────────────┐
│            PostgreSQL Database                       │
│  tb_tasks • tags • task_tags                        │
└──────────────────────────────────────────────────────┘

           External API
┌──────────────────────────────────────────────────────┐
│         Google Gemini AI (via LangChain4j)           │
│  Gemini 1.5 Flash • ChatLanguageModel                │
└──────────────────────────────────────────────────────┘
```

---

## 🎓 Conceitos Aplicados

### Design Patterns

- **Repository Pattern**: Abstração de acesso a dados
- **DTO Pattern**: Transferência de dados entre camadas
- **Service Layer**: Lógica de negócio centralizada
- **Dependency Injection**: Spring IoC Container

### Boas Práticas

- **Separation of Concerns**: Controllers apenas roteiam, Services contêm lógica
- **Single Responsibility**: Cada classe tem uma responsabilidade única
- **Open/Closed Principle**: Aberto para extensão, fechado para modificação
- **Clean Code**: Nomes descritivos, métodos pequenos, SOLID

### Tecnologias Modernas

- **Java Records**: DTOs imutáveis (Java 14+)
- **LangChain4j**: Framework para aplicações LLM
- **Chart.js**: Gráficos declarativos e responsivos
- **Native Drag & Drop**: Sem dependências jQuery/React

---

## 🔐 Segurança

### Implementado

- ✅ Validação de entrada com Bean Validation
- ✅ Exception handling centralizado
- ✅ Variáveis de ambiente para secrets

---

## 📦 Implementações Recentes (Novembro 2025)

### ✅ Sistema de Analytics Completo

**AnalyticsService + AnalyticsController** com 7 endpoints:

- `GET /api/analytics/stats` - Estatísticas gerais
- `GET /api/analytics/today` - Métricas do dia
- `GET /api/analytics/priority` - Distribuição por prioridade
- `GET /api/analytics/status` - Distribuição por status
- `GET /api/analytics/productivity` - Taxa de produtividade (0-100)
- `GET /api/analytics/overdue` - Tarefas atrasadas
- `GET /api/analytics/avg-time` - Tempo médio de conclusão

**Cálculos implementados:**

- Taxa de conclusão
- Identificação de tarefas atrasadas (LATE)
- Tempo médio de conclusão em horas
- Distribuição por prioridade/status

### ✅ Validação de Conflitos de Horário

**Regra de negócio crítica**: Impede agendamento de tarefas em slots ocupados.

**Implementação** (`TaskService.validateTimeSlotConflict()`):

- Detecta overlaps de horário entre tarefas SCHEDULED
- Exclui a própria tarefa ao reagendar
- Lança `IllegalArgumentException` com detalhes do conflito

### ✅ Documentação Swagger/OpenAPI

**SpringDoc OpenAPI 2.8.4** integrado:

- `GET /swagger-ui/index.html` - Interface interativa
- `GET /v3/api-docs` - Especificação JSON
- Controllers documentados com @Operation/@ApiResponse

### ✅ Frontend Modular com ES6

**Arquitetura de módulos JavaScript:**

1. `taskService.js` - Camada de API (11 métodos fetch)
2. `dragController.js` - HTML5 Drag & Drop
3. `calendarView.js` - Renderização do calendário
4. `aiFeedback.js` - Dashboard Chart.js + AI
5. `app.js` - Orquestrador principal

**Melhorias:**

- Removido ~500 linhas de script inline
- ES6 import/export
- Event listeners modernos
- State management local

### ✅ Dashboard de Analytics com Chart.js

**Visualizações:**

- Gráfico doughnut de distribuição por prioridade
- Painel de estatísticas dinâmicas
- Atualização automática ao mudar tarefas
- Cores consistentes com tema dark/light

---

### Próximos Passos

- [ ] Autenticação JWT
- [ ] Rate limiting para IA
- [ ] HTTPS em produção
- [ ] Sanitização de inputs

---

## 📈 Roadmap

### Versão 1.1 (Em Breve)

- [ ] Autenticação de usuários (Spring Security + JWT)
- [ ] Notificações push (WebSockets)
- [ ] Relatórios PDF com análises de IA
- [ ] Integração com Google Calendar

### Versão 2.0 (Futuro)

- [ ] App mobile (React Native + Expo)
- [ ] Modo offline (IndexedDB + Service Workers)
- [ ] Sincronização multi-dispositivo
- [ ] Gamificação (achievements, streaks)

---

## 🤝 Contribuindo

1. Fork o projeto
2. Crie uma branch: `git checkout -b feature/nova-funcionalidade`
3. Commit: `git commit -m 'Add: nova funcionalidade'`
4. Push: `git push origin feature/nova-funcionalidade`
5. Abra um Pull Request

---

## 📄 Licença

MIT License - Use livremente!

---

## 👨‍💻 Autor

**Gustavo Cirino**

- GitHub: [@gustavocirino](https://github.com/gustavocirino)
- LinkedIn: [Gustavo Cirino](https://linkedin.com/in/gustavocirino)

---

## 🙏 Agradecimentos

- **Google AI Studio**: API Gemini gratuita
- **LangChain4j**: Framework LLM em Java
- **Spring Team**: Melhor framework do ecossistema Java
- **Chart.js**: Gráficos lindos sem esforço

---

**Desenvolvido com ❤️ e ☕ | Spring Boot + Google Gemini AI**
