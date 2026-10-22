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

### A. Proyección Vertical (Celular Erguido) — Desacoplamiento de Dos Rutas
1. **Ruta A: Proyección de Pantalla Completa (Modo FIT 1.0f - 100% Íntegra):**
   - La pantalla completa del teléfono debe mostrarse al 100% íntegra en la altura física del televisor (1080p nativo) en `SCALE_ASPECT_FIT` con escala base estrictamente `1.0f`.
   - La barra de estado superior (batería, hora, señal) y la barra de navegación/gestos inferiores del teléfono deben ser 100% visibles sin ningún recorte ni pérdida de bordes.
2. **Ruta B: Proyección Por Aplicación (Modos APP / CROP_BARS / APP_FIT 1.075f):**
   - Ruta independiente para proyección de una aplicación individual o recorte deliberado de barras del sistema para ampliar contenido, aplicando el factor de 1.075x.
   - Ambas rutas operan con estados y parámetros aislados; una modificación en una no puede alterar la otra.
3. **Centrado Simétrico:**
   - La columna vertical del celular debe estar perfectamente centrada en el eje horizontal de la TV, con barras negras laterales simétricas propias de la relación de aspecto del teléfono (ej. 9:20 o 9:19.5).
4. **Sin Deformación Lateral:**
   - Queda terminantemente prohibido estirar la imagen vertical horizontalmente para llenar un televisor 16:9 (prohibido aplicar factores multiplicadores de distorsión lateral como 3.95x).

### B. Proyección Horizontal (Celular Apaisado / Pantalla Completa)
1. **Llenado 16:9 de Borde a Borde:**
   - Al reproducir videos en pantalla completa (YouTube, reproductores externos) o girar el teléfono, la imagen debe expandirse a pantalla completa 16:9 en la TV Box.
2. **Erradicación del Fallo de 1/4 de Pantalla:**
   - Queda terminantemente prohibido que la imagen horizontal se reduzca o quede atrapada en un recuadro de 1/4 en la esquina izquierda con el resto en negro.

### C. Leyes Técnicas de Hardware y Renderizado
1. **Configuración de Renderizado en TV Box (`setEnableHardwareScaler(false)`):**
   - `SurfaceViewRenderer` en la TV Box opera con `setEnableHardwareScaler(false)` para permitir que Compose gestione las dimensiones reales del marco mediante `Modifier.fillMaxHeight().aspectRatio(streamAspect)` y evitar fallos de escalado asimétrico.
2. **Contenedor Compose Reactivo a Orientación:**
   - En `TvScreens.kt`, el `AndroidView` debe ser reactivo a `isVertical`:
     - Modo Vertical: `Modifier.fillMaxHeight().aspectRatio(streamAspect)` con `layoutParams = WRAP_CONTENT, MATCH_PARENT`.
     - Modo Horizontal: `Modifier.fillMaxSize()` con `layoutParams = MATCH_PARENT, MATCH_PARENT`.
3. **Conmutación Idempotente con Mando a Distancia:**
   - Pulsar Play/Pausa o DPAD_Center en el control remoto de la TV conmuta limpiamente entre modos de visualización reinicializando traslaciones y zooms espurios, garantizando que el usuario pueda alternar modos infinitamente sin que la imagen se trabe o desplace.
