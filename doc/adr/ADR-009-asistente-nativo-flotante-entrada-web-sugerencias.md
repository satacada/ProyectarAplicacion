# ADR-009: Asistente Nativo Flotante de Entrada de Texto Web y Pipeline de Sugerencias en Vivo

**Estado:** Aprobado  
**Fecha:** 2026-10-24  
**Contexto:** WebView Chromium Android, React 18 / Next.js (Dailymotion), SPAs con modales dinámicos, IME Android (Gboard)  
**Versión de Implementación:** v3.4.45  

---

## 1. Contexto y Problemática

A pesar de los centramientos geométricos de scroll (`scrollIntoView` y `window.scrollTo(0,0)`), la entrada de texto dentro de WebView presenta problemas estructurales en interfaces web complejas como Dailymotion:
1. **Ocultamiento de Inputs bajo Modales:** La barra de búsqueda de Dailymotion se renderiza en modales dinámicos que quedan atenuados u ocultos fuera del viewport visible (`top: -16px`) en ciertas orientaciones o dimensiones, generando escritura a ciegas.
2. **Inversión de Caracteres por Desincronización IME:** Los componentes controlados de React que actualizan el Virtual DOM en cada pulsación reinician los punteros de selección a cero (`0, 0`), provocando caracteres invertidos ("aCrro" en lugar de "carro").
3. **Corrupción de Espacios en Rutas URL (`+` vs `%20`):** Al enviar búsquedas mediante formularios codificados con `URLEncoder.encode`, los espacios se transformaban en signos más (`+`), resultando en términos como `carro+deportivo` en lugar de `carro deportivo`.
4. **Falta de Sugerencias Interactivas Visibles:** El usuario no tenía acceso a sugerencias de autocompletado en tiempo real sin tener que lidiar con listas desplegables del DOM tapadas por el teclado de Android.

---

## 2. Decisión Arquitectónica

Se implementa una arquitectura híbrida desacoplada compuesta por:

1. **Asistente Nativo en Jetpack Compose (`WebInputAssistantBar.kt`):**
   - Se despliega una barra nativa flotante anclada dinámicamente con `imePadding()` justo encima del teclado virtual de Android.
   - El teclado interactúa exclusivamente con un `BasicTextField` nativo de Android. Esto otorga al usuario el 100% de la funcionalidad de Gboard (cursor libre, borrado, selección, sugerencias de teclado, dictado por voz) y visualización perfecta del texto escrito.

2. **Preservación de Referencia en DOM (`window.__omniLastFocusedInput`):**
   - Durante el evento `focusin` en JavaScript (`InputCaretStabilizer.kt`), se almacena de forma inmutable la referencia al elemento activo en `window.__omniLastFocusedInput`.
   - Cuando el usuario toca el campo nativo de Compose y el WebView pierde el foco nativo de Chromium, la referencia al elemento web permanece intacta para inyecciones posteriores.

3. **Inyección Pasiva y Letra por Letra:**
   - Cada pulsación en Compose despacha el valor al WebView invocando `HTMLInputElement.prototype` setter nativo de React, seguido de eventos sintéticos `input` y `change`.
   - Al ser una inyección de sólo salida hacia el DOM, el teclado nunca sufre reinicios de sesión ni inversión de caret.

4. **Codificación Canónica de Espacios (`Uri.encode`):**
   - Se sustituye `URLEncoder.encode` por `android.net.Uri.encode(q)`, garantizando que los espacios se codifiquen como `%20` y se mantengan limpios en las rutas canónicas de búsqueda de Dailymotion (`/search/{query}`) y buscadores web.

5. **Pipeline Dual de Sugerencias en Vivo (Chips Interactivos):**
   - `WebInputAssistantManager` consulta de manera asíncrona y con debounce (200ms) sugerencias en tiempo real desde la API oficial de títulos de Dailymotion y la API de autocompletado universal.
   - Las sugerencias se renderizan como chips táctiles horizontales (`LazyRow`). Al presionar cualquier chip, el término se completa y se dispara la búsqueda inmediatamente.

---

## 3. Consecuencias

- **Positivas:**
  - Erradicación definitiva de la escritura a ciegas en cualquier sitio web o modal.
  - Cero riesgo de inversión de caracteres: la digitación es 100% nativa de Android.
  - Búsqueda con espacios limpios y navegación directa sin bloqueos de formularios React.
  - Acceso inmediato a sugerencias de video y términos relevantes con un solo toque.
- **Límites y Mantenimiento:**
  - Mantener `WebInputAssistantBar.kt` e `InputCaretStabilizer.kt` bajo el límite estricto de 300 líneas de código (AGENTS.md Regla 3).
