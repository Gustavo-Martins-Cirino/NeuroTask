package com.gustavocirino.myday_productivity.dto;

import com.gustavocirino.myday_productivity.model.enums.DayPeriod;
import com.gustavocirino.myday_productivity.model.enums.EnergyLevel;
import java.util.List;
import java.util.Map;

public record EnergyPatternDTO(
    Map<DayPeriod, EnergyLevel> averageByPeriod,
    DayPeriod peakEnergyPeriod,
    String aiRecommendation,
    List<EnergyLogDTO> recentLogs
) {}
