package com.gustavocirino.myday_productivity.service.ai;

import com.gustavocirino.myday_productivity.model.Task;
import com.gustavocirino.myday_productivity.model.TaskContextGroup;
import com.gustavocirino.myday_productivity.model.User;
import com.gustavocirino.myday_productivity.model.enums.ContextType;
import com.gustavocirino.myday_productivity.model.enums.TaskStatus;
import com.gustavocirino.myday_productivity.repository.TaskContextGroupRepository;
import com.gustavocirino.myday_productivity.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Batching de tarefas — módulo inspirado no Clustering K-Means do SpinOps.
 *
 * <p>O método principal {@link #generateFocusBlocks(User)} agrupa as tarefas ativas
 * do usuário pelo seu {@code contextType}, cria ou atualiza um {@link TaskContextGroup}
 * por contexto e calcula a duração sugerida de cada bloco de foco com 10% de margem
 * de segurança. O objetivo é eliminar o custo cognitivo de troca de contexto e manter
 * o usuário em estado de "flow".</p>
 *
 * <p>Tarefas sem {@code contextType} definido são agrupadas em contexto
 * {@link ContextType#ADMINISTRATIVE} como fallback.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BatchingService {

    /** Margem de segurança adicionada à soma das durações estimadas (10%). */
    private static final double SAFETY_MARGIN = 1.10;

    /**
     * Duração padrão assumida por tarefa quando {@code estimatedDurationMinutes} é nulo.
     * Baseado na Técnica Pomodoro (1 sessão = 25 min).
     */
    private static final int DEFAULT_TASK_DURATION_MINUTES = 25;

    /** Tempo máximo considerado para que um grupo "stale" seja recalculado (1 hora). */
    private static final int STALE_THRESHOLD_HOURS = 1;

    private final TaskRepository taskRepository;
    private final TaskContextGroupRepository taskContextGroupRepository;

    // ─── Método principal ─────────────────────────────────────────────────────

    /**
     * Gera ou atualiza os blocos de foco para o usuário com base nas tarefas do dia.
     *
     * <p>Fluxo:
     * <ol>
     *   <li>Busca tarefas PENDING e SCHEDULED do usuário para hoje.</li>
     *   <li>Agrupa pelo {@code contextType} (fallback: ADMINISTRATIVE).</li>
     *   <li>Para cada grupo, busca ou cria o {@link TaskContextGroup} persistente.</li>
     *   <li>Calcula {@code suggestedFocusDurationMinutes} = Σ duração × 1.10.</li>
     *   <li>Atualiza a associação {@code tasks} do grupo no banco.</li>
     *   <li>Persiste e retorna a lista de grupos gerados.</li>
     * </ol>
     * </p>
     *
     * @param user Usuário para o qual os blocos devem ser gerados.
     * @return Lista de {@link TaskContextGroup} persistidos, ordenados por
     *         duração sugerida decrescente (blocos maiores primeiro).
     */
    @Transactional
    public List<TaskContextGroup> generateFocusBlocks(User user) {
        log.debug("Gerando blocos de foco para usuário {}", user.getId());

        // 1. Tarefas ativas do dia (PENDING + SCHEDULED)
        List<Task> activeTasks = fetchActiveTasks(user);

        if (activeTasks.isEmpty()) {
            log.info("Nenhuma tarefa ativa hoje para o usuário {}. Nenhum bloco gerado.", user.getId());
            return Collections.emptyList();
        }

        // 2. Agrupamento por contextType
        Map<ContextType, List<Task>> tasksByContext = groupByContext(activeTasks);

        // 3. Criação/atualização de grupos e cálculo de duração
        List<TaskContextGroup> results = new ArrayList<>();

        for (Map.Entry<ContextType, List<Task>> entry : tasksByContext.entrySet()) {
            ContextType contextType = entry.getKey();
            List<Task> groupTasks   = entry.getValue();

            TaskContextGroup group = resolveGroup(user, contextType);
            group.setTasks(groupTasks);
            group.setSuggestedFocusDurationMinutes(calculateFocusDuration(groupTasks));
            group.setName(buildGroupName(contextType, groupTasks.size()));
            group.setDescription(buildGroupDescription(contextType, groupTasks));
            group.setColorHex(defaultColorForContext(contextType));
            group.setLastOptimizedAt(LocalDateTime.now());

            results.add(taskContextGroupRepository.save(group));
            log.info("Bloco '{}' gerado: {} tarefas, {} min", group.getName(),
                    groupTasks.size(), group.getSuggestedFocusDurationMinutes());
        }

        // Ordena blocos por duração decrescente (mais densos primeiro)
        results.sort(Comparator.comparingInt(TaskContextGroup::getSuggestedFocusDurationMinutes).reversed());

        return results;
    }

    // ─── Busca de tarefas ─────────────────────────────────────────────────────

    private List<Task> fetchActiveTasks(User user) {
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay   = LocalDate.now().atTime(LocalTime.MAX);

        // SCHEDULED com horário em hoje
        List<Task> scheduled = taskRepository
                .findByStatusAndStartTimeBetween(TaskStatus.SCHEDULED, startOfDay, endOfDay)
                .stream()
                .filter(t -> user.getId().equals(t.getUser() != null ? t.getUser().getId() : null))
                .collect(Collectors.toList());

        // PENDING do usuário (sem horário definido ainda, mas precisam ser agendadas)
        List<Task> pending = taskRepository.findByUserId(user.getId())
                .stream()
                .filter(t -> t.getStatus() == TaskStatus.PENDING)
                .collect(Collectors.toList());

        List<Task> all = new ArrayList<>(scheduled);
        all.addAll(pending);
        return all;
    }

    // ─── Agrupamento ──────────────────────────────────────────────────────────

    /**
     * Agrupa as tarefas pelo {@code contextType}. Tarefas sem contexto definido
     * são atribuídas ao contexto {@link ContextType#ADMINISTRATIVE} como fallback.
     */
    private Map<ContextType, List<Task>> groupByContext(List<Task> tasks) {
        return tasks.stream()
                .collect(Collectors.groupingBy(
                        t -> t.getContextType() != null
                                ? t.getContextType()
                                : ContextType.ADMINISTRATIVE
                ));
    }

    // ─── Persistência do grupo ────────────────────────────────────────────────

    /**
     * Busca o grupo ativo mais recente para o par (usuário, contextType).
     * Se não existir, cria um novo. Grupos desatualizados há mais de
     * {@link #STALE_THRESHOLD_HOURS} hora são reutilizados e recalculados.
     */
    private TaskContextGroup resolveGroup(User user, ContextType contextType) {
        return taskContextGroupRepository
                .findFirstByUserAndContextTypeOrderByCreatedAtDesc(user, contextType)
                .orElseGet(() -> {
                    TaskContextGroup newGroup = new TaskContextGroup();
                    newGroup.setUser(user);
                    newGroup.setContextType(contextType);
                    return newGroup;
                });
    }

    // ─── Cálculo de duração ───────────────────────────────────────────────────

    /**
     * Soma as durações estimadas das tarefas do grupo e aplica 10% de margem.
     * Tarefas sem duração estimada assumem {@link #DEFAULT_TASK_DURATION_MINUTES}.
     *
     * @return Duração arredondada em minutos (inteiro).
     */
    private int calculateFocusDuration(List<Task> tasks) {
        int totalMinutes = tasks.stream()
                .mapToInt(t -> t.getEstimatedDurationMinutes() != null
                        ? t.getEstimatedDurationMinutes()
                        : DEFAULT_TASK_DURATION_MINUTES)
                .sum();

        return (int) Math.round(totalMinutes * SAFETY_MARGIN);
    }

    // ─── Labels e cores padrão ────────────────────────────────────────────────

    private String buildGroupName(ContextType type, int taskCount) {
        String label = switch (type) {
            case CALLS          -> "Bloco de Ligações";
            case EMAILS         -> "Bloco de E-mails";
            case MEETINGS       -> "Bloco de Reuniões";
            case READING        -> "Bloco de Leitura";
            case WRITING        -> "Bloco de Escrita";
            case ADMINISTRATIVE -> "Bloco Administrativo";
            case CREATIVE       -> "Bloco Criativo";
            case PHYSICAL       -> "Bloco de Atividades Físicas";
            case LEARNING       -> "Bloco de Aprendizado";
        };
        return label + " (" + taskCount + " tarefa" + (taskCount > 1 ? "s" : "") + ")";
    }

    private String buildGroupDescription(ContextType type, List<Task> tasks) {
        String taskList = tasks.stream()
                .limit(3)
                .map(Task::getTitle)
                .collect(Collectors.joining(", "));

        String suffix = tasks.size() > 3 ? " e mais " + (tasks.size() - 3) + " tarefa(s)." : ".";
        return "Agrupe sua atenção em: " + taskList + suffix;
    }

    /**
     * Cor HEX padrão por tipo de contexto para identificação visual no calendário.
     */
    private String defaultColorForContext(ContextType type) {
        return switch (type) {
            case CALLS          -> "#4A90E2"; // azul
            case EMAILS         -> "#7B68EE"; // roxo
            case MEETINGS       -> "#E67E22"; // laranja
            case READING        -> "#27AE60"; // verde
            case WRITING        -> "#2ECC71"; // verde-claro
            case ADMINISTRATIVE -> "#95A5A6"; // cinza
            case CREATIVE       -> "#E91E63"; // rosa
            case PHYSICAL       -> "#F39C12"; // amarelo
            case LEARNING       -> "#9B59B6"; // violeta
        };
    }
}
