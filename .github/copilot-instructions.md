# NeuroTask - Instruções para AIs

## Visão geral
- Backend Spring Boot 3.5.7 (Java 21) com MySQL; frontend em HTML/CSS + JS ES6 em [src/main/resources/static](src/main/resources/static).
- Pacote base: [src/main/java/com/gustavocirino/myday_productivity](src/main/java/com/gustavocirino/myday_productivity).
- Domínio principal: gestão de tarefas com time-blocking, validação de conflitos de horário e análises de produtividade com IA (Google Gemini via LangChain4j).

## Arquitetura & padrões
- Camadas: Controller → Service → Repository → DB; controllers apenas roteiam, toda regra de negócio fica em services.
- Use injeção por construtor com Lombok `@RequiredArgsConstructor` em controllers/services; não usar `@Autowired` em campos.
- DTOs são Java Records em [dto](src/main/java/com/gustavocirino/myday_productivity/dto) (imutáveis, só para fronteira HTTP).
- Entidades JPA em [model](src/main/java/com/gustavocirino/myday_productivity/model) com getters/setters manuais; enums sempre com `@Enumerated(EnumType.STRING)` e definidos em [model/enums](src/main/java/com/gustavocirino/myday_productivity/model/enums).
- Repositórios Spring Data em [repository](src/main/java/com/gustavocirino/myday_productivity/repository) estendem `JpaRepository` sem lógica adicional.

## Fluxos de domínio importantes
- Máquina de estados de tarefas em [TaskService](src/main/java/com/gustavocirino/myday_productivity/service/TaskService.java): `PENDING` → (drag para calendário) → `SCHEDULED` → (concluir) → `DONE`; é possível voltar `SCHEDULED` → `PENDING`.
- Validação crítica de conflito de horários em `TaskService.moveTask()` / `validateTimeSlotConflict(...)`: antes de mudar start/end, verifica `SCHEDULED` existentes e lança `IllegalArgumentException` com detalhes do conflito.
- Analytics de produtividade em [AnalyticsService](src/main/java/com/gustavocirino/myday_productivity/service/ai/analytics/AnalyticsService.java) e [AnalyticsController](src/main/java/com/gustavocirino/myday_productivity/controller/AnalyticsController.java) com endpoints `/api/analytics/*` (stats, today, priority, status, productivity, overdue, avg-time).
- Novos endpoints devem seguir o padrão dos controllers existentes em [controller](src/main/java/com/gustavocirino/myday_productivity/controller): métodos finos chamando services e retornando DTOs/`ResponseEntity`.

## Integração com IA (Google Gemini)
- Configuração principal em [AIClientConfig](src/main/java/com/gustavocirino/myday_productivity/config/AIClientConfig.java): cria um `ChatLanguageModel` Gemini 1.5 Flash usando a variável de ambiente `GEMINI_API_KEY`.
- Serviço de análise em [ProductivityAnalysisService](src/main/java/com/gustavocirino/myday_productivity/service/ai/ProductivityAnalysisService.java); ele constrói prompts e faz parsing das respostas para `AIAnalysisResponseDTO`.
- Prompts de análise exigem formato ESTRITO com marcadores `SUMMARY:`, `INSIGHTS:` e `RECOMMENDATIONS:`; se alterar o formato, também atualize o parsing em `parseAIResponse()` (`extractSection` / `extractList`).
- Para adicionar um novo tipo de análise, siga o padrão do `switch` em `analyzeProductivity(...)` e extraia um método `buildXPrompt(...)` reutilizando a mesma estrutura de resposta.

## Frontend (static)
- Frontend vive em [src/main/resources/static](src/main/resources/static); o entrypoint é [js/app.js](src/main/resources/static/js/app.js).
- Módulos JS em [js/modules](src/main/resources/static/js/modules) implementam: drag & drop ([dragController.js](src/main/resources/static/js/modules/dragController.js)), calendário ([calendarView.js](src/main/resources/static/js/modules/calendarView.js)) e dashboard de IA/analytics ([aiFeedback.js](src/main/resources/static/js/modules/aiFeedback.js)).
- Acesso à API sempre via `fetch` encapsulado em [js/api/taskService.js](src/main/resources/static/js/api/taskService.js); não introduza Axios/jQuery.
- `app.js` deve apenas orquestrar módulos (registrar listeners, inicializar calendário, recarregar dados) sem lógica de negócio; qualquer regra de domínio nova deve permanecer no backend.

