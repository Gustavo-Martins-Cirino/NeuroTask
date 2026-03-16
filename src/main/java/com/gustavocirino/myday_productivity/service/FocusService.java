package com.gustavocirino.myday_productivity.service;

import com.gustavocirino.myday_productivity.dto.FocusSessionDTO;
import com.gustavocirino.myday_productivity.dto.FocusStartRequestDTO;
import com.gustavocirino.myday_productivity.dto.FocusStatsDTO;
import com.gustavocirino.myday_productivity.model.FocusSession;
import com.gustavocirino.myday_productivity.model.Task;
import com.gustavocirino.myday_productivity.model.User;
import com.gustavocirino.myday_productivity.model.enums.FocusSessionStatus;
import com.gustavocirino.myday_productivity.repository.FocusSessionRepository;
import com.gustavocirino.myday_productivity.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class FocusService {

    private final FocusSessionRepository focusSessionRepository;
    private final TaskRepository taskRepository;

    @Transactional
    public FocusSessionDTO startSession(FocusStartRequestDTO request, User user) {
        // Cancel any running sessions for this user
        focusSessionRepository.findFirstByUserIdAndStatusOrderByStartedAtDesc(user.getId(), FocusSessionStatus.RUNNING)
                .ifPresent(existing -> {
                    existing.setStatus(FocusSessionStatus.CANCELLED);
                    focusSessionRepository.save(existing);
                });

        FocusSession session = new FocusSession();
        session.setUser(user);
        session.setPlannedMinutes(request.plannedMinutes() != null ? request.plannedMinutes() : 25);
        session.setBreakMinutes(request.breakMinutes() != null ? request.breakMinutes() : 5);
        session.setTotalCycles(request.totalCycles() != null ? request.totalCycles() : 4);
        session.setCycleNumber(1);
        session.setActualSeconds(0L);
        session.setStatus(FocusSessionStatus.RUNNING);
        session.setStartedAt(LocalDateTime.now());

        if (request.taskId() != null) {
            Task task = taskRepository.findById(request.taskId())
                    .orElseThrow(() -> new RuntimeException("Tarefa não encontrada"));
            session.setTask(task);
        }

        FocusSession saved = focusSessionRepository.save(session);
        log.info("🎯 Sessão de foco iniciada: {} min, ciclo 1/{}", saved.getPlannedMinutes(), saved.getTotalCycles());
        return toDTO(saved);
    }

    @Transactional
    public FocusSessionDTO pauseSession(Long sessionId, User user) {
        FocusSession session = getOwnedSession(sessionId, user);

        if (session.getStatus() != FocusSessionStatus.RUNNING) {
            throw new IllegalStateException("Sessão não está em andamento");
        }

        // Accumulate elapsed seconds
        if (session.getStartedAt() != null) {
            long elapsed = ChronoUnit.SECONDS.between(session.getStartedAt(), LocalDateTime.now());
            session.setActualSeconds(session.getActualSeconds() + elapsed);
        }

        session.setStatus(FocusSessionStatus.PAUSED);
        session.setPausedAt(LocalDateTime.now());

        FocusSession saved = focusSessionRepository.save(session);
        log.info("⏸️ Sessão de foco pausada: {}", sessionId);
        return toDTO(saved);
    }

    @Transactional
    public FocusSessionDTO resumeSession(Long sessionId, User user) {
        FocusSession session = getOwnedSession(sessionId, user);

        if (session.getStatus() != FocusSessionStatus.PAUSED) {
            throw new IllegalStateException("Sessão não está pausada");
        }

        session.setStatus(FocusSessionStatus.RUNNING);
        session.setStartedAt(LocalDateTime.now()); // Reset start for elapsed calculation
        session.setPausedAt(null);

        FocusSession saved = focusSessionRepository.save(session);
        log.info("▶️ Sessão de foco retomada: {}", sessionId);
        return toDTO(saved);
    }

    @Transactional
    public FocusSessionDTO completeSession(Long sessionId, User user) {
        FocusSession session = getOwnedSession(sessionId, user);

        if (session.getStatus() == FocusSessionStatus.COMPLETED) {
            throw new IllegalStateException("Sessão já foi concluída");
        }

        // Calculate final elapsed time if still running
        if (session.getStatus() == FocusSessionStatus.RUNNING && session.getStartedAt() != null) {
            long elapsed = ChronoUnit.SECONDS.between(session.getStartedAt(), LocalDateTime.now());
            session.setActualSeconds(session.getActualSeconds() + elapsed);
        }

        session.setStatus(FocusSessionStatus.COMPLETED);
        session.setCompletedAt(LocalDateTime.now());

        FocusSession saved = focusSessionRepository.save(session);
        log.info("✅ Sessão de foco concluída: {} ({} segundos)", sessionId, saved.getActualSeconds());
        return toDTO(saved);
    }

    @Transactional
    public FocusSessionDTO nextCycle(Long sessionId, User user) {
        FocusSession session = getOwnedSession(sessionId, user);

        if (session.getCycleNumber() >= session.getTotalCycles()) {
            return completeSession(sessionId, user);
        }

        // Accumulate time from current cycle
        if (session.getStatus() == FocusSessionStatus.RUNNING && session.getStartedAt() != null) {
            long elapsed = ChronoUnit.SECONDS.between(session.getStartedAt(), LocalDateTime.now());
            session.setActualSeconds(session.getActualSeconds() + elapsed);
        }

        session.setCycleNumber(session.getCycleNumber() + 1);
        session.setStartedAt(LocalDateTime.now());
        session.setStatus(FocusSessionStatus.RUNNING);
        session.setPausedAt(null);

        FocusSession saved = focusSessionRepository.save(session);
        log.info("🔄 Próximo ciclo: {}/{}", saved.getCycleNumber(), saved.getTotalCycles());
        return toDTO(saved);
    }

    @Transactional
    public FocusSessionDTO cancelSession(Long sessionId, User user) {
        FocusSession session = getOwnedSession(sessionId, user);
        session.setStatus(FocusSessionStatus.CANCELLED);
        session.setCompletedAt(LocalDateTime.now());

        FocusSession saved = focusSessionRepository.save(session);
        log.info("❌ Sessão de foco cancelada: {}", sessionId);
        return toDTO(saved);
    }

    public FocusSessionDTO getActiveSession(User user) {
        return focusSessionRepository.findFirstByUserIdAndStatusOrderByStartedAtDesc(user.getId(), FocusSessionStatus.RUNNING)
                .or(() -> focusSessionRepository.findFirstByUserIdAndStatusOrderByStartedAtDesc(user.getId(), FocusSessionStatus.PAUSED))
                .map(this::toDTO)
                .orElse(null);
    }

    public FocusStatsDTO getStats(User user) {
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = LocalDate.now().atTime(LocalTime.MAX);

        List<FocusSession> allSessions = focusSessionRepository.findByUserIdAndStatusOrderByStartedAtDesc(user.getId(), FocusSessionStatus.COMPLETED);
        List<FocusSession> todaySessions = focusSessionRepository.findByUserIdAndStatusAndStartedAtBetween(
                user.getId(), FocusSessionStatus.COMPLETED, startOfDay, endOfDay);

        long totalFocusMinutes = allSessions.stream()
                .mapToLong(s -> s.getActualSeconds() / 60)
                .sum();

        long todayFocusMinutes = todaySessions.stream()
                .mapToLong(s -> s.getActualSeconds() / 60)
                .sum();

        double avgMinutes = allSessions.isEmpty() ? 0 :
                allSessions.stream().mapToLong(s -> s.getActualSeconds() / 60).average().orElse(0);

        List<FocusSessionDTO> recent = focusSessionRepository
                .findByUserIdAndStartedAtBetweenOrderByStartedAtDesc(user.getId(),
                        LocalDateTime.now().minusDays(7), LocalDateTime.now())
                .stream()
                .sorted((a, b) -> b.getStartedAt().compareTo(a.getStartedAt()))
                .limit(10)
                .map(this::toDTO)
                .collect(Collectors.toList());

        return new FocusStatsDTO(
                (long) allSessions.size(),
                (long) allSessions.size(),
                totalFocusMinutes,
                (long) avgMinutes,
                (long) todaySessions.size(),
                todayFocusMinutes,
                recent
        );
    }

    private FocusSession getOwnedSession(Long sessionId, User user) {
        FocusSession session = focusSessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Sessão não encontrada"));
        if (!session.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Acesso negado à sessão");
        }
        return session;
    }

    private FocusSessionDTO toDTO(FocusSession s) {
        return new FocusSessionDTO(
                s.getId(),
                s.getTask() != null ? s.getTask().getId() : null,
                s.getTask() != null ? s.getTask().getTitle() : null,
                s.getStatus(),
                s.getPlannedMinutes(),
                s.getActualSeconds(),
                s.getBreakMinutes(),
                s.getCycleNumber(),
                s.getTotalCycles(),
                s.getStartedAt(),
                s.getPausedAt(),
                s.getCompletedAt()
        );
    }
}
