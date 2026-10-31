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
- **Ejecución del Oráculo (BLOQUEANTE):** Queda terminantemente PROHIBIDO compilar una versión de release (`assembleRelease`) o publicar en GitHub Releases sin haber ejecutado previamente `./gradlew :core:domain:test` y verificado que el 100% de los tests pasen exitosamente con 0 fallos.

---

## 3. BUENAS PRÁCTICAS Y ESTÁNDAR DE CÓDIGO
- **Límite de Tamaño:** Máximo 300 líneas por archivo. Separar estados, modelos y composables en archivos independientes.
- **Diseño de Código:** Aplicar 'Fail Fast' (evitar 'if-else' anidados). Sin 'Magic Numbers' o puertos de red 'hardcoded'.
- **Comentarios:** Solo explicar el 'POR QUÉ' de un parche o limitación técnica del hardware. Prohibido narrar lo que hace el código.
- **Catálogo de Versiones:** Declarar dependencias únicamente en 'gradle/libs.versions.toml'.
- **Ley de Intocabilidad de lo Probado:** Toda funcionalidad, pantalla, extractor, flujo de red, cálculo de escala o reproductor validado y aprobado por el usuario queda **SELLADO E INMUTABLE**. Queda PROHIBIDO refactorizarlo, reescribirlo, simplificarlo o "generalizarlo" bajo pretexto de elegancia o modularidad. Si funciona, no se toca.
- **Separación entre Consulta del Usuario y Orden de Modificación:** Ante una consulta teórica, pregunta conceptual ("¿qué pasó con esto?", "¿cómo funciona?", "¿por qué?") o análisis solicitado por el usuario, la IA responderá ÚNICAMENTE con diagnóstico, datos y explicación técnica. Queda TERMINANTEMENTE PROHIBIDO modificar archivos, ejecutar refactorizaciones o crear commits como reacción a una consulta. Toda modificación de código requiere una instrucción explícita del usuario o la aprobación previa de una propuesta.

---

## 4. CONTROL DE VERSIONES (GIT)
- **Mensajes de Commit:** Usar Convención Estándar: 'feat(modulo): descripción', 'fix(modulo): descripción'.
- **Ámbito Acotado y Cero Efecto Mariposa:** Cada tarea debe modificar únicamente los archivos estrictamente necesarios sin refactorizaciones ajenas no solicitadas. Prohibido alterar utilitarios globales compartidos para solucionar la anomalía de una web individual.

---

