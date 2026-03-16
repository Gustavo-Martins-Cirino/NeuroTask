/**
 * aiDashboard.js — Renderização dos cards de IA no Dashboard NeuroTask
 *
 * Orquestra os três cards do painel de IA:
 *   1. Bateria Cognitiva   (#ntBatteryContent)
 *   2. Blocos de Foco      (#ntFocusContent)
 *   3. Radar de Procrastinação (#ntProcrastContent)
 *
 * Registra-se em `window.loadAIDashboard` para que o script inline do
 * index.html possa invocá-la sem complexidade de importação dinâmica.
 *
 * Uso (inline script do index.html):
 *   window.loadAIDashboard?.();
 *   window.loadAIDashboard?.({ section: 'battery' });
 */

import { neurotaskApi } from "../api/neurotask-api.js";

// ─── Helpers de renderização ──────────────────────────────────────────────────

/** Formata minutos como texto legível. */
function fmtMinutes(minutes) {
  if (!minutes || minutes <= 0) return "—";
  if (minutes < 60) return `${minutes} min`;
  const h = Math.floor(minutes / 60);
  const m = minutes % 60;
  return m > 0 ? `${h}h ${m}min` : `${h}h`;
}

/** Mapa de labels em pt-BR para os níveis de risco. */
const RISK_LABELS = {
  LOW: "Baixo",
  MODERATE: "Moderado",
  HIGH: "Alto",
  CRITICAL: "Crítico",
};

/** Mapa de classe CSS para os níveis de risco. */
const RISK_CSS = {
  LOW: "risk-low",
  MODERATE: "risk-moderate",
  HIGH: "risk-high",
  CRITICAL: "risk-critical",
};

/** Cores de fallback por ContextType, para quando o backend não retornar colorHex. */
const CONTEXT_COLORS = {
  CALLS: "#60a5fa",
  EMAILS: "#34d399",
  MEETINGS: "#a78bfa",
  READING: "#fbbf24",
  WRITING: "#f472b6",
  ADMINISTRATIVE: "#94a3b8",
  CREATIVE: "#fb923c",
  PHYSICAL: "#4ade80",
  LEARNING: "#38bdf8",
};

/** Labels em pt-BR para os tipos de contexto. */
const CONTEXT_LABELS = {
  CALLS: "Ligações",
  EMAILS: "E-mails",
  MEETINGS: "Reuniões",
  READING: "Leitura",
  WRITING: "Escrita",
  ADMINISTRATIVE: "Adm.",
  CREATIVE: "Criativo",
  PHYSICAL: "Físico",
  LEARNING: "Aprendizado",
};

// ─── Renderers individuais ────────────────────────────────────────────────────

/**
 * Renderiza o Card da Bateria Cognitiva.
 * @param {Object|null} snapshot - Dados do snapshot ou null (offline/sem dados).
 */
