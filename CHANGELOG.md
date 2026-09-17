# 📋 CHANGELOG & CONTROL DE VERSIONES - OmniCast Pro

Historial sistemático de versiones, control de cambios y validación de regresiones.

---

## 📌 [v3.4-baseline] - Referencia Histórica de Casteo Estable
- **Estado de Screen Mirroring:** Totalmente funcional y estable en redes locales.
- **Canalización WebRTC:** MediaProjection H.264 High Profile por hardware.
- **Audio:** Sincronizado vía pipeline local.
- **Limitación conocida en v3.4:**
  - Fotos verticales de galería se cortaban en bordes superior/inferior si la pantalla forzaba fill.
  - YouTube y streams directos con protecciones de redirección aún no tenían captura desacoplada ni selector de pistas.

---

## 📌 [v3.7] - Módulo YouTube y Manejo de Popups
- **YouTube:** Corrección de cambio secuencial de múltiples videos mediante clave de estado única y ciclo de vida (`TvYouTubeScreen`).
- **Navegador Web:** Detección de ventanas emergentes (popups de sitios de streaming como Pelismart/Vidhide) con overlay para descartar la pestaña publicitaria y regresar a la pestaña de reproducción.
- **Regresión detectada:** Inestabilidad en la conexión de espejo de pantalla al intentar modificar el canal de audio/señalización.

---

## 📌 [v3.8] - Restauración de Espejo v3.4, Selector de Idioma/Subtítulos y Blindaje
- **Módulo Espejo de Pantalla (BLINDADO):**
  - Restauración exacta del pipeline WebRTC de v3.4.
  - Ajuste de escala predeterminado en TV a `SCALE_ASPECT_FIT` para que fotos verticales y horizontales se vean completas sin recortar.
  - Alternancia a `SCALE_ASPECT_FILL` mediante botón central del control D-Pad.
- **Módulo Reproductor Directo (Streaming TV):**
  - Soporte de selección de pistas de subtítulos (activar/desactivar).
  - Selector de pista de audio/idioma si el video fuente dispone de múltiples pistas.
- **Módulo Navegador:**
  - Soporte activo de descarte de publicidad en sitios de películas.
- **Gobernanza:**
  - Repositorio Git inicializado con control estricto de diffs y modularidad quirúrgica (archivos <= 200 líneas).

---

## 📌 [v3.9] - Aislamiento de Popups Pelismart, WebRTC Unificado y YouTube Optimizado
- **Navegador Web & Pelismart:**
  - Corrección radical del secuestro de pestaña: eliminación del override nulo de `window.open` que forzaba a los reproductores pirata a redirigir la ventana principal con `window.location.href`.
  - Intercepción completa de intentos de redirección en `BrowserWebView.kt` redirigiéndolos al `popupWebView` aislado.
  - La pestaña principal donde se presiona "Play" permanece 100% intacta, permitiendo que el reproductor cargue el stream y OmniCast intercepte el enlace `.m3u8`.
  - Botón interactivo `[ ✕ Descartar y Volver ]` para cerrar el anuncio publicitario y regresar de inmediato.
- **Espejo de Pantalla (Screen Mirroring WebRTC):**
  - Creación de `SignalingClientHolder` (Singleton) para compartir una única conexión WebSocket entre la UI y el servicio de proyección (`ProjectionSession`), eliminando pérdida de paquetes de señalización (`Answer` / ICE Candidates).
  - Normalización de resolución a 720p (máx 1280px con alineación a 16, 30-45 fps, 1.5 - 4.5 Mbps) para eliminar sobrecarga del codificador por hardware Qualcomm (`HardwareVideoEncoder: Dropped frame, encoder queue full`).
- **YouTube:**
  - Subtítulos desactivados por defecto en la TV (`cc_load_policy=0` y descarga del módulo de captions en IFrame API).
- **Control Remoto & UI:**
  - Eliminado el selector de pistas de idioma a petición del usuario.
- **Gobernanza:**
  - 100% de los archivos de código Kotlin respetan el límite de <= 200 líneas.


---

## [v3.10] - Casteo WebRTC Restaurado + Fix Pelismart Play
- **Espejo de Pantalla (Screen Mirroring WebRTC):**
  - `ProjectionSession` instancia su propio `SignalingClient` dedicado.
  - Eliminada guarda de sesion que impedia reconexion limpia en `SignalingClient.connect()`.
  - Encoder H.264 sin High Profile, compatible Qualcomm Snapdragon 685.
  - Escala de captura 1920/maxScreen restaurada igual que v3.4.
  - `setBitrate()` usa fps del perfil activo en lugar de hardcoded 45fps.
