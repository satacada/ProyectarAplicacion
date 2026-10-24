# HU-016: Estabilizador Universal de Visibilidad de Búsqueda y Caret React

**Épica:** Experiencia de Usuario en Navegador Web, Interacción Táctil e IME  
**Versión de Introducción:** v3.4.39  
**Estado:** 🟢 Implementado / Validado  

---

## 1. Descripción de la Historia

**Como** usuario del explorador web integrado de OmniCast,  
**Quiero** que al tocar el icono de búsqueda (lupa) o cualquier campo de texto en sitios web modernos como Dailymotion o Google, el cuadro de búsqueda se posicione de inmediato de forma visible y centrada por encima del teclado virtual de Android, y que al tipear el texto fluya normalmente de izquierda a derecha sin invertirse ni escribir a ciegas ("sorraC" / "aCrro" -> "carro"),  
**Para** poder buscar, ver en tiempo real lo que tecleo, editar, borrar y navegar con total fluidez táctil y comodidad.

---

## 2. Contexto del Problema y Causa Raíz Detectada

1. **Escritura a Ciegas (Cuadro de Búsqueda Invisible):**
   - En Dailymotion (y SPAs similares), la barra de búsqueda modal se monta dinámicamente en una posición fuera del viewport superior (`top: -16px`).
   - El uso de `scrollIntoView({ block: 'nearest' })` solo desplazaba el elemento una distancia mínima (16px), dejándolo anclado en `top: 0px` oculto detrás de la barra superior del explorador OmniCast.
   - Como resultado, el teclado virtual cubría el contenido, la pantalla se atenuaba y el usuario no podía ver el cuadro de texto ni el cursor, viéndose obligado a tipear a ciegas.

2. **Inversión de Caracteres en Componentes Controlados (React / Next.js):**
   - Dailymotion está construido con React/Next.js. En un componente controlado, cada pulsación de tecla (`input`) actualiza el estado interno de React, disparando una reconciliación del DOM virtual que ejecuta:
     ```javascript
     input.value = "c";
     ```
   - En el motor Chromium de Android WebView, la asignación directa de `input.value` reinicia automáticamente los punteros de selección (`selectionStart` y `selectionEnd`) a `0`.
   - Cuando el usuario presiona la siguiente tecla ('a'), Chromium la inserta en la posición `0`, produciendo "ac" en lugar de "ca", y finalmente "aCrro" en lugar de "carro".

---

## 3. Criterios de Aceptación (Contrato Inamovible)

1. **Centrado Inmediato de Búsqueda sobre el Teclado (`InputCaretStabilizer.kt`):**
   - Ante el evento `focusin` en cualquier `<input>`, `<textarea>`, elemento con `role="search"` o `isContentEditable`:
     - Se ejecuta `target.scrollIntoView({ block: 'center', behavior: 'smooth' })` con un retardo estabilizado de 120ms.
     - Queda **terminantemente prohibido** usar `block: 'nearest'` o suprimir este desplazamiento, ya que garantiza que el cuadro de búsqueda emerja hacia el centro visible despejado del viewport sobre el teclado.

2. **Estabilización de Caret React sin Inversión de Texto:**
   - **Intercepción del Setter de Prototipo:** Se intercepta el descriptor de propiedad `Object.getOwnPropertyDescriptor(HTMLInputElement.prototype, 'value')`. Si el campo está enfocado y React reasigna un nuevo valor (`originalSet.call(this, val)`), se verifica si la selección colapsó a `(0, 0)`; de ser así, se restaura inmediatamente al final mediante `this.setSelectionRange(val.length, val.length)`.
   - **Anclaje en requestAnimationFrame:** En el listener de evento `input`, se programa un micro-callback en `requestAnimationFrame` que asegura que el cursor permanezca en la longitud actual del valor (`setSelectionRange(len, len)`), garantizando compatibilidad con teclados virtuales (Gboard, Samsung, SwiftKey).

3. **Inmunidad de la Navegación y No Invasión de Formularios:**
   - La estabilización es pasiva y reactiva a eventos del usuario; no utiliza `MutationObserver` ni bucles `setInterval`.
   - No altera formularios, botones de envío ni listeners nativos del sitio web.

4. **Reglas de Hardware y Límites de Código:**
   - Mantener el archivo `InputCaretStabilizer.kt` estrictamente $\le 300$ líneas de código.
   - Prohibido modificar `TvScreens.kt` o `WebRtcReceiverManager.kt` para resolver problemas del navegador web (Ley Inviolable 8 de `AGENTS.md`).
