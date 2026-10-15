# ADR-003: Estrategia de Auto-Renovación de Flujos Expirados en Android TV (Receiver)

- **Estado:** ACEPTADO
- **Fecha:** 2026-10-15
- **Autores:** Equipo de Arquitectura OmniCast
- **Contexto:** En la Android TV (Receiver), las películas y series guardadas en listas ("Ver más tarde", etc.) contienen enlaces directos (`url`) que caducan periódicamente (tokens con TTL de 2h a 24h, HTTP 403/404 de servidores como Fastream, Streamtape, Streamwish). Sin embargo, la TV conserva la URL de la página de origen (`pageUrl`) y el progreso temporal (`lastPositionMs`).

---

## 1. Decisión de Diseño Definitiva

Se descarta la extracción ciega en segundo plano por WebView en la TV para evitar la captura de videos basura (anuncios preroll de 15s, banners de casinos o desvíos de idioma). La resolución se fundamenta en el **criterio humano asistido por Código QR dinámico con sustitución en caliente de enlace**:

### Paso 1: Detección y Presentación de QR
- Al intentar reproducir una película guardada cuyo token web haya expirado (HTTP 403 / 404 / 410):
- `ExoPlayerTvManager` intercepta la excepción sin cerrar la app ni dejar pantalla negra.
- La TV despliega un diálogo modal:
  - Título: 🎬 **Enlace de video caducado en la web**
  - Subtítulo: *"Escanea con tu celular para reactivar la película con 1 toque"*
  - **Código QR dinámico:** Contiene la `pageUrl` de la película y el parámetro temporal `posMs=lastPositionMs`.

### Paso 2: Elección Humana en Celular y Re-Transmisión
- Al escanear con el móvil, se abre la página en el navegador interno de OmniCast.
- El usuario humano selecciona con precisión el reproductor e idioma deseado (evitando cualquier video basura publicitario).
- Al pulsar *"Transmitir a TV"*, el móvil envía el flujo con el token fresco y el timestamp guardado.

### Paso 3: Sustitución Automática del Enlace en la TV (Token TTL)
- La TV recibe el nuevo flujo y ejecuta una actualización en `TvPlaylistsManager`:
  - **Sustituye la URL expirada por la nueva URL activa.**
  - **Preserva intactos:** Título, carpeta ("Ver más tarde", etc.) y posición de reproducción (`lastPositionMs`).
  - La película arranca en el segundo exacto donde se quedó.
- **Beneficio del TTL (6 a 12 horas):** Si el usuario pausa la película o apaga la TV y regresa más tarde ese mismo día, la TV puede reproducir directamente sin requerir escanear el QR nuevamente hasta que el nuevo token vuelva a caducar días después.

---

## 2. Consecuencias y Beneficios

- **Autonomía:** La APK de la TV no es un cliente dependiente del móvil; puede funcionar de manera independiente para reanudar contenido guardado.
- **Ahorro de Memoria en TV Box:** El `WebView` de la TV solo se instancia bajo demanda de renovación y se destruye inmediatamente tras capturar el stream.
- **Preservación de Estado:** El usuario nunca pierde su punto de reproducción (`lastPositionMs`).