function renderBattery(snapshot) {
  const el = document.getElementById("ntBatteryContent");
  if (!el) return;

  if (!snapshot) {
    el.innerHTML = `
      <div class="nt-empty-state">
        <div class="nt-empty-state-icon">🔋</div>
        <div class="nt-empty-state-text">
          Nenhum dado disponível ainda.<br>
          Clique em <strong>↺</strong> para calcular o snapshot atual.
        </div>
      </div>`;
    return;
  }

  const { batteryLevel = 0, cognitiveLoad = 0, riskLevel = "LOW",
    suggestedBreakMinutes = 0, aiNotes, totalTaskWeight = 0 } = snapshot;

  // Cor da barra varia de verde (alto) para vermelho (baixo)
  const barColor = batteryLevel >= 70
    ? "#22c55e"
    : batteryLevel >= 40
      ? "#eab308"
      : "#ef4444";

  const riskLabel = RISK_LABELS[riskLevel] ?? riskLevel;
  const riskClass = RISK_CSS[riskLevel] ?? "risk-low";

  const notesHtml = aiNotes
    ? `<div class="nt-battery-ai-notes" title="Mensagem da IA">&ldquo;${escapeHtml(aiNotes)}&rdquo;</div>`
    : "";

  el.innerHTML = `
    <div style="display: flex; align-items: baseline; gap: 6px; margin-bottom: 6px;">
      <span class="nt-battery-level-value">${batteryLevel}%</span>
      <span class="nt-battery-level-label">nível de bateria</span>
    </div>
    <div class="nt-battery-bar-track">
      <div class="nt-battery-bar-fill"
           style="width: ${batteryLevel}%; background-color: ${barColor};"
           aria-valuenow="${batteryLevel}" aria-valuemin="0" aria-valuemax="100"
           role="progressbar"></div>
    </div>
    <span class="nt-risk-badge ${riskClass}">Risco ${riskLabel}</span>
    ${notesHtml}
    <div class="nt-battery-meta">
      <div class="nt-battery-meta-item">
        <div class="nt-battery-meta-value">${Math.round(cognitiveLoad)}%</div>
        <div class="nt-battery-meta-label">Carga cognitiva</div>
      </div>
      <div class="nt-battery-meta-item">
        <div class="nt-battery-meta-value">${totalTaskWeight}</div>
        <div class="nt-battery-meta-label">Peso total</div>
      </div>
      <div class="nt-battery-meta-item">
        <div class="nt-battery-meta-value">${(riskLevel === 'LOW' && !suggestedBreakMinutes) ? '0 min' : (suggestedBreakMinutes || "—")}</div>
        <div class="nt-battery-meta-label">Pausa sugerida (min)</div>
      </div>
    </div>`;
}

/**
 * Renderiza o Card de Blocos de Foco.
 * @param {Array} blocks - Lista de TaskContextGroup ou [] (offline/sem dados).
 */
function renderFocusBlocks(blocks) {
  const el = document.getElementById("ntFocusContent");
  if (!el) return;

  if (!blocks || blocks.length === 0) {
    el.innerHTML = `
      <div class="nt-empty-state">
        <div class="nt-empty-state-icon">⭐</div>
        <div class="nt-empty-state-text">
          Nenhum bloco de foco gerado ainda.<br>
          Clique em <strong>↺</strong> para agrupar suas tarefas por contexto.
        </div>
      </div>`;
    return;
  }

  const items = blocks
    .slice(0, 6) // exibe no máximo 6 para não sobrecarregar o card
    .map((block) => {
      const color = block.colorHex
        || CONTEXT_COLORS[block.contextType]
        || "#9ca3af";
      const label = CONTEXT_LABELS[block.contextType] ?? block.contextType ?? "Geral";
      const taskCount = block.tasks?.length ?? 0;
      const duration = fmtMinutes(block.suggestedFocusDurationMinutes);
      const name = escapeHtml(block.name || label);

      return `
        <div class="nt-focus-item">
          <span class="nt-focus-color-dot" style="background: ${color};"></span>
          <div class="nt-focus-info">
            <div class="nt-focus-name">${name}</div>
            <div class="nt-focus-sub">${taskCount} tarefa${taskCount !== 1 ? "s" : ""} · ${label}</div>
          </div>
          <span class="nt-focus-duration">${duration}</span>
        </div>`;
    })
    .join("");

  const totalMin = blocks.reduce(
    (acc, b) => acc + (b.suggestedFocusDurationMinutes || 0), 0
  );

  el.innerHTML = `
    <div class="nt-focus-list">${items}</div>
    ${blocks.length > 6
      ? `<p style="font-size:11px; color:var(--text-muted); margin-top:8px; text-align:right;">+${blocks.length - 6} bloco(s) adicionais</p>`
      : ""}
    <p style="font-size:12px; color:var(--text-secondary); margin-top:12px; text-align:right;">
      Foco total estimado: <strong>${fmtMinutes(totalMin)}</strong>
    </p>`;
}

/**
 * Renderiza o Card do Radar de Procrastinação.
 * @param {Array} anomalies - Registros com isAnomaly=true ou [] (offline/sem dados).
 */
