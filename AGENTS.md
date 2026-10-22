# AGENTS.md — GUÍA DE INSTRUCCIONES PARA IA (Screen Mirroring - proyectarAplicacion)

Este repositorio sigue el estándar abierto AGENTS.md para el desarrollo de la app nativa Android de Screen Mirroring.

---

## 1. NATURALEZA DEL PRODUCTO: SCREEN MIRRORING
- **Dominio Principal:** Transmisión de video/audio en tiempo real, baja latencia y gestión de sockets de red.
- **Tolerancia a Red:** Toda reconexión de socket o pérdida de paquetes debe manejarse de forma silenciosa sin cerrar la aplicación (Principio Fail Fast).
- **Telemetría en TV:** Ante fallos en la Android TV, analizar el log del streaming enviado por red local con timestamps ('HH:mm:ss.SSS').

---

## 2. ESTRATEGIA DE PRUEBAS
- **Unidades Nativas:** Probar codificación/decodificación y formateo de datos en ':core' con JUnit/Mockito.
- **Pruebas de Red:** Verificar manejo de excepciones de Timeout y desconexión en ':app-sender' y ':app-receiver'.
- **Ejecución del Oráculo:** Todo cambio debe validar la compilación y tests ejecutando:
  .\gradlew.bat clean build

---

## 3. BUENAS PRÁCTICAS Y ESTÁNDAR DE CÓDIGO
- **Límite de Tamaño:** Máximo 300 líneas por archivo. Separar estados, modelos y composables en archivos independientes.
- **Diseño de Código:** Aplicar 'Fail Fast' (evitar 'if-else' anidados). Sin 'Magic Numbers' o puertos de red 'hardcoded'.
- **Comentarios:** Solo explicar el 'POR QUÉ' de un parche o limitación técnica del hardware. Prohibido narrar lo que hace el código.
- **Catálogo de Versiones:** Declarar dependencias únicamente en 'gradle/libs.versions.toml'.

---

## 4. CONTROL DE VERSIONES (GIT)
- **Mensajes de Commit:** Usar Convención Estándar: 'feat(modulo): descripción', 'fix(modulo): descripción'.
- **Ámbito Acotado:** Cada tarea debe modificar únicamente los archivos necesarios sin refactorizaciones ajenas no solicitadas.

---

## 5. DOCUMENTACIÓN VIVA Y CIERRE
- Mantener 'doc/PROJECT_DASHBOARD.md' actualizado al finalizar cada sesión.
- Si se modifica la estructura del protocolo de red o streaming, registrar el cambio en 'doc/adr/ADR-xxx.md'.
- Toda funcionalidad aprobada por el usuario debe registrarse como contrato inamovible en 'doc/historias-usuario/HU-xxx.md'.
- **Versionado y Despliegue Obligatorio:** Toda nueva versión debe actualizar 'versionCode' y 'versionName' en los archivos 'build.gradle.kts', compilarse, copiarse a la carpeta 'releases/' (conservando siempre las versiones generadas de sender y receiver) e instalarse de inmediato en el dispositivo móvil vía ADB. En GitHub Releases únicamente debe publicarse y residir la versión final actual, eliminando automáticamente de GitHub las versiones previas para no saturar espacio remoto.

---

## 6. ARQUITECTURA HEXAGONAL Y PRINCIPIO "PONER Y SACAR" (LEY INVIOLABLE)
- **Cero Código Spaghetti:** Queda terminantemente PROHIBIDO encadenar 'if-else' o modificar métodos de análisis globales para soportar un sitio web específico.
- **Extractores Aislados (Plugins):** Todo soporte para una web o servicio específico debe implementarse en un archivo independiente como adaptador del puerto 'IMediaStreamExtractorPort'.
- **Principio "Poner y Sacar":** Agregar o retirar un extractor nunca debe modificar ni desestabilizar los extractores universales (HLS, MP4, DASH).
- **Aislamiento de los 4 Pilares:** 
  1. Bloqueador de Anuncios (C++ / Red).
  2. Proyección de Fotos (WebSocket liviano / reposo de video).
  3. Reproductor / Casteo (ExoPlayer y WebRTC con exclusión mutua estricta).
  4. Detección de Streams (Núcleo Hexagonal puro).
  Ningún pilar puede invadir, depender o alterar el ciclo de vida de los otros tres.
