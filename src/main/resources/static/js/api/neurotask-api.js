/**
 * neurotask-api.js — Client HTTP para os módulos de IA do NeuroTask
 *
 * Cobre os endpoints de Bateria Social/Foco, Rastreamento de Procrastinação
 * e Agrupamento por Contexto (Batching). Segue o mesmo padrão de
 * taskService.js: Fetch API nativa, token via X-Auth-Token, tratamento
 * robusto de erros e suporte offline para o PWA.
 *
 * Uso:
 *   import { neurotaskApi } from './neurotask-api.js';
 *   const battery = await neurotaskApi.fetchCurrentBattery();
 */

const API_BASE = "/api";

// ─── Helpers internos ────────────────────────────────────────────────────────

/** Lê o token armazenado pelo fluxo de login. */
function getAuthToken() {
  try {
    return localStorage.getItem("neurotask_token");
  } catch {
    return null;
  }
}

/**
 * Monta os headers padrão com o token de autenticação.
 * @param {Object} extra - Headers adicionais (ex: Content-Type).
 */
function getAuthHeaders(extra = {}) {
  const token = getAuthToken();
  const base = token ? { "X-Auth-Token": token } : {};
  return { ...base, ...extra };
}

/**
 * Trata resposta 401 — remove o token expirado e redireciona para login.
 * @param {number} status
 */
function handleAuthFailure(status) {
  if (status === 401) {
    try {
      localStorage.removeItem("neurotask_token");
    } catch { /* ignore */ }
    if (typeof window !== "undefined" && typeof window.showAuthView === "function") {
      window.showAuthView("login");
    }
  }
}

/**
 * Wrapper de fetch com retry automático para falhas de servidor (5xx) e
 * detecção de modo offline (TypeError: Failed to fetch).
 *
 * @param {string} url
 * @param {RequestInit} options
 * @param {number} retries - Número de tentativas adicionais (padrão: 1).
 * @throws {OfflineError} quando o navegador está sem rede.
 * @throws {ApiError}    para respostas HTTP de erro após esgotar retries.
 */
async function fetchWithRetry(url, options, retries = 1) {
  let lastError;

  for (let attempt = 0; attempt <= retries; attempt++) {
    try {
      const response = await fetch(url, options);

      if (!response.ok) {
        handleAuthFailure(response.status);

        // 5xx: pode ser transitório, tenta novamente
        if (response.status >= 500 && attempt < retries) {
          await sleep(1000);
          continue;
        }

        const body = await response.text().catch(() => "");
        const err = new Error(body || `HTTP ${response.status}`);
        err.status = response.status;
        err.isApiError = true;
        throw err;
      }

      return response;

    } catch (error) {
      // Sem rede: TypeError lançado pelo fetch
      if (error instanceof TypeError) {
        const offlineErr = new Error("Sem conexão com a internet. Verifique sua rede.");
        offlineErr.isOffline = true;
        throw offlineErr;
      }

      // Erro de API já tratado acima, propaga imediatamente
      if (error.isApiError) throw error;

      lastError = error;
      if (attempt < retries) {
        await sleep(1000);
      }
    }
  }

  throw lastError;
}