function renderProcrastination(anomalies) {
  const el = document.getElementById("ntProcrastContent");
  const badge = document.getElementById("ntProcrastBadge");
  if (!el) return;

  // Atualiza badge de contagem
  if (badge) {
    if (anomalies && anomalies.length > 0) {
      badge.textContent = `${anomalies.length} alerta${anomalies.length !== 1 ? "s" : ""}`;
      badge.style.display = "inline";
    } else {
      badge.style.display = "none";
    }
  }

  if (!anomalies || anomalies.length === 0) {
    el.innerHTML = `
      <div class="nt-empty-state">
        <div class="nt-empty-state-icon">✅</div>
        <div class="nt-empty-state-text">
          Nenhuma anomalia de procrastinação detectada.<br>
          Continue assim!
        </div>
      </div>`;
    return;
  }

  const alerts = anomalies
    .slice(0, 5)
    .map((rec, idx) => {
      const title = escapeHtml(rec.task?.title ?? `Tarefa #${rec.id ?? idx + 1}`);
      const count = rec.postponeCount ?? 0;
      const suggestion = rec.aiSuggestion
        ? escapeHtml(rec.aiSuggestion)
        : "Nenhuma sugestão disponível.";
      const expandId = `nt-anomaly-expand-${idx}`;
      const textId = `nt-anomaly-text-${idx}`;
      const zScore = rec.zScore != null
        ? `Z = ${Number(rec.zScore).toFixed(2)}`
        : "";

      return `
        <div class="nt-anomaly-alert">
          <div class="nt-anomaly-alert-header">
            <span class="nt-anomaly-task-title">${title}</span>
            <span class="nt-anomaly-count-badge">${count}× adiada</span>
          </div>
          ${zScore ? `<p style="font-size:11px; color:var(--text-muted); margin-bottom:6px;">${zScore}</p>` : ""}
          <p class="nt-anomaly-suggestion" id="${textId}">${suggestion}</p>
          <button
            class="nt-anomaly-expand-btn"
            id="${expandId}"
            onclick="(function(){
              var t = document.getElementById('${textId}');
              var b = document.getElementById('${expandId}');
              var expanded = t.classList.toggle('expanded');
              b.textContent = expanded ? 'Ver menos' : 'Ver tudo';
            })()"
          >Ver tudo</button>
        </div>`;
    })
    .join("");

  el.innerHTML = `
    <div class="nt-anomaly-list">${alerts}</div>
    ${anomalies.length > 5
      ? `<p style="font-size:11px; color:var(--text-muted); margin-top:8px; text-align:right;">+${anomalies.length - 5} tarefa(s) adicionais com anomalia</p>`
      : ""}`;
}

// ─── XSS guard ────────────────────────────────────────────────────────────────

