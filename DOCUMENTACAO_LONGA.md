# NeuroTask (myday-productivity) — Documentação Longa

> Objetivo: este documento é o “mapa do projeto”. Ele explica o que o NeuroTask faz, como as peças se conectam e onde mexer quando você quiser evoluir ou debugar.

## Sumário

- Visão do produto
- Conceitos do domínio
- Arquitetura e estrutura de pastas
- Backend (Spring Boot)
- Banco de dados (MySQL)
- IA (Google Gemini via LangChain4j)
- Analytics
- Web Push / PWA
- Frontend (static/)
- Fluxos end-to-end
- Como rodar em desenvolvimento
- Troubleshooting
- Convenções do projeto (padrões)
- Limitações conhecidas e próximos passos

---

## 1) Visão do produto

O **NeuroTask** é um gerenciador de produtividade com ênfase em **time-blocking** (planejamento por blocos de horário) e em **insights** (analytics + IA). A proposta é: em vez de apenas listar tarefas, o sistema ajuda a **organizar o dia em blocos**, reduzir conflitos, e trazer leituras sobre padrões e produtividade.

Em termos práticos, o sistema entrega:

- **Backlog / To Do**: onde tarefas sem horário ficam armazenadas.
- **Calendário (agenda)**: onde tarefas com horário aparecem (time-blocking).
- **Hábitos**: uma tela que sugere encaixe de hábitos em janelas livres do dia e pode transformar hábitos em blocos agendados.
- **Analytics**: métricas sobre status, prioridade, produtividade, atrasos e tempo médio.
- **IA (Gemini)**: análise estruturada e chat de “coach”.
- **PWA + Web Push**: para experiência instalável e notificações.

---

## 2) Conceitos do domínio

### 2.1 Tarefa

A entidade central é a **Task** (tarefa). Uma tarefa pode (ou não) ter:

- `title` (título)
- `description` (descrição)
- `priority` (prioridade)
- `startTime` / `endTime` (horário planejado)
- `status` (estado no workflow)
- (opcional) tags

### 2.2 Status (máquina de estados)

O projeto usa o status para representar onde a tarefa vive no fluxo:

- **PENDING**: tarefa de backlog (normalmente sem horário).
- **SCHEDULED**: tarefa agendada no calendário (com `startTime/endTime`).
- **DONE**: tarefa concluída.
- (há referências a **LATE** em endpoints/analytics; depende da lógica do service)

A transição mais importante é:

- `PENDING → SCHEDULED` quando a tarefa é movida para um slot do calendário.

### 2.3 Time-blocking e validação de conflito

Ao agendar uma tarefa, o backend aplica uma regra de negócio essencial: **não permitir sobreposição com outras tarefas agendadas**.

Isso garante integridade do calendário (um slot não pode ter duas tarefas simultâneas) e evita que o frontend precise “adivinhar” conflitos.

### 2.4 Hábitos (conceito)

A tela de hábitos é um componente de “planejamento assistido”. Ela:

- Observa o dia selecionado (tarefas agendadas)
- Identifica intervalos ocupados
- Calcula janelas livres no intervalo **00:00–24:00**
- Sugere um slot para um hábito (5/10/15 min, ou duração mínima maior em alguns hábitos)
- Permite “Adicionar ao meu dia”, transformando o hábito em uma tarefa agendada
- Permite marcar hábito como “Concluído” e “Adotar” (persistência local)

### 2.5 Tags

O sistema inclui um modelo de **tags** (many-to-many com tarefas), permitindo categorizar por contexto (trabalho, pessoal, estudos, etc.).

---

## 3) Arquitetura e estrutura de pastas

### 3.1 Backend (Java)

O código Java vive em:

- `src/main/java/com/gustavocirino/myday_productivity/`

Estrutura típica:

- `controller/` — endpoints REST
- `service/` — regras de negócio e orquestração
- `service/ai/` — integração com Gemini + prompts e parsing
- `service/ai/analytics/` — métricas
- `service/push/` — Web Push (VAPID, subscriptions)
- `repository/` — Spring Data JPA
- `model/` — entidades JPA
- `model/enums/` — enums do domínio
- `dto/` — DTOs (records)
- `config/` — CORS, OpenAPI, AI client, web config

### 3.2 Frontend (static)

O frontend servido pelo Spring Boot fica em:

- `src/main/resources/static/`

Principais itens:

- `index.html` — UI principal
- `js/` — scripts (inclui módulos como `api/taskService.js`, `modules/*`)
- `css/` — estilos
- `sw.js` e `manifest.webmanifest` — PWA

