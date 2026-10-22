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

### A. Proyección Vertical (Celular Erguido)
1. **100% de Altura Física en TV:**
   - La imagen vertical del teléfono debe tocar exactamente los bordes físicos superior e inferior del televisor (1080p nativo).
   - Queda estrictamente prohibido que aparezcan franjas negras arriba o abajo que reduzcan la altura de la imagen.
2. **Centrado Simétrico:**
   - La columna vertical del celular debe estar perfectamente centrada en el eje horizontal de la TV, con barras negras laterales simétricas propias de la relación de aspecto del teléfono (ej. 9:20 o 9:19.5).
3. **Sin Deformación Lateral:**
   - Queda terminantemente prohibido estirar la imagen vertical horizontalmente para llenar un televisor 16:9 (prohibido aplicar escalas desproporcionadas como 3.95x).

### B. Proyección Horizontal (Celular Apaisado / Pantalla Completa)
1. **Llenado 16:9 de Borde a Borde:**
   - Al reproducir videos en pantalla completa (YouTube, reproductores externos) o girar el teléfono, la imagen debe expandirse a pantalla completa 16:9 en la TV Box.
2. **Erradicación del Fallo de 1/4 de Pantalla:**
   - Queda terminantemente prohibido que la imagen horizontal se reduzca o quede atrapada en un recuadro de 1/4 en la esquina izquierda con el resto en negro.

### C. Leyes Técnicas de Hardware y Renderizado
1. **Escalador de Hardware Obligatorio (`setEnableHardwareScaler(true)`):**
   - `SurfaceViewRenderer` en la TV Box DEBE mantener siempre activo el escalador por hardware de Android TV (`setEnableHardwareScaler(true)`). Desactivarlo bloquea el buffer de superficie en la resolución inicial e introduce el fallo de 1/4.
2. **Contenedor Compose a Pantalla Completa:**
   - En `TvScreens.kt`, el `AndroidView` debe configurarse con `Modifier.fillMaxSize()` y `layoutParams = MATCH_PARENT, MATCH_PARENT`.
3. **Conmutación Idempotente con Mando a Distancia:**
   - Pulsar Play/Pausa o DPAD_Center en el control remoto de la TV conmuta limpiamente entre modos de visualización reinicializando traslaciones y zooms espurios, garantizando que el usuario pueda alternar modos infinitamente sin que la imagen se trabe o desplace.
