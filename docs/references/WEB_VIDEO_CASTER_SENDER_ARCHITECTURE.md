# 🌐 ARQUITECTURA TÉCNICA: WEB VIDEO CASTER SENDER

Documento de referencia técnica extraído del análisis de código y assets de **Web Video Caster (v6.0.2)** (`com.instantbits.cast.webvideo`).

---

## 1. MECANISMOS DE DETECCIÓN E INTERCEPCIÓN MULTIMEDIA (`ibfunctions.js`)

Web Video Caster no depende exclusivamente de escuchar peticiones de red en `WebViewClient.shouldInterceptRequest`. Implementa un motor híbrido de doble capa:

### A. Capa JavaScript (`assets/ibfunctions.js`):
1. **Monitorización del DOM en tiempo real:**
   - Utiliza `MutationObserver` (`DomMonitor`) observando `{ childList: true, subtree: true }` sobre `document.body`.
   - Escucha dinámicamente la adición de etiquetas `<video>`, `<source>` y `<track>`.
2. **Detección de Reproductores Web Populares:**
   - **JWPlayer:** Extrae listas de reproducción directamente invocando `jwplayer().getPlaylist()`.
   - **Flowplayer:** Lee `flowplayer().video.src` y el array de subtítulos `flowplayer().video.subtitles`.
   - **Clappr:** Inspecciona `Clappr.PlayerInfo._players` para recuperar la URL fuente y el MIME type.
   - **Facebook Video:** Consulta elementos `[data-store]` y analiza el JSON embebido (`dataset.store`).
   - **VK / OK.ru (OneVideoPlayer):** Extrae `metadata.hlsManifestUrl` y calidades MP4 alternativas.
3. **Extracción de Subtítulos y Metadatos:**
   - Lee etiquetas `<track>` de tipo subtítulo con `label`, `srclang` y `src`.
   - Lee etiquetas `<meta>` (`og:title`, `og:image`, `twitter:title`, `twitter:image`) para mostrar póster y título en la notificación y en la TV.
4. **Intercepción de Streams por WebSocket (`assets/wss.js`):**
   - Sobrescribe `window.WebSocket` para interceptar enlaces de video y fuentes que se transmiten mediante sockets en vivo.
5. **Canal de Comunicación Indetectable (Bridge JS -> Android):**
   - En lugar de exponer un `@JavascriptInterface` que los sitios de streaming detectan y bloquean, utiliza `console.log(JSON.stringify(obj))` con una clave especial (`ibMessage: true`).
   - Android lo intercepta en `WebChromeClient.onConsoleMessage(consoleMessage)`.

---

## 2. MANEJO DE PUBLICIDAD Y POPUPS

1. **Aislamiento de Ventanas Emergentes:**
   - Permite que `window.open` retorne una ventana secundaria en lugar de `null` (si retorna `null`, los reproductores pirata redirigen la pestaña principal mediante `window.location.href`).
   - En Android, `WebChromeClient.onCreateWindow` captura el `WebView` secundario y lo aísla en un contenedor flotante con opción de cierre rápido.
2. **Salto de Anuncios Web (`ibSkipAds`):**
   - Si detecta un video publicitario previo al contenido principal, ajusta `video.currentTime = video.duration - 0.1` y ejecuta `video.play()` para forzar el evento de finalización del anuncio.

---

## 3. SOLUCIÓN AL THROTTLING DE YOUTUBE (`potoken_mint.js`)

- Integra `bgutils.bundle.js` y `potoken_mint.js` para resolver el desafío BotGuard de Google dentro de un WebView real en el móvil.
- Genera tokens criptográficos de integridad:
  - `sessionPoToken`: Enviado en la solicitud `/player` de YouTube.
  - `contentPoToken`: Adjuntado como parámetro `&pot=` en las URLs de streaming de los servidores CDN de YouTube para evitar la limitación de ancho de banda (throttling) y bloqueos `403`.

---

## 4. SOPORTE DE RECEPTORES UNIVERSALES (`assets/web-receiver`)

Web Video Caster incluye un cliente web completo (`index.html`, `video.js`, `videojs-http-streaming`, `app.min.js`) en sus assets para poder transmitir a navegadores web de Smart TVs (LG webOS, Samsung Tizen) sin requerir instalación de APK.
