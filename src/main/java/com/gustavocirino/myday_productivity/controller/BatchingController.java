package com.gustavocirino.myday_productivity.controller;

import com.gustavocirino.myday_productivity.model.TaskContextGroup;
import com.gustavocirino.myday_productivity.model.User;
import com.gustavocirino.myday_productivity.repository.TaskContextGroupRepository;
import com.gustavocirino.myday_productivity.repository.UserRepository;
import com.gustavocirino.myday_productivity.service.ai.BatchingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Expõe o módulo de Agrupamento por Contexto (Batching) ao frontend PWA.
 *
 * <p>Autenticação: header {@code X-Auth-Token}, seguindo o padrão do projeto.</p>
 */
@RestController
@RequestMapping("/api/batching")
@RequiredArgsConstructor
@Slf4j
public class BatchingController {

    private final BatchingService batchingService;
    private final TaskContextGroupRepository taskContextGroupRepository;
    private final UserRepository userRepository;

    // ─── POST /api/batching/generate ──────────────────────────────────────────

    /**
     * Aciona o algoritmo de clustering e gera/atualiza os blocos de foco do dia.
     *
     * <p>O frontend deve chamar este endpoint: (1) ao abrir o modo de planejamento
     * diário; (2) após criar ou atualizar tarefas com novo {@code contextType}.</p>
     *
     * @return 200 OK com a lista de {@link TaskContextGroup} gerados, ordenados por
     *         duração decrescente. Lista vazia ({@code []}) quando não há tarefas ativas.
     *         401 se o token for inválido.
     */
    @PostMapping("/generate")
    public ResponseEntity<?> generateBlocks(
            @RequestHeader(name = "X-Auth-Token", required = true) String authToken) {

        User user = resolveUser(authToken);
        if (user == null) {
            return ResponseEntity.status(401)
                    .body(Map.of("error", "Token de autenticação inválido"));
        }

        List<TaskContextGroup> blocks = batchingService.generateFocusBlocks(user);
        log.info("Blocos de foco gerados via API: usuário={}, total={}", user.getId(), blocks.size());
        return ResponseEntity.ok(blocks);
    }

    // ─── GET /api/batching/blocks ─────────────────────────────────────────────

    /**
     * Retorna os blocos de foco ativos do usuário sem recalcular.
     *
     * <p>Usado pelo frontend para renderizar o painel "Meu Dia em Blocos" sem
     * disparar o algoritmo a cada reload. O frontend deve chamar {@code /generate}
     * quando quiser forçar uma atualização.</p>
     *
     * @return 200 OK com todos os grupos de contexto do usuário. Lista vazia se
     *         nenhum bloco foi gerado ainda. 401 se o token for inválido.
     */
    @GetMapping("/blocks")
    public ResponseEntity<?> getBlocks(
            @RequestHeader(name = "X-Auth-Token", required = true) String authToken) {

        User user = resolveUser(authToken);
        if (user == null) {
            return ResponseEntity.status(401)
                    .body(Map.of("error", "Token de autenticação inválido"));
        }

        List<TaskContextGroup> blocks = taskContextGroupRepository.findByUser(user);
        return ResponseEntity.ok(blocks);
    }

    // ─── Helper ───────────────────────────────────────────────────────────────

    private User resolveUser(String authToken) {
        return userRepository.findByAuthToken(authToken).orElse(null);
    }
}