- **Prohibición en WebViews:** PROHIBIDO usar bucles 'MutationObserver' o 'querySelectorAll' masivos que saturen el hilo de JavaScript. Toda neutralización de trampas debe ser reactiva al evento del usuario.

---

## 7. LEYES TÉCNICAS INVIOLABLES DE HARDWARE (PROHIBICIÓN ESTRICTA DE REGRESIONES)
1. **Prohibido `changeCaptureFormat()` o `DisplayListener` en caliente durante Screen Mirroring:**
   El encoder por hardware H.264 (MediaCodec) y el `VirtualDisplay` de Android 14 no admiten redimensionamiento en caliente sin reinicializar el pipeline WebRTC. Llamar a `changeCaptureFormat` al rotar colapsa el capturador y desconecta la TV Box. La rotación de pantalla fluye nativamente dentro del `VirtualDisplay` sin alterar dimensiones negociadas en el SDP.
2. **Tolerancia a Desconexión Temporal ICE (`IceConnectionState.DISCONNECTED`):**
   `DISCONNECTED` es un estado transitorio de red; queda TERMINANTEMENTE PROHIBIDO cerrar la sesión WebRTC o expulsar la pantalla de TV ante este estado. Únicamente `FAILED` o `CLOSED` son causales de cierre.
3. **Intocabilidad de Foco en `BrowserWebView`:**
   PROHIBIDO asignar `setOnTouchListener` con `requestFocus()` al `WebView`. Chromium y el `InputMethodManager` de Android gestionan el foco y la posición del cursor de forma nativa; manipularlo en eventos táctiles reinicia la conexión IME en el índice 0 y genera escritura inversa o bloqueo del teclado virtual.
4. **Protección de Elementos de Entrada y Dominios Oficiales en `AdBlocker`:**
   El script de limpieza y neutralización de capas NUNCA debe ejecutarse en plataformas oficiales (`dailymotion.com`, `youtube.com`, buscadores) y NUNCA debe remover elementos que contengan formularios (`form`, `input`, `textarea`, `select`).
5. **Arquitectura Híbrida Dual y Resiliencia CDN con `TvWebStreamScreen` (HTML5/Chromium):**
   Queda terminantemente PROHIBIDO eliminar o forzar exclusivamente `ExoPlayer` para flujos protegidos por tokens efímeros, firmas criptográficas o CDNs anti-scrape (DropCDN, Fastream, etc.). Los sockets de red de Java en ExoPlayer son rechazados con `HTTP 403 Forbidden` por inspección TLS/Fingerprint de dichas CDNs. La TV Box DEBE mantener siempre activo el receptor web HTML5 nativo (`TvWebStreamScreen` con `Hls.js`) para flujos con tokens firmados (`hasSignedSecurityToken`) y como fallback reactivo automático ante cualquier error 403 en ExoPlayer (ADR-007, HU-012).
6. **Blindaje Inviolable de Plataformas Oficiales (YouTube y Dailymotion):**
   - **YouTube:** PROHIBIDO filtrar, descartar o ignorar URLs con ID de YouTube (`extractYouTubeId != null`) en `registerStream()`, `checkPlatformUrls()`, `openOptionsSheet()` o `isRealMedia()`. La detección y generación de opciones de calidad de YouTube (`createYouTubeQualityOptions`) debe ejecutarse de forma limpia y directa hacia `TvYouTubeScreen`.
   - **Dailymotion y Recursos de Interfaz:** PROHIBIDO bloquear dominios de CDN/UI de Dailymotion (`dmcdn.net`, `static1.dmcdn.net`) o YouTube (`ytimg.com`, `googlevideo.com`) en `NativeAdBlockEngine` o `AdBlocker`. Dichos dominios deben residir permanentemente en `AdBlockWhitelistManager.DEFAULT_WHITELIST` y en `AdBlocker.KNOWN_MEDIA_HOSTS` para garantizar que la caja de texto del buscador y los controles interactivos de Dailymotion NUNCA desaparezcan ni se rompan.
