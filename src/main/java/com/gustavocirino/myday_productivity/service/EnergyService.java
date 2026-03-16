package com.gustavocirino.myday_productivity.service;

import com.gustavocirino.myday_productivity.dto.EnergyLogCreateDTO;
import com.gustavocirino.myday_productivity.dto.EnergyLogDTO;
import com.gustavocirino.myday_productivity.dto.EnergyPatternDTO;
import com.gustavocirino.myday_productivity.model.EnergyLog;
import com.gustavocirino.myday_productivity.model.User;
import com.gustavocirino.myday_productivity.model.enums.DayPeriod;
import com.gustavocirino.myday_productivity.model.enums.EnergyLevel;
import com.gustavocirino.myday_productivity.repository.EnergyLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class EnergyService {

    private final EnergyLogRepository energyLogRepository;

    @Transactional
    public EnergyLogDTO logEnergy(EnergyLogCreateDTO dto, User user) {
        LocalDate date = dto.logDate() != null ? dto.logDate() : LocalDate.now();

        // Update existing or create new
        Optional<EnergyLog> existing = energyLogRepository
                .findByUserIdAndLogDateAndPeriod(user.getId(), date, dto.period());

        EnergyLog energyLog;
        if (existing.isPresent()) {
            energyLog = existing.get();
            energyLog.setLevel(dto.level());
            energyLog.setNotes(dto.notes());
        } else {
            energyLog = new EnergyLog();
            energyLog.setUser(user);
            energyLog.setLogDate(date);
            energyLog.setPeriod(dto.period());
            energyLog.setLevel(dto.level());
            energyLog.setNotes(dto.notes());
        }

        EnergyLog saved = energyLogRepository.save(energyLog);
        log.info("⚡ Energia registrada: {} - {} - {}", date, dto.period(), dto.level());
        return toDTO(saved);
    }

    public List<EnergyLogDTO> getTodayLogs(User user) {
        return energyLogRepository.findByUserIdAndLogDateOrderByPeriodAsc(user.getId(), LocalDate.now())
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public List<EnergyLogDTO> getHistory(User user, int days) {
        LocalDate start = LocalDate.now().minusDays(days);
        return energyLogRepository.findByUserIdAndLogDateBetweenOrderByLogDateDescPeriodAsc(user.getId(), start, LocalDate.now())
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public EnergyPatternDTO getPatterns(User user) {
        LocalDate start = LocalDate.now().minusDays(14);
        List<EnergyLog> logs = energyLogRepository.findByUserIdAndLogDateBetweenOrderByLogDateDescPeriodAsc(
                user.getId(), start, LocalDate.now());

        // Calculate average energy by period
        Map<DayPeriod, EnergyLevel> averageByPeriod = new EnumMap<>(DayPeriod.class);
        for (DayPeriod period : DayPeriod.values()) {
            List<EnergyLog> periodLogs = logs.stream()
                    .filter(l -> l.getPeriod() == period)
                    .toList();

            if (!periodLogs.isEmpty()) {
                double avg = periodLogs.stream()
                        .mapToInt(l -> l.getLevel().ordinal())
                        .average()
                        .orElse(2);
                averageByPeriod.put(period, EnergyLevel.values()[(int) Math.round(avg)]);
            }
        }

        // Determine peak energy period
        DayPeriod peakPeriod = averageByPeriod.entrySet().stream()
                .max(Comparator.comparingInt(e -> e.getValue().ordinal()))
                .map(Map.Entry::getKey)
                .orElse(DayPeriod.MORNING);

        String recommendation = buildEnergyRecommendation(averageByPeriod, peakPeriod);

        List<EnergyLogDTO> recentLogs = logs.stream()
                .sorted(Comparator.comparing(EnergyLog::getLogDate).reversed()
                        .thenComparing(EnergyLog::getPeriod))
                .limit(21) // 7 days * 3 periods
                .map(this::toDTO)
                .collect(Collectors.toList());

        return new EnergyPatternDTO(averageByPeriod, peakPeriod, recommendation, recentLogs);
    }

    private String buildEnergyRecommendation(Map<DayPeriod, EnergyLevel> averages, DayPeriod peak) {
        StringBuilder sb = new StringBuilder();
        sb.append("Seu pico de energia é no período da ");

        switch (peak) {
            case MORNING -> sb.append("manhã. Agende tarefas complexas e criativas pela manhã.");
            case AFTERNOON -> sb.append("tarde. Reserve a tarde para trabalho profundo e reuniões importantes.");
            case EVENING -> sb.append("noite. Aproveite a noite para tarefas que exigem mais concentração.");
        }

        EnergyLevel morningLevel = averages.get(DayPeriod.MORNING);
        if (morningLevel != null && morningLevel.ordinal() <= 1) {
            sb.append(" Considere revisar sua rotina matinal — sua energia pela manhã está baixa.");
        }

        return sb.toString();
    }

    private EnergyLogDTO toDTO(EnergyLog log) {
        return new EnergyLogDTO(
                log.getId(),
                log.getLogDate(),
                log.getPeriod(),
                log.getLevel(),
                log.getNotes()
        );
    }
}
