package com.gustavocirino.myday_productivity.dto;

import com.gustavocirino.myday_productivity.model.enums.DayPeriod;
import com.gustavocirino.myday_productivity.model.enums.EnergyLevel;
import java.time.LocalDate;

public record EnergyLogCreateDTO(
    LocalDate logDate,
    DayPeriod period,
    EnergyLevel level,
    String notes
) {}
