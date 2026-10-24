# ADR-008: Estabilización de Caret en React y Scroll Centrado de Búsqueda en WebView

**Estado:** Aprobado  
**Fecha:** 2026-10-23  
**Contexto:** Chromium Android WebView, React 18 / Next.js (Dailymotion), Teclados Virtuales (IME)  
**Versión de Implementación:** v3.4.39  

---

## 1. Contexto y Problemática

En versiones recientes, al ingresar a Dailymotion (`dailymotion.com`) o páginas con interfaces SPA modernas:
1. Al tocar el botón de búsqueda (icono de lupa), la interfaz de Dailymotion oscurece el fondo y levanta un diálogo modal cuyo `<input id="search">` se renderiza en una coordenada fuera del área visible inicial (`top = -16px`).
2. El teclado virtual de Android (IME) se despliega sobre la mitad inferior de la pantalla. Al usar un scroll `nearest`, Chromium sólo movía el input 16 píxeles, manteniéndolo oculto detrás de la barra de herramientas de OmniCast (`top = 0px`). El usuario se encontraba ante una pantalla atenuada con teclado abierto pero sin caja de texto visible ("escritura a ciegas").
3. Al escribir texto a ciegas (ej. "carro") y pulsar Buscar, el texto resultante aparecía invertido ("aCrro" o "sorraC"). Esto ocurre porque Dailymotion implementa inputs controlados por React: cada evento `input` actualiza el estado de React y reasigna `input.value = "c"`. En Chromium WebView, dicha asignación reinicia `selectionStart` y `selectionEnd` a `0`. Las pulsaciones subsiguientes se insertaban al inicio del campo.

---

## 2. Decisiones Arquitectónicas

1. **Centrado Suave en Focusin (`block: 'center'`):**
   - Ante el evento `focusin`, se programa un desplazamiento suave (`behavior: 'smooth'`) con alineación `block: 'center'` tras un retardo de 120ms (tiempo necesario para que el teclado virtual complete su animación de despliegue y calcule el nuevo `innerHeight` del WebView).
   - Esto sitúa el cuadro de búsqueda directamente en el centro libre de la pantalla visible, plenamente visible para escribir y editar.

2. **Intercepción del Setter `HTMLInputElement.prototype.value`:**
   - Se intercepta el descriptor de propiedad en `HTMLInputElement.prototype`:
     ```javascript
     var desc = Object.getOwnPropertyDescriptor(HTMLInputElement.prototype, 'value');
     var originalSet = desc.set;
     desc.set = function(val) {
         var isFocused = (document.activeElement === this);
         var oldLen = this.value ? this.value.length : 0;
         originalSet.call(this, val);
         if (isFocused && typeof val === 'string' && val.length > oldLen) {
             if (this.selectionStart === 0 && this.selectionEnd === 0) {
                 this.setSelectionRange(val.length, val.length);
             }
         }
     };
     Object.defineProperty(HTMLInputElement.prototype, 'value', desc);
     ```
   - Cuando React actualiza el valor del input enfocado y Chromium intenta colapsar la selección a 0, el interceptor la reposiciona inmediatamente a `val.length`.

3. **Anclaje en `requestAnimationFrame` en Listener de `input`:**
   - Adicionalmente, ante cada evento `input`, se programa una comprobación en el siguiente ciclo de renderizado (`requestAnimationFrame`) para asegurar que si el cursor quedó en `(0, 0)` tras la composición de teclado, se restaure al final.

4. **Telemetría Transparente (`OmniInputBridge`):**
   - Se mantiene la interfaz Javascript `@JavascriptInterface` para auditar en tiempo real a través de Logcat (`tag = OmniInput`) los valores de posición, foco y correcciones de cursor.

---

## 3. Consecuencias y Garantías

- **Positivas:**
  - El cuadro de búsqueda de Dailymotion y Google se vuelve visible de inmediato al pulsar la lupa.
  - La inversión de caracteres queda 100% erradicada tanto en inputs nativos como en componentes React controlados.
  - No hay parpadeos de cursor ni conflictos con el sistema IME de Android.
- **Negativas/Límites:**
  - Requiere inyección temprana de Javascript en `onPageStarted` y `onPageFinished`.
