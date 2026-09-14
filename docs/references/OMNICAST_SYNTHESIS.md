# 🚀 SÍNTESIS DE INGENIERÍA: OMNICAST PRO (UNIFICACIÓN DE TEORÍA Y PRÁCTICA)

Este documento sintetiza la integración de las mejores prácticas de la industria (extraídas de **Web Video Caster** y **AirScreen**) con la **Nueva Teoría de OmniCast Pro** (Clean Architecture, archivos $\le 200$ líneas, WebRTC local P2P sin nube, latencia $< 80$ms).

---

## 🏛️ MATRIZ DE ASIMILACIÓN TÉCNICA

| Módulo Crítico | Problema Clásico en Proyección | Solución Probada en Referencias | Implementación en OmniCast Pro (Nueva Teoría) |
|---|---|---|---|
| **Casteo de Pantalla (Espejo)** | Congelamiento por colas saturadas del codificador por hardware en chipsets móviles. | AirScreen decodifica por hardware con buffer mínimo de 1 frame y descarte de frames viejos. | `WebRtcSenderManager.kt`: captura ajustada a 720p (máx 1280px, múltiplo de 16, 30-45 fps, 1.5-4.5 Mbps). Cero caídas en Qualcomm/MediaTek. |
| **Señalización Local** | Dos conexiones concurrentes que colisionan y pierden el `Answer`. | Canal único persistente por sesión. | `SignalingClientHolder.kt`: Singleton de conexión WebSocket única entre UI y servicio de proyección. |
| **Aislamiento de Popups (Pelismart)** | El botón "Play" abre publicidad secuestrando la pestaña principal y volviendo a la búsqueda. | Web Video Caster permite `window.open` y aisla la ventana en un sub-WebView. | `BrowserWebView.kt`: intercepta redirecciones externas en `shouldOverrideUrlLoading`, las carga en `popupWebView` con `[ ✕ Descartar ]`, manteniendo la pestaña de la película 100% intacta. |
| **Sniffing de Video Web** | Sitios bloquean `@JavascriptInterface` o usan ofuscación DRM ligera. | `ibfunctions.js`: `MutationObserver` y envío seguro vía `console.log` interceptado en `onConsoleMessage`. | Motor modular en `AdBlocker.kt` e inyección de listeners en `BrowserWebView.kt`. |
| **Headers HTTP y Bloqueo 403** | Enlaces de streaming fallan en la TV si no tienen el `Referer` original. | Web Video Caster envía JSON con `headers` (`Referer`, `User-Agent`, `Origin`). | `MediaStreamInfo.kt`: transporta mapa de headers HTTP para pasarlos a `DefaultHttpDataSource.Factory` de ExoPlayer. |
| **Control Remoto D-Pad TV** | La TV Box se maneja con control remoto y no con pantalla táctil. | Web Video Caster: Flechas izquierda/derecha saltan +-10s, centro Play/Pause, arriba/abajo muestran/ocultan HUD. | `ConnectedActivity.kt` / `TvScreens.kt`: mapeo nativo de `KeyEvent` D-Pad con foco accesible. |
| **Subtítulos y YouTube** | Subtítulos invasivos activados por defecto y sobrecarga de opciones. | Desactivación por defecto en el arranque del reproductor. | `TvYouTubeScreen.kt`: parámetros `cc_load_policy=0` y descarga del módulo de subtítulos en la inicialización. |
