package com.gustavocirino.myday_productivity.service;

import com.gustavocirino.myday_productivity.dto.SmartRescheduleSuggestionDTO;
import com.gustavocirino.myday_productivity.model.Task;
import com.gustavocirino.myday_productivity.model.User;
import com.gustavocirino.myday_productivity.model.enums.TaskStatus;
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

@Service
@RequiredArgsConstructor
@Slf4j
public class SmartRescheduleService {

    private final TaskRepository taskRepository;

    /**
     * Finds overdue or missed scheduled tasks and suggests new time slots.
     */
    public List<SmartRescheduleSuggestionDTO> getSuggestions(User user) {
        LocalDateTime now = LocalDateTime.now();

        // Find tasks that are SCHEDULED but their endTime has passed (overdue)
        List<Task> overdueTasks = taskRepository.findByUserId(user.getId()).stream()
                .filter(t -> t.getStatus() == TaskStatus.SCHEDULED)
                .filter(t -> t.getEndTime() != null && t.getEndTime().isBefore(now))
                .sorted(Comparator.comparing(Task::getEndTime))
                .limit(10)
                .toList();

        if (overdueTasks.isEmpty()) {
            return List.of();
        }

        // Get today's scheduled tasks to find free slots
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = LocalDate.now().atTime(LocalTime.of(23, 59));
        List<Task> todayScheduled = taskRepository.findByUserId(user.getId()).stream()
                .filter(t -> t.getStatus() == TaskStatus.SCHEDULED)
                .filter(t -> t.getStartTime() != null && t.getStartTime().isAfter(now))
                .filter(t -> t.getStartTime().isBefore(endOfDay))
                .sorted(Comparator.comparing(Task::getStartTime))
                .toList();

        // Build free slot list for today (from now to end of workday 20:00)
        List<TimeSlot> freeSlots = findFreeSlots(todayScheduled, now,
                LocalDate.now().atTime(20, 0));

        // If not enough today, check tomorrow
        if (freeSlots.size() < overdueTasks.size()) {
            LocalDate tomorrow = LocalDate.now().plusDays(1);
            List<Task> tomorrowScheduled = taskRepository.findByUserId(user.getId()).stream()
                    .filter(t -> t.getStatus() == TaskStatus.SCHEDULED)
                    .filter(t -> t.getStartTime() != null
                            && t.getStartTime().toLocalDate().equals(tomorrow))
                    .sorted(Comparator.comparing(Task::getStartTime))
                    .toList();

            freeSlots.addAll(findFreeSlots(tomorrowScheduled,
                    tomorrow.atTime(8, 0), tomorrow.atTime(20, 0)));
        }

        // Match overdue tasks with free slots
        List<SmartRescheduleSuggestionDTO> suggestions = new ArrayList<>();
        Iterator<TimeSlot> slotIterator = freeSlots.iterator();

        for (Task task : overdueTasks) {
            if (!slotIterator.hasNext()) break;

            long durationMinutes = 60; // default 1h
            if (task.getStartTime() != null && task.getEndTime() != null) {
                durationMinutes = java.time.Duration.between(task.getStartTime(), task.getEndTime()).toMinutes();
            }

            // Find a slot that fits
            TimeSlot slot = null;
            while (slotIterator.hasNext()) {
                TimeSlot candidate = slotIterator.next();
                long slotMinutes = java.time.Duration.between(candidate.start, candidate.end).toMinutes();
                if (slotMinutes >= durationMinutes) {
                    slot = candidate;
                    break;
                }
            }

            if (slot != null) {
                LocalDateTime suggestedEnd = slot.start.plusMinutes(durationMinutes);
                String reason = buildReason(task, slot.start);

                suggestions.add(new SmartRescheduleSuggestionDTO(
                        task.getId(),
                        task.getTitle(),
                        task.getStartTime(),
                        task.getEndTime(),
                        slot.start,
                        suggestedEnd,
                        reason
                ));
            }
        }

        log.info("📋 {} sugestões de reagendamento geradas para o usuário {}", suggestions.size(), user.getId());
        return suggestions;
    }

    @Transactional
    public void acceptSuggestion(Long taskId, LocalDateTime newStart, LocalDateTime newEnd, User user) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Tarefa não encontrada"));

        if (!task.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Acesso negado");
        }

        task.setStartTime(newStart);
        task.setEndTime(newEnd);
        task.setStatus(TaskStatus.SCHEDULED);
        taskRepository.save(task);
        log.info("✅ Tarefa {} reagendada: {} → {}", taskId, newStart, newEnd);
    }

    private List<TimeSlot> findFreeSlots(List<Task> scheduledTasks, LocalDateTime dayStart, LocalDateTime dayEnd) {
        List<TimeSlot> freeSlots = new ArrayList<>();
        LocalDateTime cursor = dayStart;

        // Round up to next 30 min
        int minutes = cursor.getMinute();
        if (minutes % 30 != 0) {
            cursor = cursor.plusMinutes(30 - (minutes % 30)).withSecond(0).withNano(0);
        }

        for (Task task : scheduledTasks) {
            if (task.getStartTime() != null && task.getStartTime().isAfter(cursor)) {
                long gap = java.time.Duration.between(cursor, task.getStartTime()).toMinutes();
                if (gap >= 30) {
                    freeSlots.add(new TimeSlot(cursor, task.getStartTime()));
                }
            }
            if (task.getEndTime() != null && task.getEndTime().isAfter(cursor)) {
                cursor = task.getEndTime();
            }
        }

        // Add remaining time until end of day
        if (cursor.isBefore(dayEnd)) {
            long gap = java.time.Duration.between(cursor, dayEnd).toMinutes();
            if (gap >= 30) {
                freeSlots.add(new TimeSlot(cursor, dayEnd));
            }
        }

        return freeSlots;
    }

    private String buildReason(Task task, LocalDateTime suggestedStart) {
        boolean isToday = suggestedStart.toLocalDate().equals(LocalDate.now());
        String when = isToday ? "hoje" : "amanhã";
        return String.format("Tarefa \"%s\" estava atrasada. Slot livre encontrado %s às %s.",
                task.getTitle(), when, suggestedStart.toLocalTime().toString());
    }

    private record TimeSlot(LocalDateTime start, LocalDateTime end) {}
}