## 5. DOCUMENTACIÓN VIVA Y CIERRE
- Mantener 'doc/PROJECT_DASHBOARD.md' actualizado al finalizar cada sesión.
- Si se modifica la estructura del protocolo de red o streaming, registrar el cambio en 'doc/adr/ADR-xxx.md'.
- Toda funcionalidad aprobada por el usuario debe registrarse como contrato inamovible en 'doc/historias-usuario/HU-xxx.md'.
- **Versionado, Despliegue y Publicación OTA Obligatoria (LEY INVIOLABLE):**
  Toda nueva versión generada en cualquier sesión o chat DEBE cumplir sin excepción el siguiente protocolo completo:
  1. Actualizar `versionCode` y `versionName` en `app-sender/build.gradle.kts`, `app-receiver/build.gradle.kts` y sincronizar `version.json`.
  2. Compilar en release (`./gradlew assembleRelease`) y copiar los APKs generados a la carpeta `releases/` (preservando siempre el histórico local de versiones de sender y receiver).
  3. Instalar de inmediato el APK del Sender en el dispositivo móvil conectado vía ADB.
  4. **Subida Obligatoria a GitHub Releases para Actualización OTA del Receiver:** Publicar incondicionalmente la versión en GitHub Releases ejecutando `python3 scratch/publish_release.py`. Dicho script elimina las versiones previas remotas para no saturar espacio y sube obligatoriamente como assets: `version.json` (imprescindible para que `TvUpdateManager` detecte la actualización en la TV Box), `omnicast-sender-vX.X.X.apk` y `omnicast-receiver-vX.X.X.apk`. Queda TERMINANTEMENTE PROHIBIDO cerrar una tarea con cambio de versión sin haber completado la subida a GitHub Releases para que el Receiver pueda descargarlo.

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
- **Catálogo Técnico de Variantes (Obligatorio):** Todo análisis de compatibilidad, selección de reproductor y gestión de cabeceras DEBE consultar y respetar el catálogo técnico documentado en `doc/referencias/CATALOGO_VARIANTES_STREAMING.md` antes de proponer cambios en red o reproductores.


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
7. **Blindaje Inviolable de Adaptación de Imagen en Screen Mirroring y Comando STOP (HU-013, v3.4.21) [BLOQUEANTE]:**
   - **Detección Pura de Orientación por Análisis de Imagen y Dispositivo:**
     En `WebRtcReceiverManager.kt`, `isLandscape` se determina EXCLUSIVAMENTE mediante:
     1. Los frames decodificados reales (`effectiveFrameW > effectiveFrameH`).
     2. El sensor de orientación física del teléfono (`isPhoneLandscape`).
     Queda TERMINANTEMENTE PROHIBIDO que la selección de un modo (como `FILL`) adultere `isLandscape` a `true` cuando la imagen recibida es vertical, ya que provocaría una explosión de zoom de 4x destructiva.
   - **Comportamiento Matemático según Tipo de Pantalla/Imagen:**
     1. **Imagen Vertical (Celular Erguido / Apps / Reels):**
        - La TV opera incondicionalmente en `SCALE_ASPECT_FIT` centrada horizontalmente.
        - **Modo FIT (Pantalla Completa sin Barras de Sistema):** Aplica factor base estricto de **1.075f**, ocupando el 100% íntegro de la altura física del televisor (1080p nativo) de borde a borde y desplazando simétricamente fuera del marco físico la barra de estado superior (hora, batería, señal) y la barra de navegación/gestos inferior.
        - **Modo FILL (Rellenar en Vertical):** Aplica factor moderado de **1.35f**, ampliando la imagen vertical sin recortes desmedidos.
        - **Zoom Gradual:** Aplica `currentZoom * 1.075f` (75%, 85%, 115%, 130%).
     2. **Imagen Horizontal (Video de Película/Partido o Celular Apaisado):**
        - La TV opera en `SCALE_ASPECT_FILL` con escala 1.0f cubriendo los 16:9 completos del televisor de borde a borde.
   - **Obediencia Inmediata del Comando STOP:**
     Todo mensaje de control `SignalingMessage.PlayControl("STOP")` o `Disconnect` recibido en `TvReceiverActivity.kt` DEBE detener de inmediato `ExoPlayerTvManager`, `DigitalAudioReceiver`, `WebRtcReceiverManager.resetConnection()` y transicionar el estado a `ReceiverUiState.Standby` en menos de 5ms, sin congelar la pantalla ni esperar timeouts de ICE.

