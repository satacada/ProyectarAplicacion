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

1. **Centrado Inmediato y Despliegue de Header Sticky (`InputCaretStabilizer.kt`):**
   - Ante el evento `focusin` en cualquier `<input>`, `<textarea>`, elemento con `role="search"` o `isContentEditable`:
     - Si `window.scrollY > 0` o `rect.top < 0`, se ejecuta `window.scrollTo(0, 0)` para des-colapsar y expandir inmediatamente los encabezados sticky/fijos (como Dailymotion) que se esconden al navegar.
     - Se ejecuta `target.scrollIntoView({ block: 'center', inline: 'nearest', behavior: 'auto' })` con retardo estabilizado de 80ms, asegurando visibilidad inmediata sobre el teclado virtual.

2. **Edición Total y No Invasión de Prototipos en React:**
   - **Queda terminantemente prohibido** redefinir `HTMLInputElement.prototype.value` mediante `Object.defineProperty` o usar bucles de `requestAnimationFrame` que fuercen el cursor en cada renderizado. Dicha intercepción rompe el sistema `_valueTracker` de React y bloquea por completo la edición, borrado y modificación de texto.
   - **Guardia Anti-Inversión Específico en Inserción:** La estabilización de caret actúa EXCLUSIVAMENTE ante eventos de inserción de texto (`e.inputType.indexOf('insert') === 0 && !e.isComposing`). Si el cursor colapsa accidentalmente a `(0, 0)`, se restaura al final (`v.length, v.length`). Nunca interviene en borrado (`backspace`), selecciones ni reposicionamiento del cursor por el usuario.

3. **Inmunidad de la Navegación y Formularios:**
   - La estabilización es pasiva y reactiva; no utiliza `MutationObserver` ni bucles `setInterval`.
   - No altera formularios, botones de envío ni listeners nativos del sitio web.

4. **Asistente Flotante Nativo de Entrada de Texto (`WebInputAssistantBar.kt` - v3.4.43 a v3.4.45):**
   - Para erradicar definitivamente la escritura a ciegas en cualquier pantalla o web con layouts complejos:
     - Al detectar foco en cualquier campo de entrada web, se despliega una barra flotante en Compose nativo posicionada exactamente sobre el teclado virtual (`imePadding()`).
     - El usuario visualiza de forma 100% nítida el texto que escribe, con cursor desplazable, borrado nativo y autocompletado de teclado.
     - **Preservación de Referencia (`window.__omniLastFocusedInput`):** El elemento enfocado se almacena en el DOM durante `focusin`, permitiendo que la sincronización letra por letra inyecte el valor al componente web sin perder la referencia cuando Compose adquiere el foco.
     - **Codificación Limpia de Espacios (%20):** Se prohíbe el uso de `URLEncoder.encode` (que genera signos `+` incompatibles con rutas URL de Dailymotion); se utiliza estrictamente `Uri.encode` asegurando espacios naturales `%20`.
     - **Chips Nativos de Sugerencias en Vivo (v3.4.45):** Debajo de la barra de texto se presenta una lista horizontal desplazable con sugerencias en tiempo real consultadas de la API oficial de Dailymotion y el motor universal de búsqueda. Tocar cualquier sugerencia autocompleta el texto y ejecuta la búsqueda en un solo paso.
     - **Ejecución Directa de Búsqueda:** Al presionar Buscar o presionar Enter, en Dailymotion se navega directamente a la URL canónica (`/search/{query}`) para garantizar la búsqueda inmediata en SPAs con formularios controlados.

5. **Reglas de Hardware y Límites de Código:**
   - Mantener los archivos `WebInputAssistantBar.kt` e `InputCaretStabilizer.kt` estrictamente $\le 300$ líneas de código cada uno.
   - Prohibido modificar `TvScreens.kt` o `WebRtcReceiverManager.kt` para resolver problemas del navegador web (Ley Inviolable 8 de `AGENTS.md`).
