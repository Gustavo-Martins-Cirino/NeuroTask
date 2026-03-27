package com.gustavocirino.myday_productivity.controller;

import com.gustavocirino.myday_productivity.model.ProcrastinationRecord;
import com.gustavocirino.myday_productivity.model.User;
import com.gustavocirino.myday_productivity.repository.ProcrastinationRecordRepository;
import com.gustavocirino.myday_productivity.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Expõe o módulo de Rastreamento de Procrastinação ao frontend PWA.
 *
 * <p>Autenticação: header {@code X-Auth-Token}, seguindo o padrão do projeto.</p>
 */
@RestController
@RequestMapping("/api/procrastination")
@RequiredArgsConstructor
@Slf4j
public class ProcrastinationController {

    private final ProcrastinationRecordRepository procrastinationRecordRepository;
    private final UserRepository userRepository;

    // ─── GET /api/procrastination/anomalies ───────────────────────────────────

    /**
     * Lista todos os registros de procrastinação anômalos do usuário autenticado.
     *
     * <p>Retorna apenas registros onde {@code isAnomaly = true}, incluindo a
     * sugestão de decomposição gerada pela IA ({@code aiSuggestion}) quando
     * disponível. O frontend usa esses dados para exibir o painel de alertas
     * "Tarefas que você está evitando".</p>
     *
     * @return 200 OK com a lista (pode ser vazia), ou 401 se o token for inválido.
     */
    @GetMapping("/anomalies")
    public ResponseEntity<?> getAnomalies(
            @RequestHeader(name = "X-Auth-Token", required = true) String authToken) {

        User user = resolveUser(authToken);
        if (user == null) {
            return ResponseEntity.status(401)
                    .body(Map.of("error", "Token de autenticação inválido"));
        }

        List<ProcrastinationRecord> anomalies =
                procrastinationRecordRepository.findByUserAndIsAnomalyTrue(user);

        log.debug("Anomalias de procrastinação consultadas: usuário={}, total={}",
                user.getId(), anomalies.size());

        return ResponseEntity.ok(anomalies);
    }

    // ─── GET /api/procrastination/all ─────────────────────────────────────────

    /**
     * Lista todos os registros de adiamento do usuário (incluindo os não anômalos).
     *
     * <p>Útil para o gráfico de histórico de adiamentos por tarefa e para o
     * cálculo de tendências no frontend.</p>
     *
     * @return 200 OK com a lista completa de registros.
     */
    @GetMapping("/all")
    public ResponseEntity<?> getAllRecords(
            @RequestHeader(name = "X-Auth-Token", required = true) String authToken) {

        User user = resolveUser(authToken);
        if (user == null) {
            return ResponseEntity.status(401)
                    .body(Map.of("error", "Token de autenticação inválido"));
        }

        List<ProcrastinationRecord> records =
                procrastinationRecordRepository.findByUser(user);

        return ResponseEntity.ok(records);
    }

    // ─── GET /api/procrastination/stats ───────────────────────────────────────

    /**
     * Retorna métricas resumidas do módulo de procrastinação para o dashboard.
     *
     * <p>Campos: {@code totalPostponements} (total de adiamentos), {@code anomalyCount}
     * (anomalias detectadas), {@code pendingSuggestions} (anomalias sem sugestão gerada).
     * Permite que o frontend exiba KPIs sem baixar todos os registros.</p>
     */
    @GetMapping("/stats")
    public ResponseEntity<?> getStats(
            @RequestHeader(name = "X-Auth-Token", required = true) String authToken) {

        User user = resolveUser(authToken);
        if (user == null) {
            return ResponseEntity.status(401)
                    .body(Map.of("error", "Token de autenticação inválido"));
        }

        List<ProcrastinationRecord> all = procrastinationRecordRepository.findByUser(user);
        long anomalyCount = procrastinationRecordRepository.countAnomaliesByUser(user);
        long pendingSuggestions = procrastinationRecordRepository
                .findByUserAndIsAnomalyTrueAndSuggestionGeneratedFalse(user).size();

        Map<String, Object> stats = Map.of(
                "totalPostponements", all.stream().mapToInt(ProcrastinationRecord::getPostponeCount).sum(),
                "tasksWithPostponements", all.size(),
                "anomalyCount", anomalyCount,
                "pendingSuggestions", pendingSuggestions
        );

        return ResponseEntity.ok(stats);
    }

    // ─── Helper ───────────────────────────────────────────────────────────────

    private User resolveUser(String authToken) {
        Object principal = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof User) {
            return (User) principal;
        }
        return null;
    }
}