8. **Separación Total y Blindaje de las Dos Opciones: Pantalla Completa vs Por Aplicación (HU-013, v3.4.28) [SELLADO E INMUTABLE]:**
   - **Independencia Absoluta de la Captura de Flujo Web:** Queda TERMINANTEMENTE PROHIBIDO modificar `TvScreens.kt` o `WebRtcReceiverManager.kt` para solucionar problemas de detección de flujos web, reproductores (ExoPlayer/TvWebStreamScreen) o bloqueador de anuncios.
   - **Opción 1: Pantalla Completa (`isAppProjectionMode == false`):**
     - Botón dedicado "Pantalla Completa" en el Sender (`MediaProjectionConfig.createConfigForDefaultDisplay()`).
     - En `TvScreens.kt`, cuando es vertical y no está en `FILL`, utiliza obligatoriamente `Modifier.fillMaxHeight().aspectRatio(streamAspect)` y `WRAP_CONTENT, MATCH_PARENT` para centrar la pantalla vertical del teléfono al 100% de la altura física del TV sin deformaciones.
   - **Opción 2: Por Aplicación (`isAppProjectionMode == true`):**
     - Botón dedicado "Por Aplicación" en el Sender (`MediaProjectionConfig.createConfigForUserChoice()`).
     - En `TvScreens.kt`, utiliza obligatoriamente `Modifier.fillMaxSize()` y `MATCH_PARENT, MATCH_PARENT`.
     - Ignora el sensor físico del teléfono (`isPhoneLandscape`) y obedece EXCLUSIVAMENTE la orientación real del frame del video (`effectiveFrameW > effectiveFrameH`), permitiendo zoom libre (75%, 85%, 115%, 130%) sobre toda la pantalla 16:9 sin recortes de Compose.
   - **Aislamiento Mutuo:** Queda PROHIBIDO mezclar o unificar ambas rutas; un cambio en una jamás debe afectar a la otra.

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
5. **Inmunidad e Intocabilidad del Canvas WebRTC (`TvScreens.kt`):**
   Auditar obligatoriamente que ningún cambio en extractores web, bloqueador de anuncios o reproductores de streaming modifique `TvScreens.kt` ni `WebRtcReceiverManager.kt`. Ambos archivos están sellados para preservar la proyección por aplicación y pantalla completa.
6. **Interceptor Nativo Anti-CORS y Renovación Síncrona de Flujos Web (`v3.4.55` / HU-012) [LEY INVIOLABLE]:**
   - En `TvWebStreamScreen.kt`, `webViewClient` DEBE delegar incondicionalmente `shouldInterceptRequest` a `TvWebResourceInterceptor.shouldIntercept(r, headers)`. Queda TERMINANTEMENTE PROHIBIDO eliminar o alterar este interceptor. Responde preflights OPTIONS en 0ms y descarga los fragmentos (.m3u8, .ts, .vtt) inyectando obligatoriamente `Access-Control-Allow-Origin: *`, erradicando cualquier bloqueo CORS de Chromium.
   - En `MediaPlatformResolver.kt`, toda transmisión de streams protegidos (LuluStream/TNMR/Fastream) DEBE ejecutar la renovación síncrona en `prepareStreamForPlayback` al momento exacto de transmitir (token fresco de 0ms).
   - En `BrowserUrlHelper.kt`, las listas secundarias de subtítulos VTT (`/lang/`, `/subs/`, `_spa/index`) quedan permanentemente excluidas de la detección como videos primarios.
7. **Resolución Síncrona Fastream / DropCDN y User-Agent de Escritorio (`v3.4.57` / HU-012) [LEY INVIOLABLE]:**
   - Para servidores de video Fastream / DropCDN (`fastream.to`, `dropcdn.io`, `dr0pstream.com`), las solicitudes de manifiesto y fragmentos DEBEN enviarse obligatoriamente con identidad de Escritorio Windows (`UA_FASTREAM`) y cabecera `Referer: https://fastream.to/embed-$id.html`. Las CDNs de Fastream devuelven `HTTP 403 Forbidden` a identidades móviles (Android/Linux).
   - El control de subtítulos y calidad HLS en `TvWebStreamScreen` se opera dualmente: vía control remoto físico de TV Box (D-Pad Arriba = Subtítulos, D-Pad Abajo = Calidad HLS con badge flotante) y desde los botones remotos del móvil (`RemoteExpandedControls.kt`).
