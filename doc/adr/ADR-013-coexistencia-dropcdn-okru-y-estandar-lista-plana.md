# ADR-013: Coexistencia DropCDN/OK.ru, Estándar WVC de Lista Plana y Blindaje de Cabeceras en TV

## Estado
Aprobado (v3.5.21)

## Contexto y Causa Raíz
En la versión v3.5.4 a v3.5.7, con el objetivo de brindar soporte de selección de calidades múltiples (1080p, 720p, 480p, 360p) y DASH (`.mpd`) para `ok.ru`, se introdujeron modificaciones que impactaron globalmente en el sistema de captura y presentación de medios:
1. **Agrupación en Tarjetas con Píldoras (`WvcVideoCardItem.kt`):** Se sustituyó la lista plana 1:1 tradicional por una tarjeta única con píldoras (`[Adaptive]`, `[1080p]`, `[720p]`), colapsando las variantes legítimas del tráfico (`master.m3u8`, `index-v1-a1.m3u8`, `iframes-v1-a1.m3u8`) e inventando una opción "720p" sintética (`_l/index-v1-a1.m3u8`).
2. **Corrupción de URLs por Derivación `.urlset`:** Se generalizó la función `BrowserUrlHelper.deriveMasterPlaylistUrl` (diseñada para Wowza/Nimble en Fastream) incluyendo por error al dominio `dropcdn.io`. DropCDN no soporta sintaxis urlset y responde 404/403.
3. **Inexistencia de Subflujo 720p en DropCDN:** PelisPlus/DropCDN (`ds4.dropcdn.io`) aloja videos exclusivamente en resolución nativa de captura (ej. 1920x800). La URL `_l/index-v1-a1.m3u8` no existe físicamente en el servidor CDN.
4. **Despojo de Cabeceras en TV Box (`v3.5.19`):** Al proyectar PelisPlus, `TvReceiverHelper.resolveBaseUrl` sobreescribía `baseUrl = streamHostOrigin` (`https://ds4.dropcdn.io/`), eliminando el `Referer` legítimo (`https://dr0pstream.com/e/<id>`). Además, `TvWebStreamScreen.kt` no inyectaba `settings.userAgentString = headers["User-Agent"]`. El firewall de DropCDN detectó la petición huérfana de Dalvik/WebView y respondió con `HTTP 403 Forbidden` (`manifestLoadError code=403`).

## Decisión de Arquitectura: Los 4 Pilares de Coexistencia

### 1. Retorno al Estándar 1:1 de Lista Plana (Web Video Caster)
Se elimina definitivamente `WvcVideoCardItem.kt` y se restaura la lista plana en `WvcVideoListSheet.kt`.
- Cada flujo detectado en la red se muestra como una opción individual y transparente con su URL, resolución y bitrate real.
- Se eliminan las píldoras de calidad inventadas que intentaban redirigir hacia subflujos inexistentes.

### 2. Aislamiento Estricto de OK.ru en el Núcleo Hexagonal
La síntesis de calidades progresivas (1080p, 720p, 480p, 360p) reside **exclusivamente** dentro de `OkRuExtractor` y `MediaPlatformResolver.kt`.
- El atributo `groupKey = mediaUrl` se asigna única y exclusivamente si el host es de `ok.ru`.
- DropCDN, Fastream, PelisGO, Spider-Man y cualquier otro portal quedan 100% aislados de esta lógica.

### 3. Descontaminación de DropCDN en Manipulación de URLs
Se retira `dropcdn.io` de `BrowserUrlHelper.deriveMasterPlaylistUrl`.
- Las URLs de DropCDN se transmiten íntegras y puras, tal como son emitidas por el reproductor web.

### 4. Blindaje Inviolable de Cabeceras en el TV Receiver (`TvWebStreamScreen`)
- **Preservación del `Referer` como Base URL:** `TvReceiverHelper.resolveBaseUrl()` respeta prioritariamente `headers["Referer"]` (`https://dr0pstream.com/e/<id>`). NUNCA se sobreescribe con el origen del stream si existe un Referer explícito de embed.
- **Inyección de User-Agent Móvil:** `TvWebStreamScreen.kt` aplica `settings.userAgentString = headers["User-Agent"] ?: UA_MOBILE`. DropCDN valida la firma y autoriza la entrega del manifiesto y fragmentos sin error 403.

## Consecuencias
- **Coexistencia Plena:** PelisPlus (`pelisplus.hair` / DropCDN) reproduce fluidamente en la TV Box vía `TvWebStreamScreen` con `Hls.js`, y OK.ru reproduce tanto en DASH (`.mpd`) por hardware como en calidades progresivas.
- **Claridad de Código:** Al eliminar tarjetas complejas de agrupación, el código de UI es más liviano, rápido y fácil de mantener.
- **Respeto a Reglas Previas:** Ningún otro portal (PelisGO, Canela.tv, 808ball, YouTube, Dailymotion) sufrió alteraciones.