## Workflows de desenvolvimento
- Pré-requisitos: MySQL em `localhost:3306` (BD `neurotask` é criado automaticamente) e variável `GEMINI_API_KEY` definida; properties em [src/main/resources/application.properties](src/main/resources/application.properties).
- Para rodar em desenvolvimento, use a task `spring-boot:run` (ou `mvnw.cmd spring-boot:run`) na raiz do projeto; app sobe em `http://localhost:8080`.
- Build de produção: `mvnw.cmd -DskipTests package` gera o JAR em `target/`; há tasks VS Code para rodar o JAR em 8083 e executar smoke tests Playwright em [qa/playwright-smoke.mjs](qa/playwright-smoke.mjs).
- Testes Java vivem em [src/test/java/com/gustavocirino/myday_productivity](src/test/java/com/gustavocirino/myday_productivity); ao criar novos services/controllers, siga os padrões de teste existentes.
- **IMPORTANTE:** Credenciais MySQL hardcoded em `application.properties` (user: `root`, password: `@Gucirino01`); modifique se necessário.

## Convenções de código específicas
- **Sem Lombok em entidades:** Entidades JPA usam getters/setters manuais, mas controllers/services usam `@RequiredArgsConstructor` + `@Slf4j`.
- **Enums sempre STRING:** Nunca use `EnumType.ORDINAL`, sempre `EnumType.STRING` para evitar problemas com mudanças de ordem.
- **Prefixo de tabelas:** Todas as tabelas DB usam prefixo `tb_` (ex: `tb_tasks`, `tb_tags`).
- **Sem frameworks frontend:** Use apenas Vanilla JS + ES6 modules; Chart.js é a única lib permitida para gráficos.
- **Respostas IA em português:** Todos os prompts e respostas da IA devem ser em português brasileiro.

## Quando em dúvida
- Priorize seguir padrões já usados em arquivos vizinhos.
- Consulte [DOCUMENTACAO_LONGA.md](DOCUMENTACAO_LONGA.md) e [HELP.md](HELP.md) para detalhes mais extensos de domínio e setup antes de introduzir novos conceitos arquiteturais.

## Package Structure

**Base package:** `com.gustavocirino.myday_productivity`

All Java files follow this convention - the placeholder `com.seuusuario.*` mentioned in old docs has been **replaced**.

## Architecture Patterns

### Layer Responsibilities

```
Controller → Service → Repository → Database
   ↓          ↓           ↓
  DTO    Business Logic  JPA
```

- **Controllers** (`controller/`): Stateless HTTP handlers - transform requests to DTOs, delegate to services, return DTOs
- **Services** (`service/`): All business logic, orchestration, AI prompting
  - `service/TaskService` - CRUD + time-blocking state transitions + conflict validation
  - `service/ai/ProductivityAnalysisService` - AI analysis & chat
  - `service/ai/analytics/AnalyticsService` - Metrics calculation and productivity insights
  - `service/TagService` - Tag management
- **Repositories** (`repository/`): Spring Data JPA interfaces only - no custom queries needed
- **DTOs** (`dto/`): Java Records for API contracts (immutable, auto-generated methods)
- **Models** (`model/`): JPA entities with manual getters/setters (NO Lombok on entities)
- **Config** (`config/`): Spring beans for CORS, AI client, etc.

### Dependency Injection Pattern

**Always use constructor injection** (Spring best practice):

```java
@RestController
@RequiredArgsConstructor  // Lombok generates constructor
@Slf4j                    // Lombok logger
public class TaskController {
    private final TaskService taskService;  // Injected via constructor
}
```