> Observação: existem módulos JS prontos, mas a orquestração pode variar dependendo do caminho de UI (o projeto também tem bastante lógica no `index.html`).

---

## 4) Backend (Spring Boot 3.5.7, Java 21)

### 4.1 Camadas e responsabilidades

Padrão de arquitetura:

- **Controller → Service → Repository → Database**

Regras:

- Controller é “stateless”: recebe HTTP, valida o básico e delega tudo ao Service.
- Service concentra a regra de negócio: transições de status, validações de conflito, integração com IA, cálculos de métricas.
- Repository é interface JPA (persistência).

### 4.2 Principais Controllers (mapa rápido)

- **Tasks**: `TaskController` (`/api/tasks`)

  - CRUD
  - mover para calendário (`/move`)
  - completar (`/complete`) e desmarcar conclusão (`/uncomplete`)
  - voltar ao backlog (`/backlog`)
  - stats (`/stats`)

- **IA**: `AIController` (`/api/ai`)

  - `/analyze` (tipos: productivity, patterns, recommendations)
  - `/chat`

- **Analytics**: `AnalyticsController` (`/api/analytics`)

  - `/stats`, `/today`, `/priority`, `/status`, `/productivity`, `/overdue`, `/avg-time`, `/summary`

- **Tags**: `TagController` (`/api/tags`)

  - list/create/get/delete

- **Push**: `PushController` (`/api/push`)

  - `/vapid-public-key`, `/subscribe`, `/unsubscribe`

- `CalendarioController` e `UserController` podem existir como placeholders (dependendo do estado atual do projeto).

### 4.3 Configurações importantes

- **OpenAPI/Swagger**: via `springdoc-openapi-starter-webmvc-ui`.
- **CORS**: liberado em alguns controllers (ex.: `@CrossOrigin(origins = "*")`).
- **Logs**: hibernate SQL habilitado (útil para debug, mas verboso).

---

## 5) Banco de dados (MySQL)

### 5.1 Configuração atual

A configuração padrão do projeto está em `src/main/resources/application.properties`:

- MySQL em `localhost:3306`
- DB `neurotask` com `createDatabaseIfNotExist=true`
- Hibernate `ddl-auto=update` (cria/atualiza tabelas automaticamente)

### 5.2 Observação de segurança (importante)

Evite manter senha de banco e API keys diretamente versionadas. Para desenvolvimento, funciona; para qualquer ambiente compartilhado, prefira variáveis de ambiente.

---

## 6) IA (Google Gemini via LangChain4j)

### 6.1 O que a IA faz

A IA tem dois modos:

- **Análise estruturada** (`/api/ai/analyze`): retorna resumo, insights e recomendações.
- **Chat** (`/api/ai/chat`): conversa contextual (coach).

### 6.2 Por que “resposta estruturada” é crítica

O service de IA normalmente precisa de uma resposta previsível para exibir bem na UI. Por isso, prompts usam marcadores (ex.: `SUMMARY:`, `INSIGHTS:`, `RECOMMENDATIONS:`) e o código faz parsing.

### 6.3 Configuração

O projeto usa propriedades como:

- `gemini.model.name`
- `gemini.temperature`
- `gemini.max.tokens`

> Recomendação: use `GEMINI_API_KEY` via variável de ambiente e mantenha `application.properties` sem segredos.

---

## 7) Analytics

O módulo de analytics consolida números úteis para entender evolução e gargalos:

- distribuição por status
- distribuição por prioridade
- taxa de produtividade
- tarefas atrasadas
- tempo médio de conclusão
- resumo por período (`/api/analytics/summary?start=...&end=...`)

O objetivo é alimentar:

- cards e indicadores na UI
- (opcional) relatórios e análises de IA

---

## 8) Web Push / PWA

O projeto tem estrutura de PWA:

- `manifest.webmanifest`
- `sw.js`

E endpoints de push:

- obter chave pública VAPID
- registrar subscription
- remover subscription

Isso permite notificações mesmo com aba fechada (dependendo do suporte do navegador e permissão do usuário).

---

## 9) Frontend (Vanilla JS)

### 9.1 Ideia geral

O frontend é “frameworkless”: HTML/CSS + JS (módulos ES6). Isso facilita manter simples e rápido, mas exige disciplina na organização de estado e renderização.

Conceitos típicos do front:

- um estado global com `tasks` e `selectedDay`
- renderizadores por visão (calendário, diário, hábitos, todo)
- chamadas `fetch()` para `/api/*`

