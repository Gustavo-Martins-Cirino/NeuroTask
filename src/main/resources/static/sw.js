/* Service Worker - NeuroTask (PWA + Web Push + Network-First Cache)
 *
 * Estratégia:
 *   - Endpoints de IA (/api/battery/current, /api/procrastination/anomalies,
 *     /api/batching/blocks): Network-First com fallback para cache.
 *     Isso garante dados frescos quando online e graceful degradation offline.
 *   - Demais rotas: passagem direta ao browser (sem interceptação).
 *   - Push / notificationclick: inalterados.
 */

const CACHE_NAME = "neurotask-ai-v1";

/**
 * Endpoints que usam Network-First.
 * Apenas correspondência exata de pathname — sem curingas para evitar
 * captura acidental de outros endpoints.
 */
const NETWORK_FIRST_PATHS = new Set([
  "/api/battery/current",
  "/api/procrastination/anomalies",
  "/api/batching/blocks",
]);

// ─── Ciclo de vida ────────────────────────────────────────────────────────────

self.addEventListener("install", (event) => {
  self.skipWaiting();
});

self.addEventListener("activate", (event) => {
  // Remove versões antigas do cache ao ativar nova versão do SW
  event.waitUntil(
    caches
      .keys()
      .then((keys) =>
        Promise.all(
          keys
            .filter((k) => k !== CACHE_NAME)
            .map((k) => caches.delete(k))
        )
      )
      .then(() => self.clients.claim())
  );
});

// ─── Interceptação de fetch ───────────────────────────────────────────────────

self.addEventListener("fetch", (event) => {
  // Ignora requisições que não sejam GET (POST snapshot, etc.)
  if (event.request.method !== "GET") return;

  let pathname;
  try {
    pathname = new URL(event.request.url).pathname;
  } catch {
    return;
  }

  if (!NETWORK_FIRST_PATHS.has(pathname)) return;

  event.respondWith(networkFirst(event.request));
});

/**
 * Estratégia Network-First:
 *   1. Tenta a rede → armazena resposta bem-sucedida em cache → retorna.
 *   2. Se falhar (offline / timeout) → retorna do cache, se disponível.
 *   3. Se não houver cache → retorna JSON null para que o cliente JS
 *      possa tratar o estado offline sem lançar exceção.
 *
 * @param {Request} request
 * @returns {Promise<Response>}
 */
async function networkFirst(request) {
  const cache = await caches.open(CACHE_NAME);

  try {
    const networkResponse = await fetch(request.clone());

    // Só armazena em cache respostas válidas
    if (networkResponse && networkResponse.ok) {
      await cache.put(request, networkResponse.clone());
    }

    return networkResponse;
  } catch (_networkError) {
    const cached = await cache.match(request);
    if (cached) return cached;

    // Fallback seguro: JSON null — o módulo neurotask-api.js já lida com null
    return new Response(JSON.stringify(null), {
      status: 200,
      headers: { "Content-Type": "application/json; charset=utf-8" },
    });
  }
}

self.addEventListener("push", (event) => {
  let data = {};
  try {
    data = event.data ? event.data.json() : {};
  } catch {
    try {
      data = { body: event.data ? event.data.text() : "" };
    } catch {
      data = {};
    }
  }

  const title = data.title || "🔔 NeuroTask";
  const options = {
    body: data.body || "",
    data: {
      url: data.url || "/",
    },
  };

  event.waitUntil(self.registration.showNotification(title, options));
});

self.addEventListener("notificationclick", (event) => {
  event.notification.close();

  const targetUrl =
    (event.notification &&
      event.notification.data &&
      event.notification.data.url) ||
    "/";

  event.waitUntil(
    (async () => {
      const allClients = await self.clients.matchAll({
        type: "window",
        includeUncontrolled: true,
      });

      for (const client of allClients) {
        if (
          client.url &&
          new URL(client.url).pathname ===
            new URL(targetUrl, self.location.origin).pathname
        ) {
          await client.focus();
          return;
        }
      }

      await self.clients.openWindow(targetUrl);
    })()
  );
});