function escapeHtml(str) {
  if (typeof str !== "string") return String(str ?? "");
  return str
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;")
    .replace(/'/g, "&#x27;");
}

// ─── Ring buffer de histórico da bateria ────────────────────────────────────────

const BATTERY_HISTORY_KEY = "nt_battery_history";
const BATTERY_HISTORY_MAX = 10;

/**
 * Busca o snapshot atual, adiciona ao ring buffer persistido em sessionStorage
 * e retorna a série completa (mais antigo → mais recente).
 *
 * Caso a API falhe, retorna o que estiver em cache.
 *
 * @returns {Promise<Array>} Array de snapshots { batteryLevel, cognitiveLoad, recordedAt }.
 */
async function fetchBatteryHistory() {
  // Carrega histórico existente
  let history = [];
  try {
    const raw = sessionStorage.getItem(BATTERY_HISTORY_KEY);
    if (raw) history = JSON.parse(raw);
    if (!Array.isArray(history)) history = [];
  } catch {
    history = [];
  }

  // Tenta buscar o snapshot mais recente para adicionar ao buffer
  try {
    const snapshot = await neurotaskApi.fetchCurrentBattery();
    if (snapshot && snapshot.batteryLevel != null) {
      const entry = {
        batteryLevel:  snapshot.batteryLevel,
        cognitiveLoad: snapshot.cognitiveLoad ?? 0,
        recordedAt:    snapshot.recordedAt ?? new Date().toISOString(),
      };
      // Evita duplicatas consecutivas com o mesmo instante
      const last = history[history.length - 1];
      if (!last || last.recordedAt !== entry.recordedAt) {
        history.push(entry);
        if (history.length > BATTERY_HISTORY_MAX) {
          history = history.slice(-BATTERY_HISTORY_MAX);
        }
        try {
          sessionStorage.setItem(BATTERY_HISTORY_KEY, JSON.stringify(history));
        } catch {
          // sessionStorage pode estar cheio em contextos privados — ignorar
        }
      }
    }
  } catch {
    // falha de rede: retorna cache silenciosamente
  }

  return history;
}

// ─── Gráfico de Evolução da Bateria ─────────────────────────────────────────────

/** Instância ativa do Chart.js para evitar duplicação ao re-renderizar. */
let _batteryChart = null;

/** Evita loop infinito de retry se Chart.js não estiver disponível. */
let _batteryChartWaitAttempts = 0;

/**
 * Renderiza o gráfico de linha da Evolução da Bateria usando Chart.js.
 *
 * Espera um array de snapshots com { batteryLevel, cognitiveLoad, recordedAt }.
 * Se < 2 pontos, exibe o empty state.
 *
 * Zona de perigo: linha tracejada vermelha em batteryLevel = 40 (risco HIGH).
 *
 * @param {Array} historyData  - Snapshots ordenados do mais antigo ao mais recente.
 */
function renderBatteryChart(historyData) {
  const canvas = document.getElementById("batteryTrendChart");
  const emptyEl = document.getElementById("ntBatteryChartEmpty");
  const wrapEl = document.getElementById("ntBatteryChartWrap");

  if (!canvas) return;

  // CORREÇÃO: Aceitar 1 único ponto (antes exigia < 2)
  if (!historyData || historyData.length === 0) {
    if (canvas) canvas.style.display = "none";
    if (emptyEl) emptyEl.style.display = "block";
    return;
  }

  if (canvas) canvas.style.display = "block";
  if (emptyEl) emptyEl.style.display = "none";

  // Aguarda Chart.js estar disponível
  const ChartJS = window.Chart;
  if (!ChartJS) {
    _batteryChartWaitAttempts += 1;
    if (_batteryChartWaitAttempts === 1) {
      console.warn("[aiDashboard] Chart.js ainda não carregado, aguardando...");
    }
    if (_batteryChartWaitAttempts < 20) {
      setTimeout(() => renderBatteryChart(historyData), 500);
    }
    return;
  }

  _batteryChartWaitAttempts = 0;

  // Destrói instância anterior para evitar duplicação
  if (_batteryChart) {
    _batteryChart.destroy();
    _batteryChart = null;
  }

  const labels = historyData.map((s) => {
    const d = new Date(s.recordedAt);
    return d.toLocaleTimeString("pt-BR", { hour: "2-digit", minute: "2-digit" });
  });

  const batteryData   = historyData.map((s) => s.batteryLevel   ?? 0);
  const cognitiveData = historyData.map((s) => s.cognitiveLoad  ?? 0);

  // Determine se dark mode está ativo para ajustar grid lines
  const isDark = !document.body.classList.contains("light-mode");
  const gridColor = isDark ? "rgba(255,255,255,0.07)" : "rgba(0,0,0,0.07)";
  const tickColor = isDark ? "#6b7280" : "#9ca3af";

  const ctx = canvas.getContext("2d");
  // Gradiente
  const gradient = ctx.createLinearGradient(0, 0, 0, 200);
  gradient.addColorStop(0, "rgba(34,197,94, 0.4)");
  gradient.addColorStop(1, "rgba(34,197,94, 0.0)");

  _batteryChart = new ChartJS(canvas, {
    type: "line",
    data: {
      labels,
      datasets: [
        {
          label: "Bateria (%)",
          data: batteryData,
          borderColor: "#22c55e",
          backgroundColor: gradient,
          borderWidth: 2.5,
          pointRadius: 4,               // Aumentado
          pointHoverRadius: 6,
          pointBackgroundColor: "#22c55e",
          tension: 0.35,
          fill: true,
          yAxisID: "y",
        },
        /* Removido cognitiveData para simplificar visual, ou manter se desejar */
      ],
    },
    options: {
      responsive: true,
      maintainAspectRatio: false,
      plugins: {
        legend: { display: false },
        tooltip: { mode: "index", intersect: false },
        annotation: {
          annotations: {
            dangerLine: {
              type: 'line',
              yMin: 40,
              yMax: 40,
              borderColor: 'rgba(239, 68, 68, 0.6)',
              borderWidth: 1,
              borderDash: [4, 4],
              label: {
                content: 'Risco Alto',
                enabled: true,
                position: 'end',
                color: 'rgba(239, 68, 68, 0.8)',
                font: { size: 10 }
              }
            }
          }
        }
      },
      scales: {
        y: {
          min: 0,
          max: 100,
          grid: { color: gridColor },
          ticks: { color: tickColor, callback: (v) => v + '%' },
        },
        x: {
           grid: { display: false },
           ticks: { color: tickColor, maxRotation: 0, autoSkip: true, maxTicksLimit: 6 }
        }
      },
    },
  });
}
          fill: false,
          borderDash: [4, 3],
          yAxisID: "y",
        },
      ],
    },
    options: {
      responsive: true,
      maintainAspectRatio: false,
      interaction: { mode: "index", intersect: false },
      plugins: {
        legend: {
          position: "top",
          align: "end",
          labels: {
            font: { size: 11 },
            color: tickColor,
            boxWidth: 12,
            padding: 12,
          },
        },
        tooltip: {
          callbacks: {
            afterBody(ctxItems) {
              // Aviso de risco na dica de ferramenta
              const bLevel = ctxItems.find((i) => i.datasetIndex === 0)?.parsed?.y ?? 100;
              if (bLevel <= 20) return ["⚠️ Risco CRITICAL"];
              if (bLevel <= 40) return ["⚠️ Risco HIGH"];
              return [];
            },
          },
        },
        // Linha horizontal de perigo (annotation via plugin nativo ausente — usamos afterDraw)
      },
      scales: {
        x: {
          ticks: { color: tickColor, font: { size: 10 }, maxRotation: 0 },
          grid:  { color: gridColor },
        },
        y: {
          min: 0,
          max: 100,
          ticks: {
            color: tickColor,
            font: { size: 10 },
            callback: (v) => `${v}%`,
            stepSize: 20,
          },
          grid: { color: gridColor },
        },
      },
    },
    plugins: [
      {
        // Plugin inline: desenha zona de perigo (0–40%) e limiar tracejado
        id: "nt-danger-zone",
        beforeDraw(chart) {
          const { ctx, chartArea, scales } = chart;
          if (!chartArea) return;

          const yScale = scales.y;
          const y40 = yScale.getPixelForValue(40); // limite HIGH
          const y20 = yScale.getPixelForValue(20); // limite CRITICAL
          const { left, right, bottom } = chartArea;

          ctx.save();

          // Zona HIGH (40→20) — laranja suave
          ctx.fillStyle = "rgba(249,115,22,0.06)";
          ctx.fillRect(left, y40, right - left, y20 - y40);

          // Zona CRITICAL (20→0) — vermelho suave
          ctx.fillStyle = "rgba(239,68,68,0.09)";
          ctx.fillRect(left, y20, right - left, bottom - y20);

          // Linha limite em 40%
          ctx.beginPath();
          ctx.strokeStyle = "rgba(249,115,22,0.55)";
          ctx.lineWidth = 1;
          ctx.setLineDash([5, 4]);
          ctx.moveTo(left, y40);
          ctx.lineTo(right, y40);
          ctx.stroke();

          // Label "⚠ Risco"
          ctx.font = "10px sans-serif";
          ctx.fillStyle = "rgba(249,115,22,0.75)";
          ctx.fillText("⚠ risco", right - 50, y40 - 4);

          ctx.restore();
        },
      },
    ],
  });
}