Never use `@Autowired` field injection. `@RequiredArgsConstructor` is the standard pattern throughout the codebase.

### DTO Pattern (Java Records)

All DTOs use **Java 16+ Records** for immutability:

```java
public record TaskResponseDTO(
    Long id,
    String title,
    TaskStatus status,
    LocalDateTime startTime
) {}
```

Records auto-generate: constructor, getters, `equals()`, `hashCode()`, `toString()`. Do NOT create traditional classes for DTOs.

### Entity Pattern (Manual POJOs)

Entities use **manual getters/setters** (NO Lombok):

```java
@Entity
@Table(name = "tb_tasks")
public class Task {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)  // Always STRING for enums
    private TaskStatus status;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
}
```

**Critical:** Enums stored as `EnumType.STRING` in DB (not ordinal). All enums live in `model/enums/`.

## AI Integration (Google Gemini via LangChain4j)

### Configuration Flow

1. **API Key:** Set environment variable `GEMINI_API_KEY` (required at startup)
2. **Config Bean:** `AIClientConfig` creates `ChatLanguageModel` bean using properties from `application.properties`
3. **Service Injection:** `ProductivityAnalysisService` injects `ChatLanguageModel` for AI calls

```java
// AIClientConfig.java
@Bean
public ChatLanguageModel chatLanguageModel() {
    return GoogleAiGeminiChatModel.builder()
        .apiKey(apiKey)                    // From ${GEMINI_API_KEY}
        .modelName("gemini-1.5-flash")     // Fast model
        .temperature(0.7)                  // Balanced creativity
        .maxOutputTokens(2048)             // Response length
        .timeout(Duration.ofSeconds(60))
        .logRequestsAndResponses(true)     // Debug logging
        .build();
}
```

### Prompt Engineering Pattern

**Structured prompts with EXACT format requirements** for parseable responses:

```java
String prompt = """
    Você é um especialista em produtividade cognitiva. Analise os seguintes dados:

    %s

    Forneça uma análise ESTRUTURADA neste formato EXATO:
    SUMMARY: [1-2 frases sobre o estado geral da produtividade]
    INSIGHTS: [Insight 1]; [Insight 2]; [Insight 3]
    RECOMMENDATIONS: [Recomendação 1]; [Recomendação 2]; [Recomendação 3]

    Seja específico, direto e acionável.
    """.formatted(context);
```

**Critical:** AI responses are parsed with `extractSection()` and `extractList()` methods - prompts MUST specify exact markers (`SUMMARY:`, `INSIGHTS:`, `RECOMMENDATIONS:`) for parsing to work.

### AI Analysis Types

- `productivity` - General health check with completion rate score
- `patterns` - Identifies behavioral patterns (cognitive load, time-blocking effectiveness)
- `recommendations` - Actionable tips prioritized by impact vs effort

All types use same response structure (`AIAnalysisResponseDTO`) but different prompts.

## Database Configuration (MySQL)

### Connection Settings (`application.properties`)

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/neurotask?createDatabaseIfNotExist=true&useSSL=false&serverTimezone=America/Sao_Paulo
spring.datasource.username=root
spring.datasource.password=@Gucirino01
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver
spring.jpa.hibernate.ddl-auto=update  # Auto-creates/updates schema
spring.jpa.show-sql=true              # Educational: logs SQL
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQLDialect
```

**Critical:** Application uses MySQL on `localhost:3306` with `createDatabaseIfNotExist=true` (auto-creates DB). Credentials are hardcoded in properties file.

### Schema Management

- **DDL Auto:** `update` mode - Hibernate auto-generates schema from entities on startup
- **Naming:** Tables use `tb_` prefix (e.g., `tb_tasks`), join tables use underscore (e.g., `task_tags`)
- **No migrations:** Project doesn't use Flyway/Liquibase - schema evolves via JPA annotations

### Task Entity Schema

```sql
CREATE TABLE tb_tasks (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    start_time TIMESTAMP,
    end_time TIMESTAMP,
    priority VARCHAR(10),  -- HIGH, MEDIUM, LOW
    status VARCHAR(20),    -- PENDING, SCHEDULED, DONE, LATE
    created_at TIMESTAMP
);