8. **Supresión Nativa de Popups sin WebViews Temporales y Protección de Gestos (`v3.4.59` / HU-007) [LEY INVIOLABLE]:**
   - En `BrowserWebView.kt`, `WebChromeClient.onCreateWindow` DEBE retornar `false` directamente para suprimir popups publicitarios de forma nativa. Queda TERMINANTEMENTE PROHIBIDO instanciar `WebView` temporales dentro de `onCreateWindow` para evaluar URLs emergentes; cada WebView consume 40-80 MB de RAM y en sitios con alta densidad publicitaria satura el límite de 512 MB de la máquina virtual Android provocando `OutOfMemoryError` y cierre abrupto del navegador.
   - En `shouldOverrideUrlLoading`, las navegaciones externas no autorizadas se bloquean únicamente en redirecciones automáticas sin intervención del usuario (`!r.hasGesture()`). Queda PROHIBIDO bloquear clics intencionales del usuario (`r.hasGesture() == true`) que no pertenezcan a dominios de publicidad conocidos.
9. **Erradicación Definitiva de Proxies Locales y Transmisión Directa Autónoma (Estándar Web Video Caster / v3.32.88, v3.4.17) [LEY INVIOLABLE]:**
   - Queda TERMINANTEMENTE PROHIBIDO reactivar o forzar proxies locales (`LocalStreamProxyServer`, puerto 8092, o servidores NanoHTTPD/Jetty en el móvil) para enrutar flujos de video hacia la TV Box. Dicho enfoque fue eliminado definitivamente en los commits `a689254` y `56982a3`.
   - Web Video Caster y OmniCast operan transmitiendo directamente la URL de internet con las cabeceras exactas (`Referer` y `User-Agent`) hacia la TV Box, garantizando reproducción autónoma sin consumo de batería ni saturación de sockets en el celular.
   - La TV Box reproduce de forma autónoma con decodificación por hardware en ExoPlayer o mediante el reproductor HTML5 nativo `TvWebStreamScreen`.
10. **Aislamiento Estricto de Captchas (Cloudflare Turnstile) y Blindaje Anti-Spam (HU-007) [LEY INVIOLABLE]:**
   - La bandera `isChallenge` en `BrowserWebView.kt` aplica ÚNICA Y EXCLUSIVAMENTE dentro del subframe o petición hacia el dominio oficial de verificación (`challenges.cloudflare.com`).
   - Queda TERMINANTEMENTE PROHIBIDO que `isChallenge` permanezca en `true` para la página principal o para clics/navegaciones posteriores.
   - Todo toque o gesto del usuario sobre el reproductor o elementos emergentes que redirija a dominios publicitarios, de apuestas o desconocidos DEBE ser bloqueado (`return true`).
11. **Cláusula Cerrojo de Inviolabilidad Arquitectónica y Protección de Flujos Operativos [BLOQUEANTE]:**
   - Antes de proponer o aplicar cualquier cambio para un portal de películas o videos específico, el agente DEBE verificar que no altere el funcionamiento ni degrade los sitios que ya operan con total estabilidad: PelisGO, Spider-Man, LuluStream, TNMR, Canela.tv, ok.ru, YouTube, Dailymotion y portales de fútbol/deportes en vivo (808ball).
   - Queda TERMINANTEMENTE PROHIBIDO romper o desestabilizar flujos globales por intentar parchar un sitio individual. Toda adaptación para un nuevo portal debe ser un adaptador modular aislado según el Principio "Poner y Sacar" (Sección 6).
12. **Delimitación Estricta de Nombres de Archivo y Rutas URL (Ley Anti-Colisión de Subcadenas / RFC 8216) [BLOQUEANTE]:**
   - Toda verificación o comparación de nombres de manifiestos HLS (`master.m3u8`, `playlist.m3u8`, `index.m3u8`, etc.) DEBE realizarse obligatoriamente mediante coincidencia exacta del segmento final de ruta (`url.substringAfterLast('/').equals(name, ignoreCase = true)`) o exigiendo delimitador de barra (`endsWith("/$name")`).
   - Queda TERMINANTEMENTE PROHIBIDO usar `endsWith(name)` libre sin delimitador de ruta para evitar que sub-variantes de video compuesto (como `streamPlaylist.m3u8` de Sky News) colisionen con `playlist.m3u8`, lo cual desactiva el re-ascenso a master y deja la transmisión en vivo sin audio en la TV Box.




