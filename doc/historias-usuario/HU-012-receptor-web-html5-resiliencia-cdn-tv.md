# HU-012: Receptor Web HTML5 (Chromium / Hls.js) y Resiliencia ante CDNs Protegidas

**Épica:** Reproducción y Resiliencia en Android TV / TV Box  
**Versión de Introducción:** v3.32.94 (Estabilizado con Zoom en v3.32.95)  
**Estado:** Inamovible / Aprobado por el Usuario  

---

## 1. Descripción de la Historia

**Como** usuario de OmniCast en Android TV / TV Box,  
**Quiero** reproducir películas y streams protegidos por firmas criptográficas y sistemas anti-scrape (DropCDN, Fastream, Cloudflare) directamente en mi televisor,  
**Para** disfrutar de contenido en alta calidad sin sufrir bloqueos de servidor (`HTTP 403 Forbidden`) y sin obligar a que mi teléfono móvil mantenga la pantalla activa ni actúe como un servidor proxy sobrecargado.

---

## 2. Criterios de Aceptación (Contrato Inamovible)

1. **Receptor Web Nativo Chromium:**
   - La TV Box debe disponer del componente `TvWebStreamScreen.kt` basado en `Android System WebView` con la librería industrial `Hls.js`.
   - Todas las peticiones a la CDN deben realizarse a través del stack de red oficial de Chromium con las cabeceras `Referer`, `Origin` y `User-Agent` del navegador para que la CDN las reconozca como tráfico web legítimo.

2. **Arquitectura Híbrida Dual de Reproducción:**
   - **ExoPlayer:** Motor primario para contenidos estándar (MP4, IPTV, HLS abierto, YouTube, streams locales).
   - **TvWebStreamScreen:** Motor especializado para flujos con tokens firmados (`hasSignedSecurityToken`) y **fallback reactivo** obligatorio si ExoPlayer recibe `HTTP 403 Forbidden`.

3. **Paridad de Control Remoto y Zoom TV:**
   - El reproductor HTML5 debe admitir todos los controles del mando a distancia (Play/Pausa con D-Pad Center, saltos ±10s con D-Pad Left/Right y tecla MENU para ciclar modos de Zoom).
   - Debe responder a los mensajes del protocolo `SignalingMessage.PlayControl` y `SignalingMessage.SetDisplayMode` enviados desde la app móvil.

4. **Regla de No Regresión:**
   - Queda estrictamente prohibido desmantelar o descartar `TvWebStreamScreen` en favor de llamadas puras a ExoPlayer, o eliminar la verificación heurística `hasSignedSecurityToken`.

5. **Aislamiento Hexagonal por Proveedor Evaluado (Principio "Poner y Sacar"):**
   - El componente `TvWebStreamScreen.kt` es **100% agnóstico y universal**: NUNCA hardcodea dominios, orígenes ni cabeceras específicas de ningún sitio web. Recibe dinámicamente el mapa `headers` enviado por el emisor.
   - Toda resolución de orígenes o cabeceras especializadas (como `Referer`, `Origin` o tokens dinámicos) debe residir exclusivamente en el emisor dentro de `MediaPlatformResolver.kt`, evaluando de forma aislada y previa los dominios específicos que lo requieran:
     - **Fastream / DropCDN:** `fastream.to`, `dropcdn.io`, `dr0pstream.com`.
     - **LuluStream / TNMR:** `tnmr.org`, `luluvdo.com`, `lulustream.com`.
   - Cualquier otra página o flujo genérico utiliza su `pageUrl` legítimo como Referer o el origen natural del manifiesto (`streamOrigin`), sin alterar el comportamiento de las plataformas oficiales ni de sitios estándar.

6. **Estándar CORS de Navegador y Compatibilidad con Comodín (`*`):**
   - Las CDNs de distribución de video públicas y oficiales responden con el encabezado HTTP estándar `Access-Control-Allow-Origin: *` (asterisco / comodín).
   - Según el estándar W3C / WHATWG, activar `xhr.withCredentials = true` en Chromium ante una respuesta con `*` provoca el rechazo y aborto inmediato de la conexión por seguridad del navegador (`The value of 'Access-Control-Allow-Origin' must not be the wildcard '*' when credentials mode is 'include'`).
   - Por tanto, `TvWebStreamScreen` opera bajo el estándar limpio sin `withCredentials` en `Hls.js`, permitiendo que toda CDN que emita `*` sea consumida de forma transparente sin bloqueos de seguridad.

7. **Interceptor Nativo Anti-CORS (`TvWebResourceInterceptor.kt` / v3.4.55):**
   - `TvWebStreamScreen` delega sus peticiones de red en `WebViewClient.shouldInterceptRequest` hacia `TvWebResourceInterceptor.kt`.
   - Toda petición de preflight `OPTIONS` es respondida localmente en 0ms con código HTTP 200 y cabeceras de autorización total (`Access-Control-Allow-Origin: *`, `Access-Control-Allow-Methods: *`, `Access-Control-Allow-Headers: *`).
   - Cada solicitud a listas `.m3u8` y fragmentos `.ts`, `.m4s` y subtítulos `.vtt` es descargada directamente vía conexión socket Java nativa con las cabeceras `User-Agent` y `Referer` legítimas, e inyectada con `Access-Control-Allow-Origin: *` hacia Chromium. Esto garantiza inmunidad total e incondicional contra bloqueos de CORS en cualquier CDN.

8. **Control Dual de Subtítulos y Calidad HLS en TV Box y Móvil (v3.4.56):**
   - **Control Remoto de TV:** Tecla D-Pad Arriba para alternar subtítulos (Español / Desactivados) y D-Pad Abajo para ciclar niveles de calidad HLS (Automática, 1080p, 720p, etc.). La TV Box emite un badge flotante semi-transparente de confirmación durante 2.5s ("💬 Subtítulos: Español", "📺 Calidad: 1080p").
   - **Control Remoto en Teléfono:** Botones dedicados de Subtítulos e Ícono HD en la barra inferior expandida (`RemoteExpandedControls.kt`), enviando comandos `TOGGLE_SUBTITLES` y `CYCLE_QUALITY` en tiempo real sin requerir mando a distancia.

9. **Resolución Síncrona Fastream / DropCDN con User-Agent de Escritorio (v3.4.57):**
   - Para servidores de video Fastream / DropCDN (`fastream.to`, `dropcdn.io`, `dr0pstream.com`), las solicitudes de manifiesto y fragmentos se envían con identidad de Escritorio Windows (`UA_FASTREAM`) y cabecera `Referer: https://fastream.to/embed-$id.html`, resolviendo de raíz el rechazo `HTTP 403 Forbidden` que las CDNs de Fastream aplican a navegadores móviles.
