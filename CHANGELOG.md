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