7. **Blindaje Inviolable de Screen Mirroring Vertical y Horizontal (HU-013, v3.4.18) [BLOQUEANTE]:**
   - **Prioridad Absoluta de la Imagen (Horizontal vs. Vertical):**
     En `WebRtcReceiverManager.kt`, la propiedad reactiva `isLandscape` unifica obligatoriamente:
     1. Indicador del teléfono (`isPhoneLandscape`).
     2. Decodificación real de frames de video apaisados (`effectiveFrameW > effectiveFrameH`).
     3. Modos forzados de llenar pantalla (`FILL`, `16:9`, `EXTEND_16_9`).
     Si la imagen es horizontal (video de partido/película o teléfono horizontal), `isVertical` es estrictamente `false`.
   - **Área de Renderizado y Zoom Totalmente Independientes:**
     - **En Horizontal:** La TV utiliza incondicionalmente `Modifier.fillMaxSize()`, `MATCH_PARENT, MATCH_PARENT` en `SurfaceViewRenderer` y base `SCALE_ASPECT_FILL`. El zoom aplica sobre el área horizontal completa de 16:9 centrado en toda la pantalla del televisor sin colapsar a un tercio ni desplazarse a la izquierda.
     - **En Vertical:** Se mantiene centrado ocupando el 100% de la altura física de la TV (de borde a borde sin franjas negras arriba/abajo) con recorte fino de 1.075x para barras de sistema (hora, batería, navegación), y el zoom opera únicamente sobre el área vertical. Queda TERMINANTEMENTE PROHIBIDO usar `WRAP_CONTENT` en `SurfaceViewRenderer` o aplicar factores multiplicadores de distorsión lateral.
   - **Arquitectura de Renderizado en TV:** En `TvScreens.kt`, `AndroidView` debe mantener incondicionalmente `Modifier.fillMaxSize()` y `MATCH_PARENT, MATCH_PARENT` para que `SurfaceView` ocupe todo el televisor y sea el motor hardware WebRTC el que controle el centrado y la escala con `setEnableHardwareScaler(false)`. Toda conmutación desde control remoto debe ser estrictamente idempotente.

---

## 8. PROTOCOLO PRE-VUELO INVIOLABLE CONTRA REGRESIONES (CHECKLIST OBLIGATORIO)
Queda terminantemente PROHIBIDO compilar o proponer una versión sin auditar previamente este checklist contra regresiones:
1. **Preservación Total de Cabeceras en Fallback (Anti-403 / Cloudflare):**
   Todo fallback o cambio de estado hacia `ReceiverUiState.WebStreamPlaying` DEBE transferir obligatoriamente el mapa de cabeceras (`headers = lastDirectHeaders`). Queda prohibido instanciar `WebStreamPlaying` con cabeceras vacías (`emptyMap()`), ya que despoja a Chromium de las cookies de sesión y del `Referer`, provocando rechazo inmediato 403 en CDNs protegidas (Cloudflare, Repelis).
2. **Soporte HLS sin Extensión Rígida (Spider-Man / PelisGO):**
   En `TvWebStreamScreen`, toda URL que no sea un archivo de video directo (`.mp4`, `.webm`) DEBE pasar por `initHls()` (`Hls.js`). Prohibido suponer que HLS solo termina en `.m3u8`; existen manifiestos con terminación `.txt` o parámetros query que requieren `Hls.js` y fallan si se asignan directamente a `video.src`.
3. **Escalado Horizontal y Zoom en Mirroring (No-Colapso a 1/3 ni a 1/16):**
   En modo horizontal (`isLandscape == true`), `WebRtcReceiverManager` DEBE operar siempre con base `SCALE_ASPECT_FILL` y `TvScreens.kt` con `MATCH_PARENT, MATCH_PARENT`. Queda PROHIBIDO usar `WRAP_CONTENT`, resetear a `SCALE_ASPECT_FIT` o aplicar `aspectRatio` vertical de 9:16 durante el ciclo de zoom en horizontal.
4. **Prohibición de Inventar Vías Alternativas ante Fallos de un Pilar:**
   Si un flujo falla en el reproductor contratado (ExoPlayer o HTML5), queda TERMINANTEMENTE PROHIBIDO desviar la arquitectura hacia proxies o mecanismos improvisados no solicitados. Se debe inspeccionar el log local de la TV (`/logs`), identificar el fallo exacto en ese reproductor y corregirlo en su propio pilar sin romper los demás.


