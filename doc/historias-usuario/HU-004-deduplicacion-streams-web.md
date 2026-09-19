# 📌 HU-004: Detección Limpia y Anti-Duplicados en Streams Web (Dailymotion, ok.ru)

- **Estado:** 🟢 Aprobado / En Producción (v3.28)
- **Módulo Principal:** `:app-sender` (`InAppBrowserScreen`, `DynamicSnifferManager`, `MediaPlatformResolver`), `:app-receiver` (`TvPlaybackHistoryManager`)
- **Última Actualización:** 2026-09-19 (v3.28)

---

## 1. Descripción de la Historia
Como usuario que navega en sitios de video como Dailymotion, Pelismart u ok.ru,  
Quiero que OmniCast capture el título real del video y no genere tarjetas duplicadas cuando el reproductor web cargue múltiples variantes o calidades de video,  
Para tener un historial limpio y títulos comprensibles tanto en el móvil como en la TV.

---

## 2. Criterios de Aceptación Inamovibles

### CA-01: Captura del Título Real
- El título del video debe extraerse del DOM de la página (`document.title` o etiquetas del reproductor) antes de registrar el stream en el historial o enviarlo a la TV.
- No se permiten títulos genéricos vacíos o repetidos como "Dailymotion" sin el nombre del video.

### CA-02: Deduplicación Automática de Variantes CDN
- Sitios web que cargan streams adaptativos (1080p, 720p, chunklist, master.m3u8) para un mismo video deben agruparse en una sola entrada.
- En la TV Receiver y en el móvil no deben aparecer entradas repetidas del mismo contenido.

---

## 3. Historial de Ajustes
- **v3.22:** Soporte de extracción para ok.ru sin excepciones JSON.
- **v3.28:** Extracción de metadatos en Dailymotion y deduplicación de variantes CDN en `TvPlaybackHistoryManager`.
