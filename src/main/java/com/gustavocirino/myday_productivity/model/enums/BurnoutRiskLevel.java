package com.gustavocirino.myday_productivity.model.enums;

/**
 * Nível de risco de burnout calculado pela análise da Bateria Social/Foco.
 * Derivado do burnoutRiskScore (0.0–1.0) armazenado em UserBattery.
 */
public enum BurnoutRiskLevel {

    /** Score 0.00–0.24: carga saudável, bateria em boas condições. */
    LOW,

    /** Score 0.25–0.49: sinais de fadiga acumulada, atenção recomendada. */
    MODERATE,

    /** Score 0.50–0.74: risco real de esgotamento, pausas obrigatórias sugeridas. */
    HIGH,

    /** Score 0.75–1.00: esgotamento iminente, redistribuição de tarefas necessária. */
    CRITICAL
}