- **Navegador Web & Pelismart:**
  - Eliminado bloque JS de `AdBlocker` que borraba elementos zIndex>=100 (mataba boton Play).
  - Agregados pelismart.mov, pelismart.com, embed69.org y players a KNOWN_MEDIA_HOSTS.
- **Gobernanza:**
  - APKs Sender v3.10 y Receiver v3.10 guardados en releases/.
  - Regla: toda version compilada se guarda en releases/ ANTES de instalar en dispositivo.

---

## [v3.12] - Compatibilidad Universal Android 8/9/10+, YouTube TV Autoplay y Asistente Flotante con Scroll
- **Compatibilidad de Plataforma:**
  - `minSdk = 26` en `:app-sender` para habilitar instalación en celulares con Android 8.0, 8.1 y 9.0 (Pie).
  - Detección dinámica en tiempo de ejecución: audio digital interno puro en Android 10+ (`Build.VERSION_CODES.Q`) y fallback seguro en Android 9 e inferiores.
  - `:app-receiver` soporta nativamente desde Android 7.0 (API 24) hasta Android 14.
- **Módulo YouTube en TV Box (`TvYouTubeScreen`):**
  - Migración a la API formal `YT.Player` con parámetro `enablejsapi=1` y bypass del bloqueo de autoplay de Android TV (mute inicial breve y desmuteo inmediato tras arrancar).
  - Eliminado el congelamiento en el logo de YouTube; reproducción inmediata a pantalla completa sin subtítulos automáticos.
- **Asistente Flotante (`FloatingViewBuilder`):**
  - Menú envuelto en `ScrollView` con barra de desplazamiento en el lateral izquierdo (`verticalScrollbarPosition = LEFT`) y altura máxima controlada (270dp) para que todas las opciones sean siempre accesibles sin importar la posición del botón verde.
- **Casteo y Control de Relación de Aspecto en TV:**
  - Base de decodificación WebRTC idéntica a la versión v3.4 estable.
  - Alternancia entre modo `FIT` y modo `FILL (16:9)` mediante botón OK del control remoto o comando remoto desde el celular para eliminar franjas negras superior/inferior.
- **Resguardo de Artefactos:**
  - `OmniCast-Sender-Mobile-v3.12.apk` y `OmniCast-Receiver-TVBox-v3.12.apk` archivados en `releases/`.

---

## [v3.13] - Estabilización Definitiva Modular: WebRTC v3.4 Exacto, Fix YouTube 152-4, Asistente Flotante y Fotos
- **Módulo 1: Espejo de Pantalla (Screen Mirroring WebRTC):**
  - Restauración exacta de `WebRtcSenderManager.kt` a la configuración v3.4: `DefaultVideoEncoderFactory` con `enableH264HighProfile = true`.
  - Captura inmediata de pantalla dentro de `startScreenStreaming()` con resolución basada en escala `minOf(1.0f, 1920f / maxScreen)`.
  - Eliminado el desfase que provocaba pantalla negra y desconexión a los 6 segundos.
- **Módulo 2: YouTube en TV Box (`TvYouTubeScreen`):**
  - Reversión a la estructura de embed limpia `youtube-nocookie.com/embed/$videoId` de v3.8/v3.9.
  - Eliminado el interceptor de scripts de publicidad que detonaba el bloqueo anti-bot de Google ("Error 152-4").
- **Módulo 3: Asistente Flotante Permanente (`FloatingViewBuilder`):**
  - Corrección crítica en `attachDragToMove`: retorna `true` en `ACTION_DOWN` para garantizar recepción fluida de eventos `MOVE` y `UP` por el sistema operativo.
  - El botón verde responde al instante, se arrastra suavemente y el menú desplegable no se congela.
- **Módulo 4: Envío de Fotos y Galería Local:**
  - `LocalImageServer` ahora se auto-inicia automáticamente al solicitar servir cualquier imagen o video (`serveMedia`), garantizando que esté activo sin importar si la actividad principal está en segundo plano.
  - `PhotoCastViewerActivity` envía `ShowImage` al conectar y mantiene la sesión activa para soportar zoom táctil y paneo en tiempo real.
- **Módulo 5: Streaming Web (ok.ru, Dailymotion, Pelismart):**
  - Validación de tests unitarios automáticos (`StreamExtractorTest`) para captura de streams HLS, MP4 y URLs directas.
- **Resguardo de Artefactos:**
  - `releases/OmniCast-Sender-Mobile-v3.13.apk` y `releases/OmniCast-Receiver-TVBox-v3.13.apk` archivados.