### 9.2 Tela de Hábitos (explicação conceitual)

A tela de hábitos é um sistema de **encaixe**:

1. Escolha de dia (hoje / anterior / próximo)
2. Leitura do que está ocupado (tarefas com horário)
3. Cálculo do que está livre
4. Sugestão de um slot para o hábito selecionado
5. Ação de “Adicionar ao meu dia” cria um bloco agendado
6. “Adotar” e “Concluído” são estados persistidos localmente

Mesmo sem olhar o código, a regra mental é:

- **O que está no calendário ocupa tempo**
- **O hábito só pode ser sugerido em uma janela livre que caiba**
- **Hoje evita sugerir no passado**

---

## 10) Fluxos end-to-end (o que acontece de ponta a ponta)

### 10.1 Criar tarefa no backlog

- UI → `POST /api/tasks`
- Backend cria tarefa (normalmente `PENDING` se vier sem `startTime/endTime`)

### 10.2 Agendar tarefa (time-blocking)

- UI → `PATCH /api/tasks/{id}/move`
- Backend valida conflito com outras `SCHEDULED`
- Se ok: persiste `startTime/endTime` e status `SCHEDULED`

### 10.3 Concluir / desfazer conclusão

- UI → `PATCH /api/tasks/{id}/complete` ou `PATCH /api/tasks/{id}/uncomplete`

### 10.4 IA — análise

- UI → `POST /api/ai/analyze` com `{ analysisType, timeRange }`
- Backend monta contexto e chama Gemini
- Backend faz parsing da resposta e retorna estruturado

### 10.5 IA — chat

- UI → `POST /api/ai/chat` com `{ message }`
- Backend faz chamada ao modelo e retorna `{ response, timestamp }`

### 10.6 Hábitos — adicionar ao dia

- UI sugere um horário
- Ao confirmar: `POST /api/tasks` já com `startTime/endTime` e status agendado

---

## 11) Como rodar em desenvolvimento (Windows)

### 11.1 Pré-requisitos

- Java 21
- MySQL em `localhost:3306`

### 11.2 Rodar o backend

Na raiz do projeto:

- `mvnw.cmd spring-boot:run`

Acesso:

- App: `http://localhost:8080`
- Swagger: `http://localhost:8080/swagger-ui/index.html`

### 11.3 Configurar Gemini

Recomendado:

- Definir variável de ambiente `GEMINI_API_KEY`

E no `application.properties` usar placeholder (ao invés de chave fixa).

---

## 12) Troubleshooting (problemas comuns)

### 12.1 Não conecta no MySQL

- Verifique se o serviço MySQL está rodando
- Confira porta 3306
- Confira usuário/senha
- Veja logs do Hibernate para detalhes

### 12.2 IA não responde

- Verifique se a API key está válida
- Confirme tempo de timeout e limites de tokens
- Veja logs do service de IA

### 12.3 Tarefas não aparecem onde deveriam

Checklist mental:

- Backlog/To Do: tarefas `PENDING` (sem horário)
- Calendário/Agenda: tarefas `SCHEDULED` (com `startTime/endTime`)
- Se uma tarefa não tem horário, ela não deveria “ocupar” o relógio/agenda

### 12.4 Push não funciona

- Navegador precisa suportar Web Push
- É necessário conceder permissão
- Service Worker precisa estar ativo

---

## 13) Convenções do projeto (padrões)

- **DTOs**: use `record` (imutável)
- **Entidades JPA**: getters/setters manuais (evitar Lombok em entity)
- **DI**: preferir constructor injection
- **Enums no banco**: `EnumType.STRING` (nunca ordinal)
- **Controllers**: sem regra de negócio
- **Services**: regra de negócio + validações + orquestração

---

## 14) Limitações conhecidas e próximos passos

Alguns pontos comuns para evolução:

- Separar/organizar melhor a orquestração do frontend (reduzir lógica inline, consolidar módulos)
- Adicionar autenticação multiusuário
- Melhorar configuração segura (remover segredos do `application.properties`)
- Adicionar migrações (Flyway/Liquibase) se o schema começar a crescer
- Evoluir hábitos (catálogo customizável, histórico, streaks)

---

## Apêndice A — Referências rápidas

- Endpoints de tarefas: `/api/tasks`
- Endpoints de IA: `/api/ai`
- Endpoints de analytics: `/api/analytics`
- Endpoints de tags: `/api/tags`
- Endpoints de push: `/api/push`
