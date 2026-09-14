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