// ─── Função principal ─────────────────────────────────────────────────────────

/**
 * Carrega os dados dos 3 módulos de IA e injeta os resultados nos cards
 * correspondentes no Dashboard.
 *
 * @param {Object} [opts]
 * @param {string}  [opts.section]    - 'battery' | 'focus' | 'procrastination' | undefined (todos)
 * @param {boolean} [opts.regenerate] - Se true, dispara geração no backend antes de buscar
 *
 * @example
 * // Chamado pelo inline script ao entrar no Dashboard:
 * window.loadAIDashboard();
 *
 * // Chamado pelo botão de refresh do card de blocos:
 * window.loadAIDashboard({ section: 'focus', regenerate: true });
 */
async function loadAIDashboard(opts = {}) {
  const { section, regenerate = false } = opts;
  const all = !section;

  // ── 1. Bateria ──────────────────────────────────────────────────────────────
  if (all || section === "battery") {
    const el = document.getElementById("ntBatteryContent");
    if (el) {
      el.innerHTML = `
        <div class="nt-ai-skeleton" style="width:50%; height:10px;"></div>
        <div class="nt-ai-skeleton" style="width:100%; height:10px;"></div>
        <div class="nt-ai-skeleton" style="width:75%; height:10px;"></div>`;
    }

    try {
      // Quando acionado pelo botão de refresh, recalcula primeiro
      if (section === "battery" || regenerate) {
        await neurotaskApi.triggerBatterySnapshot().catch(() => null);
      }
      const snapshot = await neurotaskApi.fetchCurrentBattery();
      renderBattery(snapshot);

      // Busca histórico para o gráfico: usa o endpoint /api/battery/current
      // como ponto mais recente e monta série a partir do cache do SW
      // ou chama neurotaskApi.fetchBatteryHistory() quando disponibilizado.
      // Por ora, simula histórico com o snapshot único repetido em janela variável
      // para que o gráfico já be renderizado assim que houver dados.
      const historyRaw = await fetchBatteryHistory();
      renderBatteryChart(historyRaw);

    } catch (err) {
      console.error("[aiDashboard] battery:", err.message);
      renderBattery(null);
      renderBatteryChart([]);
    }
  }

  // ── 2. Blocos de Foco ───────────────────────────────────────────────────────
  if (all || section === "focus") {
    const el = document.getElementById("ntFocusContent");
    if (el) {
      el.innerHTML = `
        <div class="nt-ai-skeleton" style="width:70%; height:10px;"></div>
        <div class="nt-ai-skeleton" style="width:100%; height:10px;"></div>
        <div class="nt-ai-skeleton" style="width:55%; height:10px;"></div>`;
    }

    try {
      let blocks;
      if (regenerate && section === "focus") {
        blocks = await neurotaskApi.generateFocusBlocks();
      } else {
        blocks = await neurotaskApi.fetchFocusBlocks();
        // Se não houver blocos ainda, gera automaticamente na primeira visita
        if (!blocks || blocks.length === 0) {
          blocks = await neurotaskApi.generateFocusBlocks();
        }
      }
      renderFocusBlocks(blocks);
    } catch (err) {
      console.error("[aiDashboard] focus blocks:", err.message);
      renderFocusBlocks([]);
    }
  }

  // ── 3. Procrastinação ───────────────────────────────────────────────────────
  if (all || section === "procrastination") {
    const el = document.getElementById("ntProcrastContent");
    if (el) {
      el.innerHTML = `
        <div class="nt-ai-skeleton" style="width:90%; height:10px;"></div>
        <div class="nt-ai-skeleton" style="width:65%; height:10px;"></div>
        <div class="nt-ai-skeleton" style="width:80%; height:10px;"></div>`;
    }

    try {
      const anomalies = await neurotaskApi.fetchProcrastinationAnomalies();
      renderProcrastination(anomalies);
    } catch (err) {
      console.error("[aiDashboard] procrastination:", err.message);
      renderProcrastination([]);
    }
  }
}

// ─── Registro global (ponte para o inline script) ────────────────────────────
window.loadAIDashboard = loadAIDashboard;
