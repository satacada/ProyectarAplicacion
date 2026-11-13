# 🗺️ ROADMAP MAESTRO UNIFICADO — OmniCast Pro (`v4.4` Multi-Agente)

> **Versión Base de Rescate:** `v3.7.0` (`versionCodeSender: 368` | `versionCodeReceiver: 351`)  
> **Política Operativa `v4.4`:** Micro-comandos `[auto]` y `[ver]` orquestados por el Planificador (`Argon`) invocando subagentes aislados (`flash` / `flash_lite`) | **Prohibido disparar Explorador Forense en tareas mapeadas** (todas las rutas están en este archivo y en [`doc/MAPA_DEL_SISTEMA_Y_FLUJO_DE_CODIGO.md`](file:///home/sataca/Descargas/APLICACIONES/proyectarAplicacion/doc/MAPA_DEL_SISTEMA_Y_FLUJO_DE_CODIGO.md)).  
> **Estado Global:** 🎉 **FASE 8 COMPLETADA AL 100% (Hitos 8.1 [CH-11] al 8.17 [CH-27] — Discriminación Estricta de Páginas de Partidos y Firmas Reales HLS — 167 Tests Passing — 0 Archivos Kotlin > 300L)**

---

## 🎮 FASE 8 — Experiencia de Control Remoto D-Pad Avanzada y Resiliencia de Reproductores TV (`[x]` — 17 / 17 completados — 100%)

- `[x]` **Hito 8.1 [CH-11] — Foco D-Pad en Controles TV y Protección de Reproductores (`app-receiver/src/main/kotlin/com/omnicast/receiver/ui/TvRemoteKeyHandler.kt`, `app-receiver/src/main/kotlin/com/omnicast/receiver/ui/components/TvDirectControlsOverlay.kt`, `app-sender/src/main/kotlin/com/omnicast/sender/ui/browser/AdBlocker.kt`):**
  - **Objetivo:** Garantizar gestión reactiva de D-Pad en `TvRemoteKeyHandler.kt` (276L) mediante `hasActiveOverlay` y `notifyControlsVisibilityChanged`, cediendo flechas D-Pad y ENTER al OSD/Compose TV para permitir seleccionar botones interactivos (Ajustes, Subtítulos, etc.), preservando atajos de salto +/-10s en pantalla limpia. Inyección inmediata de `FocusRequester` en el botón central de `TvDirectControlsOverlay.kt` (256L) y blindaje de reproductores web legítimos (`isLegitimate`) en `AdBlocker.kt` (252L) para SoloLatino (`player.pelisserieshoy.com`) e inmunidad a clics de inicio de video.
  - **Archivos Objetivo Exactos:**
    - `app-receiver/src/main/kotlin/com/omnicast/receiver/ui/TvRemoteKeyHandler.kt` (276 líneas)
    - `app-receiver/src/main/kotlin/com/omnicast/receiver/ui/components/TvDirectControlsOverlay.kt` (256 líneas)
    - `app-sender/src/main/kotlin/com/omnicast/sender/ui/browser/AdBlocker.kt` (252 líneas)
  - **Agentes Involucrados:** `planner-architect` (`Argon`) ➔ `specialist-tv-player-pvr` (`flash`) ➔ `surgical-developer` (`flash`) ➔ `qa-oracle-tester` (`flash`) ➔ `sync-documenter` (`flash`) ➔ `git-ota-releaser` (`flash_lite`).

- `[x]` **Hito 8.2 [CH-12] — Blindaje Anti-Desvío 1er Toque y Calibración de Buffer Doupload/Doodstream (`app-receiver/src/main/kotlin/com/omnicast/receiver/ui/components/TvWebStreamScreen.kt`, `app-receiver/src/main/kotlin/com/omnicast/receiver/directstream/ExoPlayerTvManager.kt`, `app-sender/src/main/kotlin/com/omnicast/sender/ui/browser/AdBlocker.kt`):**
  - **Objetivo:** Neutralizar velos transparentes y enlaces trampa sobre el reproductor en el 1er toque (`AdBlocker.kt`: 269L) reenviando el clic al elemento subyacente para reproducir inmediatamente sin desvíos. Calibrar watchdog a 22s en `TvWebStreamScreen.kt` (291L) y timeouts a 15s en `ExoPlayerTvManager.kt` (285L) para CDNs de alta latencia/handshake lento (Doupload, Dropupload, Doodstream).
  - **Archivos Objetivo Exactos:**
    - `app-receiver/src/main/kotlin/com/omnicast/receiver/ui/components/TvWebStreamScreen.kt` (291 líneas)
    - `app-receiver/src/main/kotlin/com/omnicast/receiver/directstream/ExoPlayerTvManager.kt` (285 líneas)
    - `app-sender/src/main/kotlin/com/omnicast/sender/ui/browser/AdBlocker.kt` (269 líneas)
  - **Agentes Involucrados:** `planner-architect` (`Argon`) ➔ `specialist-tv-player-pvr` (`flash`) ➔ `surgical-developer` (`flash`) ➔ `qa-oracle-tester` (`flash`) ➔ `sync-documenter` (`flash`) ➔ `git-ota-releaser` (`flash_lite`).

- `[x]` **Hito 8.3 [CH-13] — Blindaje Anti-Popups de Iframes y Watchdog Adaptativo de Buffer (`app-sender/src/main/kotlin/com/omnicast/sender/ui/browser/BrowserPopupInterceptor.kt`, `app-sender/src/main/kotlin/com/omnicast/sender/ui/browser/BrowserWebView.kt`, `app-sender/src/main/kotlin/com/omnicast/sender/ui/browser/AdBlocker.kt`, `app-receiver/src/main/kotlin/com/omnicast/receiver/ui/components/TvWebHtmlBuilder.kt`, `app-receiver/src/main/kotlin/com/omnicast/receiver/ui/components/TvWebStreamScreen.kt`):**
  - **Objetivo:** Erradicar desvíos forzados por window.open() en iframes de Doodstream activando setSupportMultipleWindows(true) y delegando en BrowserPopupInterceptor (64L) con receptor efímero en memoria sin tocar la ventana principal, preservando buscadores y captchas (geetest, arkose). Calibrar watchdog adaptativo por arrendamiento de inactividad (heartbeats MANIFEST_PARSED, FRAG_LOADING, FRAG_LOADED en TvWebHtmlBuilder: 231L) en TvWebStreamScreen (283L) con doble umbral (22s silencio / 60s techo máximo) para CDNs de alta latencia como DropCDN/Dropload.
  - **Archivos Objetivo Exactos:**
    - `app-sender/src/main/kotlin/com/omnicast/sender/ui/browser/BrowserPopupInterceptor.kt` (64 líneas)
    - `app-sender/src/main/kotlin/com/omnicast/sender/ui/browser/BrowserWebView.kt` (289 líneas)
    - `app-sender/src/main/kotlin/com/omnicast/sender/ui/browser/AdBlocker.kt` (270 líneas)
    - `app-receiver/src/main/kotlin/com/omnicast/receiver/ui/components/TvWebHtmlBuilder.kt` (231 líneas)
    - `app-receiver/src/main/kotlin/com/omnicast/receiver/ui/components/TvWebStreamScreen.kt` (283 líneas)
  - **Agentes Involucrados:** `planner-architect` (`Argon`) ➔ `specialist-tv-player-pvr` (`flash`) ➔ `surgical-developer` (`flash`) ➔ `qa-oracle-tester` (`flash`) ➔ `sync-documenter` (`flash`) ➔ `git-ota-releaser` (`flash_lite`).

- `[x]` **Hito 8.4 [CH-14] — Erradicación de Recargas Forzadas por Popups y Camuflaje Universal Anti-Detección (`app-sender/src/main/kotlin/com/omnicast/sender/ui/browser/WvcShieldSecurityEngine.kt`, `app-sender/src/main/kotlin/com/omnicast/sender/ui/browser/BrowserPopupInterceptor.kt`):**
  - **Objetivo:** Erradicar la instrucción destructiva `window.location.href = url` dentro de `window.open` en `WvcShieldSecurityEngine.kt` (235L) que forzaba recargas de página al interactuar con selectores/botones de reproducción, delegando la gestión nativa de ventanas al interceptor `BrowserPopupInterceptor.kt` (80L) para permitir navegación interna limpia de mismo origen sin tocar la ventana principal ante anuncios. Inyección universal estándar de `navigator.webdriver = false` en el prototipo Chromium para evadir falsos positivos de automatización en portales protegidos (SoloLatino, Cloudflare) con 0% código en duro.
  - **Archivos Objetivo Exactos:**
    - `app-sender/src/main/kotlin/com/omnicast/sender/ui/browser/WvcShieldSecurityEngine.kt` (235 líneas)
    - `app-sender/src/main/kotlin/com/omnicast/sender/ui/browser/BrowserPopupInterceptor.kt` (80 líneas)
  - **Agentes Involucrados:** `planner-architect` (`Argon`) ➔ `surgical-developer` (`flash`) ➔ `qa-oracle-tester` (`flash`) ➔ `sync-documenter` (`flash`) ➔ `git-ota-releaser` (`flash_lite`).

- `[x]` **Hito 8.5 [CH-15] — Sonda Transparente de Diagnóstico HTTP/JS y Chrome DevTools Remoto (`app-sender/src/main/kotlin/com/omnicast/sender/ui/browser/BrowserWebView.kt`, `app-sender/src/main/kotlin/com/omnicast/sender/ui/browser/sentinel/WebPlayerDiagnosticSentinel.kt`):**
  - **Objetivo:** Activación de depuración remota de Chromium WebView (`setWebContentsDebuggingEnabled(true)`) para diagnóstico profundo vía `chrome://inspect`, e integración desacoplada en el centinela `WebPlayerDiagnosticSentinel.kt` (89L) de sonda de inspección de peticiones a APIs internas (`/api/` con registro de headers), captura de errores de estado HTTP (`onReceivedHttpError`) y advertencias de consola JS (`BROWSER_JS_DIAG`) con 0% código en duro y manteniendo `BrowserWebView.kt` en 292L.
  - **Archivos Objetivo Exactos:**
    - `app-sender/src/main/kotlin/com/omnicast/sender/ui/browser/BrowserWebView.kt` (292 líneas)
    - `app-sender/src/main/kotlin/com/omnicast/sender/ui/browser/sentinel/WebPlayerDiagnosticSentinel.kt` (89 líneas)
  - **Agentes Involucrados:** `planner-architect` (`Argon`) ➔ `specialist-stream-extraction` (`flash`) ➔ `surgical-developer` (`flash`) ➔ `qa-oracle-tester` (`flash`) ➔ `sync-documenter` (`flash`) ➔ `git-ota-releaser` (`flash_lite`).

- `[x]` **Hito 8.6 [CH-16] — User-Agent Canónico Dinámico y Sincronización Nativa de Client Hints (`app-sender/src/main/kotlin/com/omnicast/sender/ui/browser/BrowserUrlHelper.kt`, `app-sender/src/main/kotlin/com/omnicast/sender/ui/browser/BrowserWebView.kt`, `app-sender/src/main/kotlin/com/omnicast/sender/ui/browser/WvcShieldSecurityEngine.kt`):**
  - **Objetivo:** Erradicación del User-Agent hardcodeado en Chrome 124 mediante derivación canónica dinámica en `BrowserUrlHelper.kt` (260L) a partir de `WebSettings.getDefaultUserAgent(ctx)` (limpiando tokens de webview incrustado para paridad 1:1 con Google Chrome oficial). Poda del bloque artificial en JavaScript de `navigator.userAgentData` en `WvcShieldSecurityEngine.kt` (208L), permitiendo que la pila C++ de Chromium emita cabeceras `sec-ch-ua` legítimas con versiones completas y sin campos vacíos, eliminando la causa raíz del bloqueo HTTP 403 de Cloudflare WAF en llamadas a endpoints `/api/` con 0% código en duro y manteniendo `BrowserWebView.kt` en 292L.
  - **Archivos Objetivo Exactos:**
    - `app-sender/src/main/kotlin/com/omnicast/sender/ui/browser/BrowserUrlHelper.kt` (260 líneas)
    - `app-sender/src/main/kotlin/com/omnicast/sender/ui/browser/BrowserWebView.kt` (292 líneas)
    - `app-sender/src/main/kotlin/com/omnicast/sender/ui/browser/WvcShieldSecurityEngine.kt` (208 líneas)
  - **Agentes Involucrados:** `planner-architect` (`Argon`) ➔ `specialist-stream-extraction` (`flash`) ➔ `surgical-developer` (`flash`) ➔ `qa-oracle-tester` (`flash`) ➔ `sync-documenter` (`flash`) ➔ `git-ota-releaser` (`flash_lite`).

- `[x]` **Hito 8.7 [CH-17] — Sincronización Canónica de UserAgentMetadata y Client Hints Oficiales (`app-sender/src/main/kotlin/com/omnicast/sender/ui/browser/BrowserUrlHelper.kt`, `app-sender/src/main/kotlin/com/omnicast/sender/ui/browser/BrowserWebView.kt`):**
  - **Objetivo:** Inyección nativa de `androidx.webkit.UserAgentMetadata` a través de `WebSettingsCompat.setUserAgentMetadata(settings, meta)` en `BrowserUrlHelper.applyCanonicalUserAgent()`, poblando dinámicamente marcas de Chromium, Google Chrome y Not_A Brand con versiones reales derivadas de `WebSettings.getDefaultUserAgent(ctx)`, arquitectura arm/arm64, bitness (32/64) y release de Android. Erradicación de la emisión mutilada de `sec-ch-ua` con versión vacía (`v=""`), anulando el rechazo HTTP 403 de Cloudflare WAF en llamadas AJAX `/api/` con 0% código en duro y preservando `BrowserUrlHelper.kt` (290L) y `BrowserWebView.kt` (292L) estrictamente <= 300 líneas.
  - **Archivos Objetivo Exactos:**
    - `app-sender/src/main/kotlin/com/omnicast/sender/ui/browser/BrowserUrlHelper.kt` (290 líneas)
    - `app-sender/src/main/kotlin/com/omnicast/sender/ui/browser/BrowserWebView.kt` (292 líneas)
  - **Agentes Involucrados:** `planner-architect` (`Argon`) ➔ `specialist-stream-extraction` (`flash`) ➔ `surgical-developer` (`flash`) ➔ `qa-oracle-tester` (`flash`) ➔ `sync-documenter` (`flash`) ➔ `git-ota-releaser` (`flash_lite`).

- `[x]` **Hito 8.8 [CH-18] — Erradicación Universal de Huellas In-App y Autonomía Pura de Scripts (`app-sender/src/main/kotlin/com/omnicast/sender/ui/browser/WvcShieldSecurityEngine.kt`, `app-sender/src/main/kotlin/com/omnicast/sender/ui/browser/BrowserWebView.kt`, `app-sender/src/main/kotlin/com/omnicast/sender/ui/browser/WvcSnifferScripts.kt`, `app-sender/src/main/assets/sniffer/universal_sniffer.js`):**
  - **Objetivo:** Neutralización universal de la cabecera delatora `X-Requested-With` asignando `XMLHttpRequest` para peticiones AJAX same-origin en `WvcShieldSecurityEngine.kt` (263L) mediante hooks de `window.fetch` y `XMLHttpRequest` con `WeakMap` privado, erradicando la inyección nativa de `com.omnicast.sender` de Chromium C++ que activaba el rechazo HTTP 403 `incompatible_browser` en servidores protegidos. Poda de la directiva estéril `setRequestedWithHeaderOriginAllowList` en `BrowserWebView.kt` (289L). Erradicación total de propiedades globales `window.__omni*` en `universal_sniffer.js` encapsulando el estado en closures IIFE privados y dotando a `buildAggressiveRescanScript` en `WvcSnifferScripts.kt` (191L) de autonomía de rescaneo directa sobre el DOM sin llamadas acopladas a `window`, manteniendo el objeto `window` 100% virgen e indistinguible de Google Chrome con 0% código en duro.
  - **Archivos Objetivo Exactos:**
    - `app-sender/src/main/kotlin/com/omnicast/sender/ui/browser/WvcShieldSecurityEngine.kt` (263 líneas)
    - `app-sender/src/main/kotlin/com/omnicast/sender/ui/browser/BrowserWebView.kt` (289 líneas)
    - `app-sender/src/main/kotlin/com/omnicast/sender/ui/browser/WvcSnifferScripts.kt` (191 líneas)
    - `app-sender/src/main/assets/sniffer/universal_sniffer.js`
  - **Agentes Involucrados:** `planner-architect` (`Argon`) ➔ `specialist-stream-extraction` (`flash`) ➔ `surgical-developer` (`flash`) ➔ `qa-oracle-tester` (`flash`) ➔ `sync-documenter` (`flash`) ➔ `git-ota-releaser` (`flash_lite`).

- `[x]` **Hito 8.9 [CH-19] — Defensa Universal de Iframes en Document_Start y Normalización de Orígenes (`app-sender/src/main/kotlin/com/omnicast/sender/ui/browser/WvcShieldSecurityEngine.kt`, `app-sender/src/main/kotlin/com/omnicast/sender/ui/browser/BrowserWebView.kt`):**
  - **Objetivo:** Inyección anticipada universal a nivel de todos los frames e iframes en tiempo de `document_start` (`WebViewCompat.addDocumentStartJavaScript(webView, getProtectionScript(), setOf("*"))`) en `BrowserWebView.kt` (290L). Erradicación de huellas nativas `android-app://` en `new URL(...)` dentro de `WvcShieldSecurityEngine.kt` (277L), sustituyéndolas por el origen legítimo derivado de `document.referrer` o `window.location.origin`. Bloqueo de la autodestrucción DOM en subframes mediante trampa selectiva sobre `Element.prototype.innerHTML` cuando se intenta inyectar tarjetas de redirección o tarjetas de bloqueo de incrustación (`class="card"`, `target="_top"`), permitiendo que reproductores con anti-embed estricto (como el reproductor principal de SoloLatino `player.pelisserieshoy.com`) ejecuten el pipeline de video Plyr/HLS con total normalidad, con 0% código en duro y manteniendo todos los archivos <= 300 líneas.
  - **Archivos Objetivo Exactos:**
    - `app-sender/src/main/kotlin/com/omnicast/sender/ui/browser/WvcShieldSecurityEngine.kt` (277 líneas)
    - `app-sender/src/main/kotlin/com/omnicast/sender/ui/browser/BrowserWebView.kt` (290 líneas)
  - **Agentes Involucrados:** `planner-architect` (`Argon`) ➔ `specialist-stream-extraction` (`flash`) ➔ `surgical-developer` (`flash`) ➔ `qa-oracle-tester` (`flash`) ➔ `sync-documenter` (`flash`) ➔ `git-ota-releaser` (`flash_lite`).
 
- `[x]` **Hito 8.10 [CH-20] — Poda de Código Inútil y Vía 1: Interceptación Desacoplada Anti-Embed a Nivel de Red (`app-sender/src/main/kotlin/com/omnicast/sender/ui/browser/WvcShieldSecurityEngine.kt`, `app-sender/src/main/kotlin/com/omnicast/sender/ui/browser/interceptor/PlayerAntiEmbedNetworkInterceptor.kt`, `app-sender/src/main/kotlin/com/omnicast/sender/ui/browser/BrowserWebView.kt`):**
  - **Objetivo:** Poda rigurosa de los hooks ineficaces en JavaScript de `window.URL` y `Element.prototype.innerHTML` en `WvcShieldSecurityEngine.kt` (reducido a 241L). Creación e integración del interceptor desacoplado `PlayerAntiEmbedNetworkInterceptor.kt` (62L) en `BrowserWebView.kt` (292L), actuando directamente en la capa de transporte de red (`shouldInterceptRequest` para subframes `Sec-Fetch-Dest: iframe` / `text/html`) con cookies y cabeceras completas para neutralizar la condición `isAllowedHost` (forzando retorno `true`) y neutralizar `window.stop()`. Permite que el reproductor Plyr/HLS cargue al 100% de forma nativa sin manipulación frágil del DOM y con 0% código en duro.
  - **Archivos Objetivo Exactos:**
    - `app-sender/src/main/kotlin/com/omnicast/sender/ui/browser/WvcShieldSecurityEngine.kt` (241 líneas)
    - `app-sender/src/main/kotlin/com/omnicast/sender/ui/browser/interceptor/PlayerAntiEmbedNetworkInterceptor.kt` (62 líneas)
    - `app-sender/src/main/kotlin/com/omnicast/sender/ui/browser/BrowserWebView.kt` (292 líneas)
  - **Agentes Involucrados:** `planner-architect` (`Argon`) ➔ `specialist-stream-extraction` (`flash`) ➔ `surgical-developer` (`flash`) ➔ `qa-oracle-tester` (`flash`) ➔ `sync-documenter` (`flash`) ➔ `git-ota-releaser` (`flash_lite`).

- `[x]` **Hito 8.11 [CH-21] — Reinicio Automático de Videos Concluidos y Diferenciación de Capítulos SPA (`app-sender/src/main/kotlin/com/omnicast/sender/stream/StreamResumeManager.kt`, `app-sender/src/main/kotlin/com/omnicast/sender/ui/browser/BrowserUrlHelper.kt`, `app-sender/src/main/kotlin/com/omnicast/sender/ui/SenderMainActivity.kt`, `app-sender/src/main/kotlin/com/omnicast/sender/ui/browser/InAppBrowserScreen.kt`):**
  - **Objetivo:** Erradicación del reinicio forzado en los créditos finales ("Reanudando en el minuto 58") mediante purga automática de marcas temporales (`clearPosition`) en `StreamResumeManager.kt` (60L) y `SenderMainActivity.kt` (295L) cuando la reproducción alcanza >= 95% de duración, los últimos 45 segundos o el estado `ENDED`, asegurando que cualquier video o episodio completado recomience siempre en `00:00`. Incorporación del token de parámetro `v=([a-zA-Z0-9_-]+)` en `BrowserUrlHelper.extractMediaIdentityKey` (289L) para dotar a los capítulos de servidores basados en `p.php?v=` (como SoloLatino) de identidades multimedia únicas e independientes, impidiendo colisiones o sobreescritura de streams entre episodios. Priorización inmediata en el índice 0 y activación del auto-apertura del selector de casteo en `InAppBrowserScreen.kt` (296L) ante la detección de un nuevo stream con identidad distinta al actualmente proyectado en la TV, permitiendo conmutar episodios en portales SPA con un solo toque y 0% código en duro.
  - **Archivos Objetivo Exactos:**
    - `app-sender/src/main/kotlin/com/omnicast/sender/stream/StreamResumeManager.kt` (60 líneas)
    - `app-sender/src/main/kotlin/com/omnicast/sender/ui/browser/BrowserUrlHelper.kt` (289 líneas)
    - `app-sender/src/main/kotlin/com/omnicast/sender/ui/SenderMainActivity.kt` (295 líneas)
    - `app-sender/src/main/kotlin/com/omnicast/sender/ui/browser/InAppBrowserScreen.kt` (296 líneas)
  - **Agentes Involucrados:** `planner-architect` (`Argon`) ➔ `specialist-stream-extraction` (`flash`) ➔ `surgical-developer` (`flash`) ➔ `qa-oracle-tester` (`flash`) ➔ `sync-documenter` (`flash`) ➔ `git-ota-releaser` (`flash_lite`).

- `[x]` **Hito 8.12 [CH-22] — Algoritmo Heurístico Universal de Afinidad de Marca y Navegación por Gesto Legítimo (`app-sender/src/main/kotlin/com/omnicast/sender/ui/browser/BrowserPopupInterceptor.kt`):**
  - **Objetivo:** Erradicación del bloqueo indebido de enlaces de navegación legítimos que abren en nuevas pestañas (`target="_blank"`) hacia dominios hermanos o espejos de redes deportivas y cine (como `livesports808.st` hacia `808ball16.com`). Implementación del algoritmo heurístico de afinidad alfanumérica pura `hasSharedBrandAffinity` y descomposición matemática en tokens de longitud >= 3 caracteres (`extractTokens`), identificando automáticamente redes de espejos con 0% código en duro y sin listas de dominios hardcodeadas. Habilitación del principio canónico de navegación por gesto físico del usuario (`isUserGesture == true`) hacia enlaces web legítimos no catalogados como publicidad en SQLite (`AdBlockDatabase`), permitiendo que el navegador cargue el partido o video directamente en la pantalla principal con paridad 1:1 frente a Google Chrome y manteniendo `BrowserPopupInterceptor.kt` en 107 líneas.
  - **Archivos Objetivo Exactos:**
    - `app-sender/src/main/kotlin/com/omnicast/sender/ui/browser/BrowserPopupInterceptor.kt` (107 líneas)
  - **Agentes Involucrados:** `planner-architect` (`Argon`) ➔ `specialist-stream-extraction` (`flash`) ➔ `surgical-developer` (`flash`) ➔ `qa-oracle-tester` (`flash`) ➔ `sync-documenter` (`flash`) ➔ `git-ota-releaser` (`flash_lite`).

- `[x]` **Hito 8.13 [CH-23] — Desempaquetador Universal de Pasarelas de Redirección y Evasión de Trackers (`app-sender/src/main/kotlin/com/omnicast/sender/ui/browser/WvcShieldSecurityEngine.kt`, `app-sender/src/main/kotlin/com/omnicast/sender/ui/browser/BrowserWebView.kt`):**
  - **Objetivo:** Erradicación del congelamiento de pantalla provocado por pasarelas de afiliados y trackers publicitarios en portales deportivos y de cine (como `rdr2.todotttdk576.cfd/rdr-stats/?_u=...` en `livesports808.st`). Implementación de la función heurística universal `extractCleanTargetFromRedirect` en `WvcShieldSecurityEngine.kt` (270L) para extraer parámetros estándar de destino (`_u`, `url`, `target`, `dest`, `destination`, `redirect`, `link`, `r`, `to`, `goto`), verificando que el destino no sea publicidad en `AdBlocker` ni `AdBlockDatabase`. Conexión directa en `BrowserWebView.kt` (295L) dentro de `shouldOverrideUrlLoading`: al interceptar un secuestro de clic hacia un tracker, si contiene un destino limpio válido, el WebView navega de inmediato a la dirección real (`v?.loadUrl(cleanDest)`) en la misma pestaña, puenteando el tracker publicitario sin tocar el DOM y con 0% código en duro.
  - **Archivos Objetivo Exactos:**
    - `app-sender/src/main/kotlin/com/omnicast/sender/ui/browser/WvcShieldSecurityEngine.kt` (270 líneas)
    - `app-sender/src/main/kotlin/com/omnicast/sender/ui/browser/BrowserWebView.kt` (295 líneas)
  - **Agentes Involucrados:** `planner-architect` (`Argon`) ➔ `specialist-stream-extraction` (`flash`) ➔ `surgical-developer` (`flash`) ➔ `qa-oracle-tester` (`flash`) ➔ `sync-documenter` (`flash`) ➔ `git-ota-releaser` (`flash_lite`).

- `[x]` **Hito 8.14 [CH-24] — Soporte Universal de Cifrado Base64 en Pasarelas de Redirección y Telemetría Enriquecida (`app-sender/src/main/kotlin/com/omnicast/sender/ui/browser/WvcShieldSecurityEngine.kt`):**
  - **Objetivo:** Erradicación del secuestro de clics en portales que ofuscan el destino de sus enlaces mediante esquemas criptográficos o serialización Base64 (`rdr2.../rdr-stats/?_u=aHR0cHM6Ly9...`). Incorporación de la decodificación reactiva `android.util.Base64.decode` como fallback en `extractCleanTargetFromRedirect` dentro de `WvcShieldSecurityEngine.kt` (276L) cuando el parámetro no responde a decodificación URL estándar. Permite al navegador resolver instantáneamente destinos empaquetados en Base64 navegando directo a la web limpia (como `https://www.808ball16.com/...`). Enriquecimiento simultáneo del log de advertencia en `CLICK_HIJACK_BLOCKED` incluyendo la URL completa en el mensaje principal para diagnóstico inmediato con micro-comandos de Linux de 0 tokens y manteniendo el archivo en 276 líneas (<= 300).
  - **Archivos Objetivo Exactos:**
    - `app-sender/src/main/kotlin/com/omnicast/sender/ui/browser/WvcShieldSecurityEngine.kt` (276 líneas)
  - **Agentes Involucrados:** `planner-architect` (`Argon`) ➔ `specialist-stream-extraction` (`flash`) ➔ `surgical-developer` (`flash`) ➔ `qa-oracle-tester` (`flash`) ➔ `sync-documenter` (`flash`) ➔ `git-ota-releaser` (`flash_lite`).

- `[x]` **Hito 8.15 [CH-25] — Desintoxicación Arquitectónica del Receiver (Caja Tonta Pura) y Blindaje Dinámico Anti-CORS en Emisor (`app-receiver/src/main/kotlin/com/omnicast/receiver/ui/components/TvWebResourceInterceptor.kt`, `app-receiver/src/main/kotlin/com/omnicast/receiver/ui/TvReceiverHelper.kt`, `app-sender/src/main/kotlin/com/omnicast/sender/ui/browser/InAppBrowserScreen.kt`, `app-sender/src/main/kotlin/com/omnicast/sender/ui/browser/BrowserUrlHelper.kt`):**
  - **Objetivo:** Erradicación total de listas negras internas (`nativeChromiumHosts`) y dominios hardcodeados (`dropcdn.io`, `okcdn.ru`, `mycdn.me`) en `:app-receiver`, devolviendo a la TV Box su rol sagrado de "caja tonta" pasiva y agnóstica (ADR-016 / HU-018) que inyecta incondicionalmente `Access-Control-Allow-Origin: *` sin vetar servidores. Delegación íntegra de la inteligencia de red en el Emisor (`:app-sender`): inyección dinámica de la directiva `X-Base-Url-Policy: STREAM_HOST` en `BrowserUrlHelper.kt` (297L) cuando la CDN opera en origen cruzado frente al Referer, permitiendo a `TvReceiverHelper.kt` (247L) y `TvWebResourceInterceptor.kt` (139L) resolver el reproductor en Same-Origin absoluto sin colisión CORS. Blindaje estricto en `InAppBrowserScreen.kt` (296L) impidiendo que URLs HTML de páginas web se despachen a la TV Box, con 0% código en duro y manteniendo todos los archivos <= 300 líneas.
  - **Archivos Objetivo Exactos:**
    - `app-receiver/src/main/kotlin/com/omnicast/receiver/ui/components/TvWebResourceInterceptor.kt` (139 líneas)
    - `app-receiver/src/main/kotlin/com/omnicast/receiver/ui/TvReceiverHelper.kt` (247 líneas)
    - `app-sender/src/main/kotlin/com/omnicast/sender/ui/browser/InAppBrowserScreen.kt` (296 líneas)
    - `app-sender/src/main/kotlin/com/omnicast/sender/ui/browser/BrowserUrlHelper.kt` (297 líneas)
  - **Agentes Involucrados:** `planner-architect` (`Argon`) ➔ `specialist-tv-player-pvr` (`flash`) ➔ `surgical-developer` (`flash`) ➔ `qa-oracle-tester` (`flash`) ➔ `sync-documenter` (`flash`) ➔ `git-ota-releaser` (`flash_lite`).

- `[x]` **Hito 8.16 [CH-26] — Saneamiento Dinámico de Referer y Diferenciación de Capítulos de Series (`app-sender/src/main/kotlin/com/omnicast/sender/stream/StreamResumeManager.kt`, `app-sender/src/main/kotlin/com/omnicast/sender/ui/browser/BrowserWebView.kt`, `app-sender/src/main/kotlin/com/omnicast/sender/ui/browser/InAppBrowserScreen.kt`, `app-sender/src/main/kotlin/com/omnicast/sender/ui/SenderMainActivity.kt`):**
  - **Objetivo:** Erradicación del error `HTTP 403 Forbidden` en CDNs deportivas y de streaming (como StreamEast24) provocado por cabeceras `Referer` contaminadas con dominios de portales visitados anteriormente. Implementación de resolución dinámica de `docUrl` y `ref` en `BrowserWebView.kt` (293L) priorizando en tiempo real la URL viva del motor Chromium (`v?.url`) sobre la captura estática del estado. Resolución del incidente de colisión de marcas temporales y títulos entre capítulos de series (como en SoloLatino): extensión de `cleanKey` en `StreamResumeManager.kt` (66L) incorporando de forma agnóstica el slug de la ruta de la página (`pageUrl` con tokens `@episod/@capitul/@season/@temporad`), asegurando que cada episodio tenga una identidad de reanudación independiente e inicie desde `00:00` sin arrastrar el progreso de episodios previos. Blindaje en `InAppBrowserScreen.kt` (298L) y `SenderMainActivity.kt` (295L) reseteando detecciones y títulos de capítulos en navegaciones SPA y propagando la URL de referencia, con 0% código en duro y manteniendo todos los archivos <= 300 líneas.
  - **Archivos Objetivo Exactos:**
    - `app-sender/src/main/kotlin/com/omnicast/sender/stream/StreamResumeManager.kt` (66 líneas)
    - `app-sender/src/main/kotlin/com/omnicast/sender/ui/browser/BrowserWebView.kt` (293 líneas)
    - `app-sender/src/main/kotlin/com/omnicast/sender/ui/browser/InAppBrowserScreen.kt` (298 líneas)
    - `app-sender/src/main/kotlin/com/omnicast/sender/ui/SenderMainActivity.kt` (295 líneas)
  - **Agentes Involucrados:** `planner-architect` (`Argon`) ➔ `specialist-stream-extraction` (`flash`) ➔ `surgical-developer` (`flash`) ➔ `qa-oracle-tester` (`flash`) ➔ `sync-documenter` (`flash`) ➔ `git-ota-releaser` (`flash_lite`).

- `[x]` **Hito 8.17 [CH-27] — Discriminación Estricta de Páginas de Partidos y Firmas Reales HLS (`core/.../StreamCandidateValidator.kt`, `app-sender/.../UniversalHlsExtractorAdapter.kt`, `app-sender/.../BrowserWebView.kt`):**
  - **Objetivo:** Erradicación de la falsa captura de URLs de páginas web de partidos como streams de video (como en StreamEast24 con partidos de NBA/NFL). Extensión de `StreamCandidateValidator.kt` (202L) para descartar incondicionalmente peticiones de Server Components (`_rsc=`) y rutas de navegación de eventos deportivos (`/match/`, `/fixture/`, `/partido/`) sin extensión multimedia directa. Robustecimiento de `UniversalHlsExtractorAdapter.kt` (63L) acotando el clasificador `/live` para que exija obligatoriamente firmas o extensiones HLS legítimas (`.m3u8`, `.ts`, `/hls`, `chunklist`, `/stream/`), impidiendo que rutas web con `/live/` sean despachadas erróneamente a ExoPlayer en la TV Box. Resolución definitiva de la advertencia de concurrencia en `BrowserWebView.kt` (273L) implementando `activeLiveUrl` atómico y thread-safe para alimentar `shouldInterceptRequest` sin invocar `v?.url` en el worker thread de Chromium, con 0% código en duro y manteniendo todos los archivos <= 300 líneas.
  - **Archivos Objetivo Exactos:**
    - `core/domain/src/main/kotlin/com/omnicast/core/domain/usecases/StreamCandidateValidator.kt` (202 líneas)
    - `app-sender/src/main/kotlin/com/omnicast/sender/ui/browser/extractors/UniversalHlsExtractorAdapter.kt` (63 líneas)
    - `app-sender/src/main/kotlin/com/omnicast/sender/ui/browser/BrowserWebView.kt` (273 líneas)
  - **Agentes Involucrados:** `planner-architect` (`Argon`) ➔ `specialist-stream-extraction` (`flash`) ➔ `surgical-developer` (`flash`) ➔ `qa-oracle-tester` (`flash`) ➔ `sync-documenter` (`flash`) ➔ `git-ota-releaser` (`flash_lite`).

---

## 💾 FASE 7 — PVR Inteligente, Touchpad, Navegación y Stealth Chrome (`[x]` — 4 / 4 completados — 100%)

- `[x]` **Hito 7.1 [CH-07] — Auto-Detección Preferencial USB y Purga de Huérfanos en Android TV (`app-receiver/src/main/kotlin/com/omnicast/receiver/directstream/TvStorageSafetyManager.kt`, `app-receiver/src/main/kotlin/com/omnicast/receiver/directstream/PvrStreamRecorder.kt`):**
  - **Objetivo:** Detectar automáticamente memorias USB conectadas con espacio holgado (`>= 1.5 GB`), aplicar la constante `SAFE_RESERVE_USB_BYTES` (500 MB) y purgar en segundo plano archivos temporales huérfanos (`.tmp`, `.part`, 0-byte) con más de 3 minutos de antigüedad, garantizando descargas y grabaciones PVR estables sin desbordar el almacenamiento de la TV Box.
  - **Archivos Objetivo Exactos:**
    - `app-receiver/src/main/kotlin/com/omnicast/receiver/directstream/TvStorageSafetyManager.kt` (290 líneas)
    - `app-receiver/src/main/kotlin/com/omnicast/receiver/directstream/PvrStreamRecorder.kt` (260 líneas)
  - **Agentes Involucrados:** `planner-architect` (`Argon`) ➔ `specialist-tv-player-pvr` (`flash`) ➔ `surgical-developer` (`flash`) ➔ `qa-oracle-tester` (`flash`) ➔ `sync-documenter` (`flash`) ➔ `git-ota-releaser` (`flash_lite`).

- `[x]` **Hito 7.2 [CH-08] — Modo Touchpad / Puntero Virtual en TV vía WebRTC/WebSocket (`core/protocol/src/main/kotlin/com/omnicast/core/protocol/SignalingMessage.kt`, `app-receiver/src/main/kotlin/com/omnicast/receiver/ui/components/TvVirtualPointerOverlay.kt`, `app-receiver/src/main/kotlin/com/omnicast/receiver/ui/components/TvGlobalRecordOverlay.kt`, `app-receiver/src/main/kotlin/com/omnicast/receiver/ui/TvReceiverActivity.kt`):**
  - **Objetivo:** Implementar control remoto tipo touchpad y puntero virtual interactivo en la pantalla del Android TV mediante señalización WebRTC/WebSocket, permitiendo navegación precisa y feedback visual de clics.
  - **Archivos Objetivo Exactos:**
    - `core/protocol/src/main/kotlin/com/omnicast/core/protocol/SignalingMessage.kt` (ampliado con `PointerControl`: 169 líneas)
    - `app-receiver/src/main/kotlin/com/omnicast/receiver/ui/components/TvVirtualPointerOverlay.kt` (nuevo HUD composable de puntero virtual: 93 líneas)
    - `app-receiver/src/main/kotlin/com/omnicast/receiver/ui/components/TvGlobalRecordOverlay.kt` (inyección global no invasiva: 60 líneas)
    - `app-receiver/src/main/kotlin/com/omnicast/receiver/ui/TvReceiverActivity.kt` (recepción síncrona de comandos de puntero: 296 líneas, estrictamente `< 300` líneas)
  - **Agentes Involucrados:** `planner-architect` (`Argon`) ➔ `specialist-tv-player-pvr` (`flash`) ➔ `surgical-developer` (`flash`) ➔ `qa-oracle-tester` (`flash`) ➔ `sync-documenter` (`flash`) ➔ `git-ota-releaser` (`flash_lite`).

- `[x]` **Hito 7.3 [CH-09] — Desacoplamiento de window.open y Navegación Inteligente de Mismo Origen (`app-sender/src/main/assets/sniffer/universal_sniffer.js`, `app-sender/src/main/kotlin/com/omnicast/sender/browser/WvcShieldSecurityEngine.kt`):**
  - **Objetivo:** Eliminar el hook ciego redundante de `window.open` en `universal_sniffer.js` que interfería con la heurística de seguridad de `WvcShieldSecurityEngine.kt`, incorporando trazas de auditoría (`OMNICAST_NAV_SAME_ORIGIN`) para clics legítimos en portales de deportes en vivo (LiveSports808) gobernados por SQLite (`AdBlockDatabase`) y C++20 (`NativeAdBlockEngine`).
  - **Archivos Objetivo Exactos:**
    - `app-sender/src/main/assets/sniffer/universal_sniffer.js` (hook redundante eliminado)
    - `app-sender/src/main/kotlin/com/omnicast/sender/browser/WvcShieldSecurityEngine.kt` (log de trazabilidad: 217 líneas)
  - **Agentes Involucrados:** `planner-architect` (`Argon`) ➔ `specialist-tv-player-pvr` (`flash`) ➔ `surgical-developer` (`flash`) ➔ `qa-oracle-tester` (`flash`) ➔ `sync-documenter` (`flash`) ➔ `git-ota-releaser` (`flash_lite`).

- `[x]` **Hito 7.4 [CH-10] — Modo Stealth Chrome Nativo Anti-Detección de WebView (`app-sender/src/main/kotlin/com/omnicast/sender/browser/WvcShieldSecurityEngine.kt`):**
  - **Objetivo:** Suprimir la cabecera `X-Requested-With` mediante AndroidX WebKit (`setRequestedWithHeaderOriginAllowList` con `emptySet`) e inyectar polyfills JavaScript en `WvcShieldSecurityEngine.kt` para spoofing de `window.chrome` y `navigator.userAgentData` con marcas nativas de Google Chrome, evadiendo defensas de Cloudflare y portales como SoloLatino.
  - **Archivos Objetivo Exactos:**
    - `app-sender/src/main/kotlin/com/omnicast/sender/browser/WvcShieldSecurityEngine.kt` (supresión de header y polyfill Stealth JS: 217 líneas)
  - **Agentes Involucrados:** `planner-architect` (`Argon`) ➔ `specialist-tv-player-pvr` (`flash`) ➔ `surgical-developer` (`flash`) ➔ `qa-oracle-tester` (`flash`) ➔ `sync-documenter` (`flash`) ➔ `git-ota-releaser` (`flash_lite`).

---

## 📺 FASE 6 — Telemetría OSD en Tiempo Real y Experiencia de Control Remoto (Android TV) (`[x]` — 2 / 2 completados — 100%)

- `[x]` **Hito 6.1 [CH-05] — Overlay HUD de Telemetría OSD en Tiempo Real y Activación Remota en Android TV (`app-receiver/src/main/kotlin/com/omnicast/receiver/ui/components/TvTelemetryOverlay.kt`, `app-receiver/src/main/kotlin/com/omnicast/receiver/ui/components/TvGlobalRecordOverlay.kt`, `app-receiver/src/main/kotlin/com/omnicast/receiver/ui/TvRemoteKeyHandler.kt`):**
  - **Objetivo:** Proveer visualización instantánea en pantalla (OSD HUD) de métricas técnicas de streaming, reproducción y hardware en Android TV, permitiendo a los operadores e ingenieros diagnosticar la calidad de video sin interrumpir la reproducción.
  - **Archivos Objetivo Exactos:**
    - `app-receiver/src/main/kotlin/com/omnicast/receiver/ui/components/TvTelemetryOverlay.kt` (nuevo componente HUD composable: 159 líneas)
    - `app-receiver/src/main/kotlin/com/omnicast/receiver/ui/components/TvGlobalRecordOverlay.kt` (inyección global no invasiva: 59 líneas)
    - `app-receiver/src/main/kotlin/com/omnicast/receiver/ui/TvRemoteKeyHandler.kt` (intercepción de atajos en control remoto: teclas `INFO`, `PROG_GREEN` y `1`: 236 líneas)
  - **Agentes Involucrados:** `planner-architect` (`Argon`) ➔ `specialist-tv-player-pvr` (`flash`) ➔ `surgical-developer` (`flash`) ➔ `qa-oracle-tester` (`flash`) ➔ `sync-documenter` (`flash`) ➔ `git-ota-releaser` (`flash_lite`).

- `[x]` **Hito 6.2 [CH-06] — Controles Multimedia y Navegación D-Pad en Reproductor Web y Android TV (`app-receiver/src/main/kotlin/com/omnicast/receiver/ui/TvRemoteKeyHandler.kt`):**
  - **Objetivo:** Mapear nativamente las teclas D-Pad del control remoto (`Center/Enter` para Toggle Play, `Right` para avance rápido +10s con Toast, `Left` para rebobinado -10s con Toast, `Down/Up` para OSD) en estados `DirectStreamPlaying`, `YouTubePlaying` y `WebStreamPlaying`, permitiendo control multimedia fluido en mandos minimalistas de Android TV sin requerir botones dedicados.
  - **Archivos Objetivo Exactos:**
    - `app-receiver/src/main/kotlin/com/omnicast/receiver/ui/TvRemoteKeyHandler.kt` (ampliado a 280 líneas, cumpliendo estrictamente con la regla `< 300` líneas).
  - **Agentes Involucrados:** `planner-architect` (`Argon`) ➔ `specialist-tv-player-pvr` (`flash`) ➔ `surgical-developer` (`flash`) ➔ `qa-oracle-tester` (`flash`) ➔ `sync-documenter` (`flash`) ➔ `git-ota-releaser` (`flash_lite`).

---

## 🛡️ FASE 0 — Base Heredada e Inmutable (`[x]` — 100% Sellada bajo Ley de Intocabilidad)

Todo lo listado en la Fase 0 se encuentra en producción, validado por 78 tests unitarios (100% passing) y **protegido contra refactorizaciones no autorizadas**:

- `[x]` **0.1 Núcleo Hexagonal y Protocolo de Señalización (`:core:domain`, `:core:protocol`):**
  - *Archivos sellados:*
    - `core/domain/src/main/kotlin/com/omnicast/core/domain/usecases/DetectMediaStreamUseCase.kt`
    - `core/domain/src/main/kotlin/com/omnicast/core/domain/usecases/StreamCandidateValidator.kt`
    - `core/domain/src/main/kotlin/com/omnicast/core/domain/usecases/HlsMasterResolver.kt`
    - `core/domain/src/main/kotlin/com/omnicast/core/domain/usecases/StreamManifestProbe.kt`
    - `core/domain/src/main/kotlin/com/omnicast/core/domain/ports/IMediaStreamExtractorPort.kt`
    - `core/protocol/src/main/kotlin/com/omnicast/core/protocol/SignalingMessage.kt`
  - *Cobertura:* 26 tests en `:core:domain:test` + 7 tests en `:core:protocol:test`.

- `[x]` **0.2 Emisor Móvil, Tenaza Dual (Red + DOM), Motor C++20 y Gobernanza SQLite/Supabase (`:app-sender`):**
  - *Archivos sellados:*
    - `app-sender/src/main/kotlin/com/omnicast/sender/ui/browser/InAppBrowserScreen.kt` (ciclo de captura y selector sellado)
    - `app-sender/src/main/kotlin/com/omnicast/sender/ui/browser/BrowserWebView.kt` (foco nativo sin `setOnTouchListener` + aislamiento Cloudflare)
    - `app-sender/src/main/kotlin/com/omnicast/sender/ui/browser/WvcVideoListSheet.kt` (lista plana 1:1)
    - `app-sender/src/main/assets/sniffer/universal_sniffer.js`
    - `app-sender/src/main/cpp/stream_classifier.cpp`, `dean_edwards_unpacker.cpp`, `ad_pattern_matcher.cpp` (C++20 Zero-Allocation)
    - `app-sender/src/main/kotlin/com/omnicast/sender/stream/StreamProfileDatabase.kt` y `CloudStreamRuleSyncManager.kt`
    - `app-sender/src/main/kotlin/com/omnicast/sender/ui/browser/AdBlockDatabase.kt` y `AdBlockRuleSyncManager.kt`
  - *Contratos cubiertos:* `HU-001`, `HU-003`, `HU-004`, `HU-005`, `HU-007`, `HU-008`, `HU-009`, `HU-010`, `HU-011`, `HU-014`, `HU-015`, `HU-016`, `HU-018`, `HU-020`.
  - *Cobertura:* 31 tests en `:app-sender:testDebugUnitTest`.

- `[x]` **0.3 Receptor Android TV, Screen Mirroring Dual y Reproductor Híbrido Anti-CORS (`:app-receiver`):**
  - *Archivos sellados:*
    - `app-receiver/src/main/kotlin/com/omnicast/receiver/ui/components/TvScreens.kt` (Pantalla Completa vs Por Aplicación)
    - `app-receiver/src/main/kotlin/com/omnicast/receiver/webrtc/WebRtcReceiverManager.kt` (escala `1.075f` vertical FIT / `1.0f` horizontal, STOP `< 5 ms`)
    - `app-receiver/src/main/kotlin/com/omnicast/receiver/directstream/ExoPlayerTvManager.kt`
    - `app-receiver/src/main/kotlin/com/omnicast/receiver/ui/components/TvWebStreamScreen.kt`
    - `app-receiver/src/main/kotlin/com/omnicast/receiver/ui/components/TvWebResourceInterceptor.kt` (`Access-Control-Allow-Origin: *`)
    - `app-receiver/src/main/kotlin/com/omnicast/receiver/ui/components/TvYouTubeScreen.kt` (IFrame oficial inmune a 403 Botguard)
  - *Contratos cubiertos:* `HU-002`, `HU-006`, `HU-012`, `HU-013`.

- `[x]` **0.4 Motor PVR Dual (ExoPlayer + Puente Chromium BoringSSL) y Guardián Anti-Brick (`v3.5.59` – `v3.6.08`):**
  - *Archivos sellados:*
    - `app-receiver/src/main/kotlin/com/omnicast/receiver/directstream/PvrStreamRecorder.kt`
    - `app-receiver/src/main/kotlin/com/omnicast/receiver/directstream/PvrHlsMasterParser.kt` (soporte `.ts` y fMP4 `#EXT-X-MAP`)
    - `app-receiver/src/main/kotlin/com/omnicast/receiver/directstream/TvWebPvrBridge.kt` (Variante 1 DropCDN/Fastream)
    - `app-receiver/src/main/kotlin/com/omnicast/receiver/directstream/TvStorageSafetyManager.kt` (reserva sagrada 2.0 GB TV / 500 MB USB y purga de 0 bytes)
    - `app-receiver/src/main/kotlin/com/omnicast/receiver/ui/components/TvRecordExitConfirmDialog.kt`
  - *Contratos cubiertos:* `HU-017`, `HU-019` y entrega `v3.6.08`.
  - *Cobertura:* 14 tests en `:app-receiver:testDebugUnitTest`.

---

## 🧹 FASE 1 — Saneamiento y Reestructuración Quirúrgica (`[x]` — 4 / 4 completadas — 100%)

Actividades acotadas de Cirugía Mínima para resolver la deuda técnica detectada en la auditoría forense sin alterar ninguna lógica funcional:

- `[x]` **Actividad 1.1 `[auto]` — Limpieza Forense de Raíz, Blindaje `.gitignore` y Purga de Directorios Vacíos (`.gitignore`, `phone_dump.txt`, `phone_logcat_*.log`, `log_all_recent.txt`, `screen_v*.png`, `rules/`):**
  - **Objetivo:** Eliminar los >60 MB de volcados y capturas temporales de depuración en la raíz del repositorio (`phone_dump.txt`, `phone_logcat_1204.log`, `phone_logcat_1130.log`, `log_all_recent.txt`, `screen_v17*.png` a `screen_v21.png`), eliminar la carpeta vacía `rules/` y reforzar `.gitignore` para impedir que se rastreen logs o dumps de ADB.
  - **Archivos Objetivo Exactos:**
    - `.gitignore`
    - Raíz del proyecto (`phone_dump.txt`, `phone_logcat_*.log`, `log_all_recent.txt`, `screen_v*.png`, `rules/`)
  - **Agentes Involucrados:** `planner-architect` (`Argon`) ➔ `surgical-developer` (`flash`) ➔ `qa-oracle-tester` (`flash`) ➔ `sync-documenter` (`flash`) ➔ `git-ota-releaser` (`flash_lite`).

- `[x]` **Actividad 1.2 `[auto]` — Cirugía de Límite de 300 Líneas en `VideoCastPlayerActivity.kt` (342L ➔ 159L) (`app-sender/src/main/kotlin/com/omnicast/sender/overlay/VideoCastPlayerActivity.kt`, `app-sender/src/main/kotlin/com/omnicast/sender/overlay/VideoCastPlayerControls.kt` [233L]):**
  - **Objetivo:** Cumplir la Regla 3 de `AGENTS.md` (`<= 300` líneas) extrayendo los subcomponentes visuales de controles del reproductor local a un archivo delegado dentro del mismo paquete `com.omnicast.sender.overlay`, preservando intacta la lógica de reproducción y casteo.
  - **Archivos Objetivo Exactos:**
    - `app-sender/src/main/kotlin/com/omnicast/sender/overlay/VideoCastPlayerActivity.kt` (reducido a 159 líneas)
    - `app-sender/src/main/kotlin/com/omnicast/sender/overlay/VideoCastPlayerControls.kt` (nuevo archivo delegado: 233 líneas)
  - **Agentes Involucrados:** `planner-architect` (`Argon`) ➔ `surgical-developer` (`flash`) ➔ `qa-oracle-tester` (`flash`) ➔ `sync-documenter` (`flash`) ➔ `git-ota-releaser` (`flash_lite`).

- `[x]` **Actividad 1.3 `[auto]` — Cirugía de Límite de 300 Líneas en `GalleryPickerActivity.kt` (326L ➔ 235L) (`app-sender/src/main/kotlin/com/omnicast/sender/overlay/GalleryPickerActivity.kt`, `app-sender/src/main/kotlin/com/omnicast/sender/overlay/GalleryPermissionHelper.kt` [108L]):**
  - **Objetivo:** Cumplir la Regla 3 de `AGENTS.md` (`<= 300` líneas) extrayendo los helpers de permisos y resolución de rutas de galería a un archivo auxiliar en `com.omnicast.sender.overlay`, manteniendo intacto el flujo de galería continua (`HU-001`) y el test `GalleryRouteNavigationTest.kt`.
  - **Archivos Objetivo Exactos:**
    - `app-sender/src/main/kotlin/com/omnicast/sender/overlay/GalleryPickerActivity.kt` (reducido a 235 líneas)
    - `app-sender/src/main/kotlin/com/omnicast/sender/overlay/GalleryPermissionHelper.kt` (nuevo archivo delegado: 108 líneas)
    - `app-sender/src/test/kotlin/com/omnicast/sender/gallery/GalleryRouteNavigationTest.kt` (verificación de no-regresión)
  - **Agentes Involucrados:** `planner-architect` (`Argon`) ➔ `surgical-developer` (`flash`) ➔ `qa-oracle-tester` (`flash`) ➔ `sync-documenter` (`flash`) ➔ `git-ota-releaser` (`flash_lite`).

- `[x]` **Actividad 1.4 `[ver]` — Unificación de Configuración Cloud Supabase y Script Canónico de Release OTA (`core/domain/src/main/kotlin/com/omnicast/core/domain/config/SupabaseCloudConfig.kt`, `app-sender/src/main/kotlin/com/omnicast/sender/stream/CloudStreamRuleSyncManager.kt`, `app-sender/src/main/kotlin/com/omnicast/sender/ui/browser/AdBlockRuleSyncManager.kt`, `app-sender/src/main/kotlin/com/omnicast/sender/log/SenderLogger.kt`, `app-receiver/src/main/kotlin/com/omnicast/receiver/log/ReceiverLogger.kt`, `scratch/publish_release.py`, `scripts/publish_release.py`):**
  - **Objetivo:** Eliminar la duplicación textual de la URL y `SUPABASE_ANON_KEY` en 4 archivos de telemetría/sincronización y alinear `scratch/publish_release.py` con `scripts/publish_release.py` para garantizar despliegues OTA consistentes.
  - **Archivos Objetivo Exactos:**
    - `core/domain/src/main/kotlin/com/omnicast/core/domain/config/SupabaseCloudConfig.kt` (nuevo objeto centralizado en `:core:domain`)
    - `app-sender/src/main/kotlin/com/omnicast/sender/stream/CloudStreamRuleSyncManager.kt`
    - `app-sender/src/main/kotlin/com/omnicast/sender/ui/browser/AdBlockRuleSyncManager.kt`
    - `app-sender/src/main/kotlin/com/omnicast/sender/log/SenderLogger.kt`
    - `app-receiver/src/main/kotlin/com/omnicast/receiver/log/ReceiverLogger.kt`
    - `scratch/publish_release.py` y `scripts/publish_release.py`
  - **Agentes Involucrados:** `planner-architect` (`Argon`) ➔ `specialist-stream-extraction` (`flash`) ➔ `surgical-developer` (`flash`) ➔ `qa-oracle-tester` (`flash`) ➔ `sync-documenter` (`flash`) ➔ `git-ota-releaser` (`flash_lite`).

---

## 🌐 FASE 2 — Evolución de Resiliencia CDN Anti-Scrape y Gobernanza Cloud (`[x]` — 2 / 2 completadas — 100%)

- `[x]` **Actividad 2.1 `[ver]` — Nuevos Adaptadores Modulares "Poner y Sacar" (`IMediaStreamExtractorPort`) y Reglas Dinámicas (`core/domain/src/main/kotlin/com/omnicast/core/domain/usecases/StreamCandidateValidator.kt`, `app-sender/src/main/kotlin/com/omnicast/sender/ui/browser/extractors/LuluStreamExtractorAdapter.kt`, `app-sender/src/main/kotlin/com/omnicast/sender/ui/browser/extractors/StreamwishExtractorAdapter.kt`, `app-sender/src/main/kotlin/com/omnicast/sender/stream/StreamProfileDbSchema.kt`, `app-sender/src/main/kotlin/com/omnicast/sender/stream/StreamProfileDatabase.kt`, `app-sender/src/test/kotlin/com/omnicast/sender/ui/browser/MediaPlatformRegressionTest.kt`):**
  - **Objetivo:** Incorporar soporte para nuevas CDNs de portales de cine y deportes en vivo como adaptadores aislados delegando la validación binaria en `StreamCandidateValidator.isValidCdnStream()`, sin tocar extractores universales ni `InAppBrowserScreen.kt`.
  - **Archivos Objetivo Exactos:**
    - `core/domain/src/main/kotlin/com/omnicast/core/domain/usecases/StreamCandidateValidator.kt`
    - `app-sender/src/main/kotlin/com/omnicast/sender/ui/browser/extractors/LuluStreamExtractorAdapter.kt` (nuevo adaptador modular: 61 líneas)
    - `app-sender/src/main/kotlin/com/omnicast/sender/ui/browser/extractors/StreamwishExtractorAdapter.kt` (nuevo adaptador modular: 63 líneas)
    - `app-sender/src/main/kotlin/com/omnicast/sender/stream/StreamProfileDbSchema.kt` (nuevo esquema SQLite delegado: 79 líneas)
    - `app-sender/src/main/kotlin/com/omnicast/sender/stream/StreamProfileDatabase.kt` (reducido a 247 líneas)
    - `app-sender/src/main/kotlin/com/omnicast/sender/ui/browser/extractors/StreamExtractorRegistry.kt`
    - `app-sender/src/test/kotlin/com/omnicast/sender/ui/browser/MediaPlatformRegressionTest.kt`
    - `core/domain/src/test/kotlin/com/omnicast/core/domain/usecases/StreamCandidateValidatorTest.kt`
  - **Agentes Involucrados:** `planner-architect` (`Argon`) ➔ `specialist-stream-extraction` (`flash`) ➔ `surgical-developer` (`flash`) ➔ `qa-oracle-tester` (`flash`) ➔ `sync-documenter` (`flash`) ➔ `git-ota-releaser` (`flash_lite`).

- `[x]` **Actividad 2.2 `[ver]` — Refuerzo Heurístico de Variantes HLS/DASH y Centinela Anti-Honeypot (`core/domain/src/main/kotlin/com/omnicast/core/domain/usecases/HlsMasterResolver.kt`, `core/domain/src/main/kotlin/com/omnicast/core/domain/usecases/StreamManifestProbe.kt`, `app-receiver/src/test/kotlin/com/omnicast/receiver/ui/components/sentinel/CdnHoneypotSentinelTest.kt`):**
  - **Objetivo:** Ampliar la detección de pistas de audio/subtítulos desacopladas y escudos anti-honeypot en manifiestos HLS compuestos respetando la Ley Anti-Colisión RFC 8216.
  - **Archivos Objetivo Exactos:**
    - `core/domain/src/main/kotlin/com/omnicast/core/domain/usecases/HlsMasterResolver.kt`
    - `core/domain/src/main/kotlin/com/omnicast/core/domain/usecases/StreamManifestProbe.kt`
    - `app-receiver/src/main/kotlin/com/omnicast/receiver/ui/components/sentinel/CdnHoneypotSentinel.kt`
    - `app-receiver/src/test/kotlin/com/omnicast/receiver/ui/components/sentinel/CdnHoneypotSentinelTest.kt`
  - **Agentes Involucrados:** `planner-architect` (`Argon`) ➔ `specialist-stream-extraction` (`flash`) ➔ `surgical-developer` (`flash`) ➔ `qa-oracle-tester` (`flash`) ➔ `sync-documenter` (`flash`) ➔ `git-ota-releaser` (`flash_lite`).

---

## 📼 FASE 3 — Evolución del Motor PVR y Biblioteca de Grabaciones en TV (`[x]` — 2 / 2 completadas — 100%)

- `[x]` **Actividad 3.1 `[auto]` — Gestión de Biblioteca Local/USB de Grabaciones PVR en Android TV (`app-receiver/src/main/kotlin/com/omnicast/receiver/directstream/TvStorageSafetyManager.kt`, `app-receiver/src/main/kotlin/com/omnicast/receiver/ui/components/TvPvrLibrarySummaryBar.kt`, `app-receiver/src/test/kotlin/com/omnicast/receiver/directstream/TvStorageSafetyManagerTest.kt`):**
  - **Objetivo:** Permitir explorar, reproducir y administrar las grabaciones `.ts`/`.mp4` almacenadas en `/Download/PVR_Grabaciones` y `Pelis_descargas` (memoria interna o USB) desde la interfaz de TV respetando el límite `< 300` líneas y la reserva sagrada de `TvStorageSafetyManager.kt`.
  - **Archivos Objetivo Exactos:**
    - `app-receiver/src/main/kotlin/com/omnicast/receiver/directstream/TvStorageSafetyManager.kt` (ampliado con `scanPvrRecordings`, `scanDirectoryForRecordings` y modelo `TvPvrRecordingFile`: 274 líneas)
    - `app-receiver/src/main/kotlin/com/omnicast/receiver/ui/components/TvPvrLibrarySummaryBar.kt` (nuevo composable modular: 161 líneas)
    - `app-receiver/src/test/kotlin/com/omnicast/receiver/directstream/TvStorageSafetyManagerTest.kt` (8 tests unitarios de escaneo, reserva y cálculo de bitrate)
  - **Agentes Involucrados:** `planner-architect` (`Argon`) ➔ `specialist-tv-player-pvr` (`flash`) ➔ `surgical-developer` (`flash`) ➔ `qa-oracle-tester` (`flash`) ➔ `sync-documenter` (`flash`) ➔ `git-ota-releaser` (`flash_lite`).

- `[x]` **Actividad 3.2 `[ver]` — Reanudación Inteligente y Telemetría Enriquecida de Descargas VOD/PVR (`core/protocol/src/main/kotlin/com/omnicast/core/protocol/SignalingMessage.kt`, `app-receiver/src/main/kotlin/com/omnicast/receiver/directstream/PvrResumeMetadataStore.kt`, `app-receiver/src/main/kotlin/com/omnicast/receiver/directstream/PvrStreamRecorder.kt`, `app-receiver/src/test/kotlin/com/omnicast/receiver/directstream/PvrResumeMetadataStoreTest.kt`):**
  - **Objetivo:** Persistir metadatos de progreso de descarga y permitir reanudación segura ante micro-cortes de red en flujos VOD desacoplando la persistencia de metadatos en un archivo independiente.
  - **Archivos Objetivo Exactos:**
    - `core/protocol/src/main/kotlin/com/omnicast/core/protocol/SignalingMessage.kt` (ampliado con `bytesDownloaded`, `totalSizeBytes`, `isResumable` y `errorNotice` en `RecordTelemetry`)
    - `app-receiver/src/main/kotlin/com/omnicast/receiver/directstream/PvrResumeMetadataStore.kt` (nuevo almacén atómico JSON de metadatos de reanudación: 62 líneas)
    - `app-receiver/src/main/kotlin/com/omnicast/receiver/directstream/PvrStreamRecorder.kt` (reducido a 250 líneas al delegar metadatos y reanudación)
    - `app-receiver/src/main/kotlin/com/omnicast/receiver/directstream/PvrRecordProgress.kt` y `TvReceiverHelper.kt` (soporte de bytes y reanudación VOD)
    - `app-receiver/src/test/kotlin/com/omnicast/receiver/directstream/PvrResumeMetadataStoreTest.kt` (3 tests unitarios de persistencia, carga y purga)
    - `core/protocol/src/test/kotlin/com/omnicast/core/protocol/SignalingProtocolTest.kt` (2 tests unitarios de serialización de nuevos campos)
  - **Agentes Involucrados:** `planner-architect` (`Argon`) ➔ `specialist-tv-player-pvr` (`flash`) ➔ `surgical-developer` (`flash`) ➔ `qa-oracle-tester` (`flash`) ➔ `sync-documenter` (`flash`) ➔ `git-ota-releaser` (`flash_lite`).

---

## 🧪 FASE 4 — Cobertura Cuantitativa de Tests y Telemetría Proactiva (`[x]` — 1 / 1 completada — 100%)

- `[x]` **Actividad 4.1 `[auto]` — Ampliación del Oráculo Unitario y Métricas de Regresión por Pilar (`core/domain/build.gradle.kts`, `core/domain/src/test/kotlin/com/omnicast/core/domain/usecases/HlsMasterResolverTest.kt`, `core/protocol/src/test/kotlin/com/omnicast/core/protocol/SignalingProtocolTest.kt`, `app-receiver/src/test/kotlin/com/omnicast/receiver/directstream/TvStorageSafetyManagerTest.kt`):**
  - **Objetivo:** Incorporar reportes cuantitativos de cobertura unitaria con JaCoCo en `:core:domain` (`jacocoTestReport`) y expandir casos de prueba de bordes en `:core:domain` (múltiples etiquetas HLS `#EXT-X-MEDIA`, query parameters codificados y URLs relativas), `:core:protocol` (claves JSON desconocidas, strings vacíos y valores numéricos extremos) y `:app-receiver` (límites de frontera matemática en reserva sagrada 2.0GB/500MB, cálculo seguro de minutos y purga de 0 bytes).
  - **Archivos Objetivo Exactos:**
    - `core/domain/build.gradle.kts` (plugin JaCoCo y tarea `jacocoTestReport` con salida XML y HTML)
    - `core/domain/src/test/kotlin/com/omnicast/core/domain/usecases/HlsMasterResolverTest.kt` (ampliado con 3 tests de borde: 187 líneas)
    - `core/protocol/src/test/kotlin/com/omnicast/core/protocol/SignalingProtocolTest.kt` (ampliado con 3 tests de resiliencia: 268 líneas)
    - `app-receiver/src/test/kotlin/com/omnicast/receiver/directstream/TvStorageSafetyManagerTest.kt` (ampliado con test de límites de frontera: 213 líneas)
  - **Agentes Involucrados:** `planner-architect` (`Argon`) ➔ `qa-oracle-tester` (`flash`) ➔ `surgical-developer` (`flash`) ➔ `sync-documenter` (`flash`) ➔ `git-ota-releaser` (`flash_lite`).
