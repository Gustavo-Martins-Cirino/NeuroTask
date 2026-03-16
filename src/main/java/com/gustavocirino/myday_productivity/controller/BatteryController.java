package com.gustavocirino.myday_productivity.controller;

import com.gustavocirino.myday_productivity.model.User;
import com.gustavocirino.myday_productivity.model.UserBattery;
import com.gustavocirino.myday_productivity.repository.UserBatteryRepository;
import com.gustavocirino.myday_productivity.repository.UserRepository;
import com.gustavocirino.myday_productivity.service.ai.BatteryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Expõe a Bateria Social/Foco ao frontend PWA.
 *
 * <p>Autenticação: header {@code X-Auth-Token}, seguindo o padrão de
 * {@code TaskController}. Endpoints sem token retornam 401.</p>
 */
@RestController
@RequestMapping("/api/battery")
@RequiredArgsConstructor
@Slf4j
public class BatteryController {

    private final BatteryService batteryService;
    private final UserBatteryRepository userBatteryRepository;
    private final UserRepository userRepository;

    // ─── POST /api/battery/snapshot ───────────────────────────────────────────

    /**
     * Aciona o cálculo do snapshot da bateria para o usuário autenticado.
     *
     * <p>Útil para: (1) o frontend chamar após criar/agendar tarefas pesadas;
     * (2) o scheduler interno chamar periodicamente sem autenticação de usuário.</p>
     *
     * @return O snapshot persistido com todos os campos calculados (200 OK),
     *         ou 401 se o token for inválido.
     */
    @PostMapping("/snapshot")
    public ResponseEntity<?> triggerSnapshot(
            @RequestHeader(name = "X-Auth-Token", required = true) String authToken) {

        User user = resolveUser(authToken);
        if (user == null) {
            return ResponseEntity.status(401)
                    .body(Map.of("error", "Token de autenticação inválido"));
        }

        UserBattery snapshot = batteryService.calculateAndSaveBatterySnapshot(user);
        log.info("Snapshot acionado via API para usuário {}: risco={}",
                user.getId(), snapshot.getRiskLevel());
        return ResponseEntity.ok(snapshot);
    }

    // ─── GET /api/battery/current ─────────────────────────────────────────────

    /**
     * Retorna o snapshot mais recente da bateria do usuário autenticado.
     *
     * <p>Usado pelo frontend para exibir o indicador de bateria no dashboard em
     * tempo real, sem recalcular. Se nenhum snapshot existir ainda, retorna 204
     * com corpo vazio para que o frontend saiba que deve acionar {@code /snapshot}.</p>
     *
     * @return 200 OK com o snapshot mais recente, 204 se ainda não há dados,
     *         ou 401 se o token for inválido.
     */
    @GetMapping("/current")
    public ResponseEntity<?> getCurrentBattery(
            @RequestHeader(name = "X-Auth-Token", required = true) String authToken) {

        User user = resolveUser(authToken);
        if (user == null) {
            return ResponseEntity.status(401)
                    .body(Map.of("error", "Token de autenticação inválido"));
        }

        return userBatteryRepository.findFirstByUserOrderByRecordedAtDesc(user)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.noContent().build());
    }

    // ─── GET /api/battery/history ─────────────────────────────────────────────

    @GetMapping("/history")
    public ResponseEntity<?> getBatteryHistory(
            @RequestHeader(name = "X-Auth-Token", required = true) String authToken,
            @RequestParam(defaultValue = "10") int limit) {

        User user = resolveUser(authToken);
        if (user == null) {
            return ResponseEntity.status(401)
                    .body(Map.of("error", "Token de autenticação inválido"));
        }

        int safeLimit = Math.min(Math.max(limit, 1), 50);
        var snapshots = userBatteryRepository.findByUserOrderByRecordedAtDesc(user);
        if (snapshots.size() > safeLimit) {
            snapshots = snapshots.subList(0, safeLimit);
        }
        // Reverse to chronological order (oldest first)
        java.util.Collections.reverse(snapshots);
        return ResponseEntity.ok(snapshots);
    }

    // ─── Helper ───────────────────────────────────────────────────────────────

    private User resolveUser(String authToken) {
        return userRepository.findByAuthToken(authToken).orElse(null);
    }
}