CREATE TABLE task_tags (
    task_id BIGINT REFERENCES tb_tasks(id),
    tag_id BIGINT REFERENCES tags(id),
    PRIMARY KEY (task_id, tag_id)
);
```

## Development Workflows

### Prerequisites

```cmd
# 1. MySQL must be running on localhost:3306
# Database 'neurotask' is auto-created by Spring Boot

# 2. Set Gemini API key (get from https://aistudio.google.com/app/apikey)
set GEMINI_API_KEY=your_key_here

# 3. Verify Java 21
java -version
```

### Build & Run (Windows cmd.exe)

```cmd
# Run application (hot reload enabled)
mvnw.cmd spring-boot:run

# Build JAR
mvnw.cmd clean package

# Run JAR
java -jar target\myday-productivity-0.0.1-SNAPSHOT.jar

# Run tests
mvnw.cmd test
```

### Testing API Endpoints

```bash
# Create task
curl -X POST http://localhost:8080/api/tasks ^
  -H "Content-Type: application/json" ^
  -d "{\"title\":\"Test\",\"description\":\"...\",\"priority\":\"HIGH\"}"

# AI analysis
curl -X POST http://localhost:8080/api/ai/analyze ^
  -H "Content-Type: application/json" ^
  -d "{\"analysisType\":\"productivity\",\"timeRange\":\"today\"}"

# AI chat
curl -X POST http://localhost:8080/api/ai/chat ^
  -H "Content-Type: application/json" ^
  -d "{\"message\":\"Como melhorar produtividade?\"}"

# Analytics - General stats
curl http://localhost:8080/api/analytics/stats

# Analytics - Today's metrics
curl http://localhost:8080/api/analytics/today

# Analytics - Priority distribution
curl http://localhost:8080/api/analytics/priority
```

### Accessing Frontend

Open `http://localhost:8080` - served from `src/main/resources/static/`

## Critical Implementation Patterns

### Time-Blocking State Machine

Tasks transition through states based on user actions:

```
PENDING (backlog)
    ↓ [drag to calendar]
SCHEDULED (time-blocked)
    ↓ [click complete]
DONE (archived)

SCHEDULED → PENDING [move to backlog]
```

Implemented in `TaskService.moveTask()` and `TaskService.moveBackToBacklog()`.

### Time Slot Conflict Validation

**Critical business rule:** When moving a task to the calendar, the system validates that no other `SCHEDULED` task overlaps the requested time slot.

```java
// In TaskService.moveTask()
validateTimeSlotConflict(id, dto.newStartTime(), dto.newEndTime());

// Overlap detection logic:
boolean hasOverlap = !(newEnd.isBefore(existing.getStartTime()) ||
                       newStart.isAfter(existing.getEndTime()) ||
                       newEnd.isEqual(existing.getStartTime()) ||
                       newStart.isEqual(existing.getEndTime()));
```

**Exception thrown:** `IllegalArgumentException` with message indicating the conflicting task's title and time range.

**Important:** The validation excludes the task being moved (when rescheduling) by filtering out its own ID.### AI Response Parsing

All AI prompts MUST use structured format markers:

```java
// In prompt template:
SUMMARY: [content]
INSIGHTS: [item1]; [item2]; [item3]
RECOMMENDATIONS: [item1]; [item2]; [item3]

// Parsing in ProductivityAnalysisService:
String summary = extractSection(aiResponse, "SUMMARY:");
List<String> insights = extractList(aiResponse, "INSIGHTS:");  // Splits on ";"
```

**Do not modify markers** without updating parsing logic in `ProductivityAnalysisService.parseAIResponse()`.

### Controller Pattern (REST Best Practices)

Controllers are **stateless routers** - all logic in services:

```java
@PostMapping("/{id}/complete")
public ResponseEntity<TaskResponseDTO> completeTask(@PathVariable Long id) {
    TaskResponseDTO completed = taskService.markAsDone(id);  // Service handles logic
    return ResponseEntity.ok(completed);                      // Controller just wraps HTTP
}
```

Never put business logic (calculations, validations, AI calls) in controllers.

## Frontend Architecture (ES6 Modules)

### Module Organization

Frontend uses **vanilla JavaScript ES6 modules** (no frameworks) organized in 3 layers:

```
static/js/
├── app.js                    # Main orchestrator (partially implemented)
├── api/
│   └── taskService.js       # ✅ HTTP client (11 methods, fully implemented)
└── modules/
    ├── dragController.js    # ✅ HTML5 Drag & Drop logic (4 methods)
    ├── calendarView.js      # ✅ Calendar rendering (slot generation)
    └── aiFeedback.js        # ✅ Dashboard + Chart.js integration
```

**Status:** All modules have complete implementations, but `app.js` integration is incomplete - UI doesn't wire up drag-and-drop events to backend API yet.

### Critical Pattern: Native Fetch API Only

All HTTP calls use native `fetch()` - **no jQuery/Axios**:

```javascript
// taskService.js pattern
export const taskService = {
  async moveTask(taskId, newStartTime, newEndTime) {
    const response = await fetch(`${API_BASE}/tasks/${taskId}/move`, {
      method: "PATCH",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ newStartTime, newEndTime }),
    });
    if (!response.ok) {
      const error = await response.text();
      throw new Error(error);
    }
    return response.json();
  },
};
```

### Drag-and-Drop Implementation

Uses HTML5 native Drag & Drop API (NOT libraries):

```javascript
// dragController.js pattern
makeDraggable(taskElement, taskId) {
  taskElement.setAttribute("draggable", "true");
  taskElement.addEventListener("dragstart", (e) => {
    draggedTaskId = taskId;
    e.dataTransfer.effectAllowed = "move";
  });
}

makeDroppable(dropZone, hour) {
  dropZone.addEventListener("drop", async (e) => {
    e.preventDefault();
    const startTime = calculateStartTime(hour);
    await taskService.moveTask(draggedTaskId, startTime, endTime);
  });
}
```

**Integration gap:** `app.js` doesn't call `dragController.makeDraggable()` or `calendarView.render()` on page load yet.

## API Endpoints Reference

### Tasks API (`/api/tasks`)

- `GET /api/tasks` - List all tasks
- `GET /api/tasks/{id}` - Get task by ID
- `POST /api/tasks` - Create task (starts as PENDING)
- `PATCH /api/tasks/{id}/move` - Move to calendar (validates conflicts)
- `PATCH /api/tasks/{id}/complete` - Mark as DONE
- `PATCH /api/tasks/{id}/backlog` - Move back to backlog
- `PUT /api/tasks/{id}` - Update task details
- `DELETE /api/tasks/{id}` - Delete task
- `GET /api/tasks/status/{status}` - Filter by status
- `GET /api/tasks/stats` - Get basic statistics

### AI API (`/api/ai`)

- `POST /api/ai/analyze` - AI productivity analysis
  - Request: `{ "analysisType": "productivity|patterns|recommendations", "timeRange": "today|week|month" }`
  - Response: `{ "summary", "insights[]", "recommendations[]", "score", "timestamp" }`
- `POST /api/ai/chat` - Interactive chat with AI coach
  - Request: `{ "message": "How to improve productivity?" }`
  - Response: `{ "response", "timestamp" }`

### Analytics API (`/api/analytics`)

- `GET /api/analytics/stats` - General statistics (all tasks)
- `GET /api/analytics/today` - Statistics filtered for today
- `GET /api/analytics/priority` - Distribution by priority (HIGH/MEDIUM/LOW)
- `GET /api/analytics/status` - Distribution by status (PENDING/SCHEDULED/DONE/LATE)
- `GET /api/analytics/productivity` - Productivity rate (0-100)
- `GET /api/analytics/overdue` - List of overdue tasks
- `GET /api/analytics/avg-time` - Average completion time (hours)

