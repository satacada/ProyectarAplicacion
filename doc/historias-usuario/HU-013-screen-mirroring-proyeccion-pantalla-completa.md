# HU-013: Screen Mirroring — Proyección de Pantalla Completa Vertical y Horizontal

**Épica:** Transmisión de Pantalla en Tiempo Real (Screen Mirroring)  
**Versión de Introducción:** v3.32.27 (Consolidado e Inamovible en v3.4.8)  
**Estado:** Inamovible / Contrato Técnico y de Experiencia de Usuario  

---

## 1. Descripción de la Historia

**Como** usuario de OmniCast en teléfono móvil y Android TV / TV Box,  
**Quiero** proyectar la pantalla de mi celular en tiempo real de forma limpia y automática,  
**Para** que en orientación vertical ocupe el 100% de la altura de mi televisor sin franjas negras superior ni inferior, y al girar a horizontal (YouTube, videos o juegos) se expanda automáticamente a pantalla completa 16:9 sin encogerse a un cuarto ni quedar cortada.

---

## 2. Criterios de Aceptación (Contrato Inamovible)

### A. Proyección Vertical (Celular Erguido) — Adaptación Matemática de Pantalla
1. **Modo FIT (100% Altura sin Barras de Sistema - Factor 1.075f):**
   - En orientación vertical, la TV Box renderiza en `SCALE_ASPECT_FIT` con escala base estricta de **1.075f**.
   - Ocupa el 100% íntegro de la altura física del televisor (1080p nativo) sin franjas negras arriba o abajo.
   - Desplaza simétricamente fuera del marco físico del televisor los ~40px de la barra de estado superior (hora, batería, señal) y los ~40px de la barra inferior de navegación/gestos, garantizando que el 100% del contenido útil del teléfono sea visible sin recortes de texto ni pérdida de elementos de la interfaz.
2. **Modo FILL en Vertical (Rellenar Proporcional Controlado - Factor 1.35f):**
   - Queda terminantemente PROHIBIDO aplicar `SCALE_ASPECT_FILL` a un flujo vertical en un televisor 16:9, ya que requeriría escalar un ratio 9:20 a 16:9 multiplicando la escala por ~3.95x y recortando el 75% del celular.
   - En su lugar, el modo FILL en vertical aplica un factor moderado de **1.35f**, ampliando la imagen vertical para acercar el contenido sin destruir la visibilidad de la pantalla.
3. **Modos de Zoom Gradual:**
   - Aplica `currentZoom * 1.075f` (presets de 75%, 85%, 115%, 130%) preservando el centrado simétrico horizontal.
4. **Centrado Simétrico:**
   - La columna vertical del celular se mantiene centrada en el eje horizontal de la TV con barras laterales simétricas propias del aspect ratio vertical del teléfono.

### B. Proyección Horizontal (Celular Apaisado / Videos 16:9)
1. **Llenado 16:9 de Borde a Borde:**
   - Al reproducir videos horizontales (YouTube, películas) o girar el teléfono a apaisado, la TV Box opera incondicionalmente en `SCALE_ASPECT_FILL` con escala 1.0f cubriendo toda la pantalla de borde a borde.
2. **Detección Pura de Orientación por Frames y Sensor:**
   - `isLandscape` se determina EXCLUSIVAMENTE por frames decodificados reales (`effectiveFrameW > effectiveFrameH`) o sensor de orientación (`isPhoneLandscape`).
   - Ningún modo de visualización (`FILL`, `16:9`) puede adulterar la orientación a horizontal si los frames recibidos son verticales.

### C. Separación Total de las Dos Opciones: Pantalla Completa vs Por Aplicación
1. **Opción 1: Proyección de Pantalla Completa (`isAppProjectionMode == false`):**
   - Botón dedicado "Pantalla Completa" en el Sender (`MediaProjectionConfig.createConfigForDefaultDisplay()`).
   - En `TvScreens.kt`, cuando es vertical y no está en `FILL`, utiliza `Modifier.fillMaxHeight().aspectRatio(streamAspect)` y `WRAP_CONTENT, MATCH_PARENT` para centrar la pantalla vertical del teléfono al 100% de la altura física del TV sin deformaciones.
2. **Opción 2: Proyección por Aplicación (`isAppProjectionMode == true`):**
   - Botón dedicado "Por Aplicación" en el Sender (`MediaProjectionConfig.createConfigForUserChoice()`).
   - En `TvScreens.kt`, utiliza `Modifier.fillMaxSize()` y `MATCH_PARENT, MATCH_PARENT`.
   - Ignora la orientación física del teléfono y obedece exclusivamente la orientación real del frame del video (`effectiveFrameW > effectiveFrameH`), permitiendo zoom libre sobre los 16:9 de la TV sin recortes.
3. **Independencia Absoluta de la Captura de Flujos Web:**
   - Ambas opciones son 100% independientes entre sí y de la captura web (ExoPlayer, HTML5, extractores).

### D. Obediencia Inmediata del Comando STOP
1. **Terminación Instantánea (<5ms):**
   - Al pulsar "Detener Transmisión" o recibir `SignalingMessage.PlayControl("STOP")` o `Disconnect`, la TV Box detiene inmediatamente el decodificador WebRTC (`webRtcReceiver.resetConnection()`), el receptor de audio digital (`digitalAudioReceiver?.stop()`) y transiciona de inmediato a `ReceiverUiState.Standby` sin congelamiento ni espera de timeouts.

---

## 3. Estado de Sellado Definitivo (v3.4.28)
- **Consolidación Final:** Validado y sellado en `v3.4.28` con separación total de ambas opciones.
- **Intocabilidad:** Los archivos `TvScreens.kt` y `WebRtcReceiverManager.kt` quedan blindados contra modificaciones transversales.

