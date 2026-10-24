# ADR-008: Estabilización de Caret en React y Scroll Centrado de Búsqueda en WebView

**Estado:** Aprobado  
**Fecha:** 2026-10-23  
**Contexto:** Chromium Android WebView, React 18 / Next.js (Dailymotion), Teclados Virtuales (IME)  
**Versión de Implementación:** v3.4.40  

---

## 1. Contexto y Problemática

En versiones recientes, al ingresar a Dailymotion (`dailymotion.com`) o páginas con interfaces SPA modernas:
1. Al tocar el botón de búsqueda (icono de lupa), la interfaz de Dailymotion oscurece el fondo y levanta un diálogo modal cuyo `<input id="search">` se renderiza en una coordenada fuera del área visible inicial (`top = -16px`). Esto ocurre porque al deslizar la página hacia abajo, el encabezado sticky se contrae/oculta offscreen.
2. El teclado virtual de Android (IME) se despliega sobre la mitad inferior de la pantalla. Un simple `scrollIntoView()` sobre un elemento dentro de un contenedor sticky o con `top < 0` no desplaza la ventana principal. El usuario se encontraba ante el teclado abierto pero sin caja de texto visible ("escritura a ciegas").
3. Al intentar corregir la inversión de texto interceptando `HTMLInputElement.prototype.value` en v3.4.39, se quebró el mecanismo interno `_valueTracker` de React. Como consecuencia, React dejó de procesar eventos `onChange` y reseteaba de inmediato el input a su valor original, **impidiendo cualquier edición, borrado o modificación de texto** ("no permite editar el cuadro de búsqueda").

---

## 2. Decisiones Arquitectónicas (v3.4.40)

1. **Despliegue Inmediato de Encabezado Sticky con `window.scrollTo(0, 0)`:**
   - Ante el evento `focusin`, si `window.scrollY > 0` o el elemento enfocado tiene `rect.top < 0`:
     - Se ejecuta `window.scrollTo(0, 0)` inmediatamente. Esto expande por completo el encabezado sticky contraído de Dailymotion.
     - Seguidamente, se ejecuta `target.scrollIntoView({ block: 'center', inline: 'nearest', behavior: 'auto' })` a los 80ms, asegurando que el input y el cursor queden 100% visibles centrados sobre el teclado.

2. **Cero Manipulación de Prototipos (`prototype.value` Intacto):**
   - **Queda terminantemente prohibido** redefinir `HTMLInputElement.prototype.value` o instalar callbacks en bucle `requestAnimationFrame`. React mantiene acceso nativo a sus getters y setters, permitiendo editar, borrar con backspace y seleccionar texto sin que React descarte los cambios.

3. **Guardia Anti-Inversión Selectivo en Inserción:**
   - La corrección de caret actúa exclusivamente si `e.inputType && e.inputType.indexOf('insert') === 0 && !e.isComposing`.
   - Si tras una inserción el cursor colapsa accidentalmente a `(0, 0)` teniendo longitud de texto $>1$, se reubica al final (`v.length, v.length`).
   - Esta condición **nunca** se ejecuta durante borrado (`deleteContentBackward`), clics de reposicionamiento ni selecciones de texto, preservando la capacidad total de edición.

4. **Telemetría Transparente (`OmniInputBridge`):**
   - Se mantiene la interfaz Javascript `@JavascriptInterface` para auditar en tiempo real a través de Logcat (`tag = OmniInput`) los valores de posición, foco y correcciones de cursor.

---

## 3. Consecuencias y Garantías

- **Positivas:**
  - El cuadro de búsqueda de Dailymotion emerge visible de inmediato al tocar la lupa, erradicando la escritura a ciegas.
  - La edición, borrado y modificación de texto funciona con total libertad y respuesta nativa de React y Chromium.
  - La escritura inversa queda neutralizada durante la inserción sin alterar el flujo de edición.
- **Negativas/Límites:**
  - Requiere inyección en `onPageStarted` y `onPageFinished`.
