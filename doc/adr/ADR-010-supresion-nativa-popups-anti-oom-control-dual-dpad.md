# ADR-010: Supresión Nativa de Popups Anti-OOM, Blindaje de Gestos y Control Dual D-Pad en TV

**Estado:** Aprobado  
**Fecha:** 2026-10-25  
**Contexto:** Chromium WebView Android, Sitios de Streaming con Inyección Agresiva de Popups (Pelismart, Reypelis, Pelis182), Límite ART Heap de 512 MB, Android TV Box D-Pad Remote  
**Versión de Implementación:** v3.4.56 - v3.4.59  

---

## 1. Contexto y Problemática

Durante la navegación en portales de películas y series con reproductores embebidos de terceros (`embed69.org`, `paulinito.com`, `fastream.to`):
1. **Colapso de Memoria RAM (OOM Crash a 512 MB):** Sitios de streaming disparan múltiples ráfagas continuas de `window.open(adUrl, "_blank")` ligadas al evento click. Previamente, `WebChromeClient.onCreateWindow` creaba una instancia de `WebView` temporal (`tempWebView`) para evaluar y transportar la navegación. En páginas con alta densidad de scripts, se acumulaban decenas de WebViews en cuestión de segundos, alcanzando el techo de 512 MB del heap de Android ART y provocando el cierre forzado de la aplicación por `OutOfMemoryError`.
2. **Bloqueo Accidental de Clics Legítimos:** Al intentar frenar redirecciones automáticas sin calibrar el gesto del usuario, se bloqueaban enlaces externos legítimos donde el usuario hacía clic explícitamente para seleccionar un servidor de video.
3. **Control Ergonométrico Incompleto en TV Box:** Los usuarios de TV Box con mando a distancia tradicional no disponían de un mecanismo rápido y directo con el D-Pad para activar o alternar subtítulos ni cambiar la calidad de video HLS sin invocar menús intrusivos.
4. **Restricciones de Origen Cruzado (CORS) en Chromium TV:** Reproductores HTML5 (`Hls.js`) en TV Box eran bloqueados por Chromium al solicitar fragmentos a CDNs con dominios dispares (`*.tnmr.org`, `*.dropcdn.io`), abortando la reproducción por fallos de red.

---

## 2. Decisión Arquitectónica

Se implementa una solución integral de 4 capas:

1. **Supresión Nativa de Popups en `onCreateWindow` (Anti-OOM):**
   - En `BrowserWebView.kt`, cuando `isAdBlockEnabled` está activo y el dominio no está en la lista blanca de sitios oficiales, `onCreateWindow` retorna incondicionalmente `false`.
   - Esto indica nativamente al motor Chromium que rechace la creación de la ventana emergente en 0ms sin instanciar ningún objeto `WebView` en memoria.
   - El consumo de memoria adicional por intento de popup pasa de ~40-80 MB a **0 MB exactos**, erradicando de raíz cualquier colapso de heap.

2. **Blindaje de Gestos Intencionales del Usuario (`r.hasGesture()`):**
   - En `shouldOverrideUrlLoading`, se respeta estrictamente la condición `!r.hasGesture()`.
   - Si el usuario hizo clic intencional sobre un enlace o pestaña de servidor, la navegación procede con normalidad. Solo las redirecciones automáticas sin interacción táctil son interceptadas y neutralizadas.

3. **Control Dual de Subtítulos y Calidad HLS por D-Pad:**
   - **D-Pad Arriba (`TvRemoteKeyHandler.kt`):** Alterna pistas de subtítulos (Español / Desactivado) en reproductores HTML5 (`TvWebStreamScreen.kt`).
   - **D-Pad Abajo:** Cicla secuencialmente los niveles de calidad HLS disponibles (Auto -> 1080p -> 720p -> 480p).
   - **Feedback Visual:** La TV Box despliega un badge flotante semi-transparente durante 2.5s confirmando el cambio sin interrumpir el video.
   - **Paridad en Móvil:** `RemoteExpandedControls.kt` incorpora botones equivalentes para controlar subtítulos y calidad desde el teléfono.

4. **Interceptor Nativo Anti-CORS (`TvWebResourceInterceptor.kt`):**
   - Responde inmediatamente preflights `OPTIONS` en 0ms con código HTTP 200 y cabeceras permisivas.
   - Descarga manifiestos y fragmentos de video vía socket nativo Java e inyecta `Access-Control-Allow-Origin: *` hacia Chromium, neutralizando cualquier política restrictiva de origen cruzado.

---

## 3. Consecuencias

- **Positivas:**
  - Estabilidad absoluta de memoria: cero riesgo de `OutOfMemoryError` en sitios con popups masivos.
  - Navegación fluida: los clics del usuario a servidores y opciones de películas funcionan al primer toque.
  - Control de video de nivel profesional en TV Box usando únicamente las flechas Arriba y Abajo del mando.
  - Reproducción garantizada de flujos HLS protegidos sin colapsos de CORS.
- **Límites:**
  - Toda nueva regla de filtrado en `AdBlocker.kt` debe mantenerse concisa y modular.
  - Los archivos de TV y navegador deben preservar el límite inquebrantable de <= 300 líneas.
