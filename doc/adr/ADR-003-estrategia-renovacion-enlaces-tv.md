# ADR-003: Estrategia de Auto-Renovación de Flujos Expirados en Android TV (Receiver)

- **Estado:** ACEPTADO
- **Fecha:** 2026-10-15
- **Autores:** Equipo de Arquitectura OmniCast
- **Contexto:** En la Android TV (Receiver), las películas y series guardadas en listas ("Ver más tarde", etc.) contienen enlaces directos (`url`) que caducan periódicamente (tokens con TTL de 2h a 24h, HTTP 403/404 de servidores como Fastream, Streamtape, Streamwish). Sin embargo, la TV conserva la URL de la página de origen (`pageUrl`) y el progreso temporal (`lastPositionMs`).

---

## 1. Decisión de Diseño

Se adopta una arquitectura de resolución en **tres niveles escalonados**, priorizando la **autonomía total de la APK en la TV**:

### Nivel 1: Resolución Autónoma en la TV (Headless / HTTP Extractor)
- La APK de la TV detecta fallos `ERROR_CODE_IO_BAD_HTTP_STATUS` (HTTP 403 / 404 / 410) emitidos por `ExoPlayer`.
- La TV ejecuta de forma autónoma una de dos técnicas:
  1. **Extractor HTTP nativo:** Para servidores estándar, descarga el HTML/JS del embed en milisegundos, aplica regex y desempaqueta el script JS para obtener el nuevo `.m3u8`/`.mp4`.
  2. **WebView Headless temporal:** Si el reproductor requiere inicialización de scripts DOM, se instancia un `WebView` invisible en memoria durante 2-3 segundos con interceptor de peticiones de red (`shouldInterceptRequest`). Al capturar el stream, el `WebView` se destruye inmediatamente para evitar consumo de RAM en TV Boxes de 1GB/2GB.
- Al obtener el stream fresco, se sobreescribe el campo `url` en `TvPlaylistsManager`, conservando intactos `lastPositionMs`, `title` y la carpeta contenedora, reanudando la reproducción en el segundo exacto.

### Nivel 2: Asistencia por Red Local (Fallback Online con Móvil)
- Si la TV está enlazada al emisor móvil (`LocalSignalingServer` :8088) y el extractor local de la TV detecta un bloqueo complejo, envía un mensaje de socket: `RenewStreamRequest(videoId, pageUrl, lastPositionMs)`.
- El móvil, que dispone de un motor de extracción completo y AdBlocker avanzado en C++, resuelve el flujo fresco y lo devuelve a la TV en segundo plano.

### Nivel 3: Respaldo Manual por Código QR (Fallback Anti-Bot/Captcha)
- Si el sitio web requiere interacción humana obligatoria (ej. resolución de Cloudflare Turnstile o Captcha), la TV despliega el diálogo modal de error enriquecido con un **Código QR dinámico**.
- Al escanear el QR con el móvil, se abre la `pageUrl` en el navegador interno de OmniCast con el tiempo preseleccionado para reenviar el flujo en 1 toque.

---

## 2. Consecuencias y Beneficios

- **Autonomía:** La APK de la TV no es un cliente dependiente del móvil; puede funcionar de manera independiente para reanudar contenido guardado.
- **Ahorro de Memoria en TV Box:** El `WebView` de la TV solo se instancia bajo demanda de renovación y se destruye inmediatamente tras capturar el stream.
- **Preservación de Estado:** El usuario nunca pierde su punto de reproducción (`lastPositionMs`).