### Tags API (`/api/tags`)

- `GET /api/tags` - List all tags
- `POST /api/tags` - Create tag
- `GET /api/tags/{id}` - Get tag by ID
- `DELETE /api/tags/{id}` - Delete tag

### Swagger Documentation

- **Swagger UI:** `http://localhost:8080/swagger-ui/index.html` - Interactive API testing
- **OpenAPI JSON:** `http://localhost:8080/v3/api-docs` - Machine-readable spec
- **Configuration:** `OpenApiConfig` with metadata in `config/`

## Common Development Tasks

### Adding New Features

**Creating New Entities:**

1. Create entity in `model/` with manual getters/setters (NO Lombok)
2. Use `@Entity`, `@Table(name = "tb_entityname")`
3. Store enums as `@Enumerated(EnumType.STRING)`, define in `model/enums/`
4. Create repository extending `JpaRepository<Entity, Long>` in `repository/`
5. Create service in `service/` with constructor injection
6. Create DTOs as Java Records in `dto/`
7. Create controller in `controller/` with `/api/{resource}` mapping

**Adding AI Analysis Types:**

Modify `ProductivityAnalysisService.analyzeProductivity()` switch statement and add corresponding prompt method:

```java
private String buildNewTypePrompt(String context) {
    return """
        Prompt template with REQUIRED markers:
        SUMMARY: ...
        INSIGHTS: ...
        RECOMMENDATIONS: ...
        """.formatted(context);
}
```

### Debugging Tips

- **SQL Logging:** Already enabled with `spring.jpa.show-sql=true`
- **AI Request/Response:** LangChain4j logs enabled in `AIClientConfig.logRequestsAndResponses(true)`
- **Application Logs:** Set to DEBUG level for package `com.gustavocirino.myday_productivity`
- **Custom Banner:** ASCII art "GUSTA" displays on startup from `banner.txt`

### Known Limitations

- Frontend JavaScript modules are placeholders - drag-and-drop not implemented in UI yet
- No user authentication - single-user system
- No time zone handling - all times in server local time
- AI responses in Portuguese - prompts hardcoded for Brazilian Portuguese

## Recent Implementations (Nov 2025)

### ✅ Analytics System

Created `AnalyticsService` and `AnalyticsController` with 7 endpoints providing:

- General and time-filtered statistics
- Priority/status distribution
- Productivity rate calculation
- Overdue task detection
- Average completion time tracking

### ✅ Time Slot Conflict Validation

Enhanced `TaskService.moveTask()` with overlap detection:

- Prevents scheduling tasks in occupied time slots
- Validates against all SCHEDULED tasks
- Provides clear error messages with conflicting task details
- Allows rescheduling (excludes own task from conflict check)

### ✅ Push Notifications (Web Push)

Sistema de notificações web via `PushController` (`/api/push`):
- VAPID keys gerados automaticamente ou via `application.properties`
- Endpoints: `/vapid-public-key`, `/subscribe`, `/unsubscribe`
- Serviços: `VapidKeyService`, `WebPushService`, `NotificationScheduler`
- Notificações agendadas para tarefas próximas do horário

## Quick Reference

### Environment Variables Required

```cmd
# Required for AI features
set GEMINI_API_KEY=your_gemini_key

# Optional for web push (auto-generated if not set)
set VAPID_PUBLIC_KEY=...
set VAPID_PRIVATE_KEY=...
```

### Application Entry Points

- **Backend:** `MydayProductivityApplication.java` with `@EnableScheduling`
- **Frontend:** `src/main/resources/static/index.html` → `js/app.js`
- **Server:** Runs on port `8080` (configurable via `--server.port=XXXX`)

### Task Status Lifecycle

```
PENDING → SCHEDULED → DONE
    ↑          ↓
    └──────────┘
```

- `PENDING`: Task in backlog
- `SCHEDULED`: Time-blocked on calendar
- `DONE`: Completed task
- `LATE`: Auto-assigned when end_time is past (not explicitly set by user)