function sleep(ms) {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

// ─── API pública ─────────────────────────────────────────────────────────────

export const neurotaskApi = {

  // ── Bateria Social/Foco ─────────────────────────────────────────────────────

  /**
   * Busca o snapshot mais recente da bateria cognitiva do usuário.
   *
   * @returns {Promise<Object|null>} Snapshot com campos: batteryLevel, cognitiveLoad,
   *   burnoutRiskScore, riskLevel, suggestedBreakMinutes, aiNotes, recordedAt.
   *   Retorna `null` quando o servidor responde 204 (nenhum snapshot ainda).
   *
   * @example
   * const battery = await neurotaskApi.fetchCurrentBattery();
   * if (battery) {
   *   renderBatteryWidget(battery.batteryLevel, battery.riskLevel);
   * }
   */
  async fetchCurrentBattery() {
    try {
      const response = await fetchWithRetry(`${API_BASE}/battery/current`, {
        cache: "no-store",
        headers: getAuthHeaders({ "Cache-Control": "no-cache" }),
      });

      if (response.status === 204) return null;
      return response.json();

    } catch (error) {
      if (error.isOffline) {
        console.warn("[NeuroTask] fetchCurrentBattery: offline, retornando null.");
        return null;
      }
      console.error("[NeuroTask] fetchCurrentBattery:", error.message);
      throw error;
    }
  },

  /**
   * Aciona o cálculo de um novo snapshot da bateria para o usuário autenticado.
   * Use após criar ou agendar tarefas pesadas para obter a carga atualizada.
   *
   * @returns {Promise<Object>} O novo snapshot persistido.
   *
   * @example
   * await neurotaskApi.triggerBatterySnapshot();
   */
  async triggerBatterySnapshot() {
    try {
      const response = await fetchWithRetry(`${API_BASE}/battery/snapshot`, {
        method: "POST",
        headers: getAuthHeaders(),
      });
      return response.json();

    } catch (error) {
      if (error.isOffline) {
        console.warn("[NeuroTask] triggerBatterySnapshot: offline, snapshot não salvo.");
        return null;
      }
      console.error("[NeuroTask] triggerBatterySnapshot:", error.message);
      throw error;
    }
  },

  // ── Rastreamento de Procrastinação ──────────────────────────────────────────

  /**
   * Busca métricas resumidas de procrastinação sem baixar todos os registros.
   *
   * @returns {Promise<Object>} Objeto com: totalPostponements, tasksWithPostponements,
   *   anomalyCount, pendingSuggestions.
   *
   * @example
   * const stats = await neurotaskApi.fetchProcrastinationStats();
   * badge.textContent = stats.anomalyCount;
   */
  async fetchProcrastinationStats() {
    try {
      const response = await fetchWithRetry(`${API_BASE}/procrastination/stats`, {
        cache: "no-store",
        headers: getAuthHeaders({ "Cache-Control": "no-cache" }),
      });
      return response.json();

    } catch (error) {
      if (error.isOffline) {
        console.warn("[NeuroTask] fetchProcrastinationStats: offline.");
        return { totalPostponements: 0, tasksWithPostponements: 0, anomalyCount: 0, pendingSuggestions: 0 };
      }
      console.error("[NeuroTask] fetchProcrastinationStats:", error.message);
      throw error;
    }
  },

  /**
   * Lista os registros de procrastinação anômalos do usuário, incluindo as
   * sugestões de decomposição geradas pela IA.
   *
   * @returns {Promise<Array>} Array de ProcrastinationRecord com campos:
   *   id, task { id, title }, postponeCount, zScore, isAnomaly,
   *   aiSuggestion, detectedAt.
   *   Retorna `[]` quando offline, para não bloquear o painel.
   *
   * @example
   * const anomalies = await neurotaskApi.fetchProcrastinationAnomalies();
   * anomalies.forEach(a => renderAnomalyCard(a));
   */
  async fetchProcrastinationAnomalies() {
    try {
      const response = await fetchWithRetry(`${API_BASE}/procrastination/anomalies`, {
        cache: "no-store",
        headers: getAuthHeaders({ "Cache-Control": "no-cache" }),
      });
      return response.json();

    } catch (error) {
      if (error.isOffline) {
        console.warn("[NeuroTask] fetchProcrastinationAnomalies: offline, retornando [].");
        return [];
      }
      console.error("[NeuroTask] fetchProcrastinationAnomalies:", error.message);
      throw error;
    }
  },

  // ── Agrupamento por Contexto (Batching) ─────────────────────────────────────

  /**
   * Aciona o algoritmo de clustering e gera/atualiza os blocos de foco do dia.
   * Chame ao abrir o planejador diário ou após alterar o contextType de uma tarefa.
   *
   * @returns {Promise<Array>} Lista de TaskContextGroup ordenados por duração
   *   decrescente. Cada item contém: id, name, contextType, colorHex, description,
   *   suggestedFocusDurationMinutes, tasks[].
   *   Retorna `[]` quando offline.
   *
   * @example
   * const blocks = await neurotaskApi.generateFocusBlocks();
   * renderFocusBlocksPanel(blocks);
   */
  async generateFocusBlocks() {
    try {
      const response = await fetchWithRetry(`${API_BASE}/batching/generate`, {
        method: "POST",
        headers: getAuthHeaders(),
      });
      return response.json();

    } catch (error) {
      if (error.isOffline) {
        console.warn("[NeuroTask] generateFocusBlocks: offline, retornando [].");
        return [];
      }
      console.error("[NeuroTask] generateFocusBlocks:", error.message);
      throw error;
    }
  },

  /**
   * Retorna os blocos de foco já calculados sem re-executar o algoritmo.
   * Use para re-renderizar o painel sem custo computacional no backend.
   *
   * @returns {Promise<Array>} Lista de TaskContextGroup (mesmo formato de generateFocusBlocks).
   *   Retorna `[]` quando offline.
   *
   * @example
   * const blocks = await neurotaskApi.fetchFocusBlocks();
   * if (blocks.length === 0) {
   *   await neurotaskApi.generateFocusBlocks();
   * }
   */
  async fetchFocusBlocks() {
    try {
      const response = await fetchWithRetry(`${API_BASE}/batching/blocks`, {
        cache: "no-store",
        headers: getAuthHeaders({ "Cache-Control": "no-cache" }),
      });
      return response.json();

    } catch (error) {
      if (error.isOffline) {
        console.warn("[NeuroTask] fetchFocusBlocks: offline, retornando [].");
        return [];
      }
      console.error("[NeuroTask] fetchFocusBlocks:", error.message);
      throw error;
    }
  },

  // ── Focus / Pomodoro ────────────────────────────────────────────────────────

  async startFocusSession(taskId, plannedMinutes = 25, breakMinutes = 5, totalCycles = 4) {
    try {
      const response = await fetchWithRetry(`${API_BASE}/focus/start`, {
        method: "POST",
        headers: getAuthHeaders({ "Content-Type": "application/json" }),
        body: JSON.stringify({ taskId, plannedMinutes, breakMinutes, totalCycles }),
      });
      return response.json();
    } catch (error) {
      if (error.isOffline) return null;
      throw error;
    }
  },

  async pauseFocusSession(sessionId) {
    try {
      const response = await fetchWithRetry(`${API_BASE}/focus/${sessionId}/pause`, {
        method: "PATCH",
        headers: getAuthHeaders(),
      });
      return response.json();
    } catch (error) {
      if (error.isOffline) return null;
      throw error;
    }
  },

  async resumeFocusSession(sessionId) {
    try {
      const response = await fetchWithRetry(`${API_BASE}/focus/${sessionId}/resume`, {
        method: "PATCH",
        headers: getAuthHeaders(),
      });
      return response.json();
    } catch (error) {
      if (error.isOffline) return null;
      throw error;
    }
  },

  async completeFocusSession(sessionId) {
    try {
      const response = await fetchWithRetry(`${API_BASE}/focus/${sessionId}/complete`, {
        method: "PATCH",
        headers: getAuthHeaders(),
      });
      return response.json();
    } catch (error) {
      if (error.isOffline) return null;
      throw error;
    }
  },

  async nextFocusCycle(sessionId) {
    try {
      const response = await fetchWithRetry(`${API_BASE}/focus/${sessionId}/next-cycle`, {
        method: "PATCH",
        headers: getAuthHeaders(),
      });
      return response.json();
    } catch (error) {
      if (error.isOffline) return null;
      throw error;
    }
  },

  async cancelFocusSession(sessionId) {
    try {
      const response = await fetchWithRetry(`${API_BASE}/focus/${sessionId}/cancel`, {
        method: "PATCH",
        headers: getAuthHeaders(),
      });
      return response.json();
    } catch (error) {
      if (error.isOffline) return null;
      throw error;
    }
  },

  async getActiveFocusSession() {
    try {
      const response = await fetchWithRetry(`${API_BASE}/focus/active`, {
        cache: "no-store",
        headers: getAuthHeaders({ "Cache-Control": "no-cache" }),
      });
      if (response.status === 204) return null;
      return response.json();
    } catch (error) {
      if (error.isOffline) return null;
      throw error;
    }
  },

  async getFocusStats() {
    try {
      const response = await fetchWithRetry(`${API_BASE}/focus/stats`, {
        cache: "no-store",
        headers: getAuthHeaders({ "Cache-Control": "no-cache" }),
      });
      return response.json();
    } catch (error) {
      if (error.isOffline) return { totalSessions: 0, completedSessions: 0, totalFocusMinutes: 0, avgSessionMinutes: 0, todaySessions: 0, todayFocusMinutes: 0, recentSessions: [] };
      throw error;
    }
  },

  // ── Energy Mapping ──────────────────────────────────────────────────────────

  async logEnergy(logDate, period, level, notes = "") {
    try {
      const response = await fetchWithRetry(`${API_BASE}/energy`, {
        method: "POST",
        headers: getAuthHeaders({ "Content-Type": "application/json" }),
        body: JSON.stringify({ logDate, period, level, notes }),
      });
      return response.json();
    } catch (error) {
      if (error.isOffline) return null;
      throw error;
    }
  },

  async getTodayEnergy() {
    try {
      const response = await fetchWithRetry(`${API_BASE}/energy/today`, {
        cache: "no-store",
        headers: getAuthHeaders({ "Cache-Control": "no-cache" }),
      });
      return response.json();
    } catch (error) {
      if (error.isOffline) return [];
      throw error;
    }
  },

  async getEnergyHistory(days = 14) {
    try {
      const response = await fetchWithRetry(`${API_BASE}/energy/history?days=${days}`, {
        cache: "no-store",
        headers: getAuthHeaders({ "Cache-Control": "no-cache" }),
      });
      return response.json();
    } catch (error) {
      if (error.isOffline) return [];
      throw error;
    }
  },

  async getEnergyPatterns() {
    try {
      const response = await fetchWithRetry(`${API_BASE}/energy/patterns`, {
        cache: "no-store",
        headers: getAuthHeaders({ "Cache-Control": "no-cache" }),
      });
      return response.json();
    } catch (error) {
      if (error.isOffline) return null;
      throw error;
    }
  },

  // ── Smart Rescheduling ──────────────────────────────────────────────────────

  async getRescheduleSuggestions() {
    try {
      const response = await fetchWithRetry(`${API_BASE}/reschedule/suggestions`, {
        cache: "no-store",
        headers: getAuthHeaders({ "Cache-Control": "no-cache" }),
      });
      return response.json();
    } catch (error) {
      if (error.isOffline) return [];
      throw error;
    }
  },

  async acceptReschedule(taskId, suggestedStart, suggestedEnd) {
    try {
      const response = await fetchWithRetry(`${API_BASE}/reschedule/accept`, {
        method: "POST",
        headers: getAuthHeaders({ "Content-Type": "application/json" }),
        body: JSON.stringify({ taskId, suggestedStart, suggestedEnd }),
      });
      return response.json();
    } catch (error) {
      if (error.isOffline) return null;
      throw error;
    }
  },

  // ── Habit Streaks + Gamification ────────────────────────────────────────────

  async habitCheckIn(habitType) {
    try {
      const response = await fetchWithRetry(`${API_BASE}/habits/checkin`, {
        method: "POST",
        headers: getAuthHeaders({ "Content-Type": "application/json" }),
        body: JSON.stringify({ habitType }),
      });
      return response.json();
    } catch (error) {
      if (error.isOffline) return null;
      throw error;
    }
  },

  async getGamificationData() {
    try {
      const response = await fetchWithRetry(`${API_BASE}/habits`, {
        cache: "no-store",
        headers: getAuthHeaders({ "Cache-Control": "no-cache" }),
      });
      return response.json();
    } catch (error) {
      if (error.isOffline) return { streaks: [], badges: [], totalPoints: 0, level: 1 };
      throw error;
    }
  },

  async getBadges() {
    try {
      const response = await fetchWithRetry(`${API_BASE}/habits/badges`, {
        cache: "no-store",
        headers: getAuthHeaders({ "Cache-Control": "no-cache" }),
      });
      return response.json();
    } catch (error) {
      if (error.isOffline) return [];
      throw error;
    }
  },

  // ── Weekly Review ───────────────────────────────────────────────────────────

  async generateWeeklyReview() {
    try {
      const response = await fetchWithRetry(`${API_BASE}/weekly-review/generate`, {
        method: "POST",
        headers: getAuthHeaders(),
      });
      return response.json();
    } catch (error) {
      if (error.isOffline) return null;
      throw error;
    }
  },

  async getWeeklyReviewHistory() {
    try {
      const response = await fetchWithRetry(`${API_BASE}/weekly-review`, {
        cache: "no-store",
        headers: getAuthHeaders({ "Cache-Control": "no-cache" }),
      });
      return response.json();
    } catch (error) {
      if (error.isOffline) return [];
      throw error;
    }
  },

  async getWeeklyReviewById(reviewId) {
    try {
      const response = await fetchWithRetry(`${API_BASE}/weekly-review/${reviewId}`, {
        cache: "no-store",
        headers: getAuthHeaders({ "Cache-Control": "no-cache" }),
      });
      return response.json();
    } catch (error) {
      if (error.isOffline) return null;
      throw error;
    }
  },
};
