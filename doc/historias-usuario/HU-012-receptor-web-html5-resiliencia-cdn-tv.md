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
