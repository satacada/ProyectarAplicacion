# 📘 ESPECIFICACIÓN GENERAL DE DISEÑO Y REQUERIMIENTOS: OMNICAST PRO

**Documento Canónico de Diseño y Contrato Funcional del Sistema**  
**Última actualización:** 2026-10-15  
**Estado:** Activo y Vigente  

---

## 1. Visión y Directiva Fundamental del Producto
OmniCast Pro es un ecosistema cliente-servidor (Emisor Móvil y Receptor Android TV) diseñado para la transmisión de pantalla, audio digital, galerías locales y casteo web de alta fidelidad, operando con **cero internet obligatorio (100% LAN local)** y bajo estrictos principios de **Arquitectura Hexagonal**, **Clean Architecture** y **Fail-Fast**.

---

## 2. Los 4 Pilares Arquitectónicos Aislados (Ley Inviolable)
1. **Screen Mirroring & Audio Interno (WebRTC P2P):**
   - Transmisión en tiempo real con latencia < 60ms.
   - Codificación y decodificación por hardware H.264 (`MediaCodec`).
   - Captura de audio digital interno (`AudioPlaybackCaptureConfiguration`).
   - *Ley de Hardware:* Prohibido `changeCaptureFormat` o redimensionamiento en caliente durante la transmisión. La rotación de pantalla fluye nativamente en el `VirtualDisplay`.
2. **Galería Multimedia Local (Fotos Base64 & Videos HTTP 206):**
   - Proyección de fotos vía WebSocket Base64 (~15ms) en reposo de video.
   - Selector universal de galería con swipe continuo y retorno seguro con botón Atrás.
   - Servidor HTTP local embebido (`LocalImageServer`) con soporte de peticiones parciales `HEAD` y `206 Partial Content`.
3. **Navegador Web Integrado con Web Caster & AdBlocker C++:**
   - Interceptación de flujos multimedia HLS (`.m3u8`), MP4 y DASH.
   - Motor AdBlock nativo en C++ (`libadblock-client.so`) y base de datos relacional SQLite (`omnicast_adblock.db`) con reglas dinámicas.
   - Neutralización reactiva de trampas y popups sin secuestro de pantalla durante reproducción de películas.
   - Filtrado estricto de anuncios cortos (<60s) y descarte de videos basura.
4. **Reproductor Autónomo ExoPlayer en TV Receiver:**
   - Recepción directa de flujos web (Handoff) para que el celular quede libre.
   - Sincronización continua de estado de reproducción (`PLAY`, `PAUSE`, `SEEK`, `VOLUME`).
   - Navegación optimizada para control remoto D-Pad y Leanback UI.

---

## 3. Catálogo Integral de Requerimientos Funcionales Aprobados

### A. Gestión de Conexión y Receptores TV (Emisor Móvil)
- **Tarjeta de Selección de TV (`DeviceSelectionCard.kt`):**
  - Caja de entrada manual (IP y Puerto) resaltada con borde verde esmeralda (`#10B981`) cuando está enlazada.
  - La lista inferior de receptores detectados por NSD **excluye automáticamente la TV conectada** para no duplicar información.
  - Tipografía compacta en filas independientes con soporte de **multi-casteo**: checkboxes (`[✓]`) para transmitir simultáneamente a 1, 2 o más TVs, con un receptor principal resaltado.

### B. Listas Guardadas, "Ver Más Tarde" e Historial
- **Persistencia del Progreso Exacto (`lastPositionMs`):**
  - Al pausar o cerrar una película en la TV o móvil, se almacena el segundo exacto.
  - Al volver a abrir la película desde "Ver más tarde", la reproducción reanuda en dicho punto (`▶ Reanudar en XX:XX`).
- **Navegación Visual y Jerarquía en Carpetas:**
  - Icono cerrado `📁` / abierto `📂` al expandir listas.
  - Películas sangradas a la derecha con conector de rama `↳`.
  - Conteo visible de películas guardadas en cabecera.
- **Scrollbar Lateral y Herramientas de Organización:**
  - Barra de desplazamiento vertical estilizada en el margen derecho (`VerticalScrollbar.kt` en móvil y `TvVerticalScrollbar.kt` en TV).
  - Barra de búsqueda reactiva por texto `🔍` con botón de limpieza `✕`.
  - Chips de ordenamiento instantáneo: `📅 Recientes`, `🔤 A - Z`, `▶ En progreso`, `⏳ Antiguos`.
- **Menú Contextual por Pulsación Prolongada:**
  - Móvil: Mantener pulsado el dedo sobre una película abre opciones: *Reproducir en TV*, *Compartir enlace* y *Eliminar*.
  - TV Box: Mantener pulsado OK en el control remoto abre `TvVideoActionDialog`: *Reproducir*, *Compartir enlace (con Código QR en pantalla)*, *Renovar enlace* y *Eliminar*.

### C. Estrategia de Auto-Renovación de Enlaces Expirados y Streams en Vivo (Celular y TV)
- **Persistencia de Origen (`pageUrl` y `headers`):** Tanto en el navegador del celular (`HistoryItem` / `PlaylistManager`) como en la TV (`TvPlaylistItem` / `TvPlaylistManager`), cada video guardado conserva su `url` directa, `pageUrl` (página exacta donde está publicada la película) y las `headers` reales capturadas.
- **Manejo de Vencimiento de Token (En Celular y TV):**
  1. **Intento Directo:** Al tocar "Reproducir" en *"Ver más tarde"* o *"Favoritos"*, se verifica si el flujo `.m3u8`/`.mp4` aún responde `HTTP 200` con sus cabeceras guardadas.
  2. **Auto-Recuperación Local por Permutación y Re-Extracción (`$0` con Objetos/Clases):** Si el token expiró (`HTTP 403 / 404 / 410`), en el celular abre un extractor silencioso sobre `pageUrl` para capturar el nuevo token automáticamente.
  3. **Respaldo Directo a la Página (`pageUrl`) sin Re-búsqueda:** Solo si falla la extracción y reproducción automática, en el celular se abre directamente la `pageUrl` guardada en el navegador (evitando que el usuario tenga que volver a buscar la película), y en la TV se presenta el **Código QR dinámico** hacia `pageUrl` (o re-extracción local asistida), reemplazando el enlace viejo por el nuevo y conservando `lastPositionMs`.
- **Manejo de Streams en Vivo (TV / Deportes 24/7):**
  - Se identifican de forma determinista leyendo el manifiesto `.m3u8` (ausencia de `#EXT-X-ENDLIST` o duración `0.0`).
  - Nunca son descartados por el filtro de duración corta (<60s) ni almacenan minuto de reanudación antiguo; siempre enganchan en el borde en vivo (`seekToDefaultPosition`).

### D. Arquitectura Realista de Costo Cero ($0): Objetos/Clases Deterministas vs. IA Gratuita Acotada
- **Principio de Realismo y Costo $0 Absoluto:**
  El **90% de los problemas** se resuelven con **Objetos y Clases en Kotlin deterministas (0 tokens, 0 latencia, $0 costo)** dentro de la app, sin inventar fuentes en duro (`hardcoded`), reservando la **bolsa gratuita de IA (Gemini Flash Free Tier en GitHub Actions)** exclusivamente para el **10% de casos donde cambia la estructura de un sitio web desconocido**.

| Necesidad del Aplicativo | ¿Requiere IA o Clases Kotlin? | Solución Realista de Costo $0 |
| :--- | :--- | :--- |
| **1. Saber la fuente real (`Referer`/`Origin`) del video** | **Clases Kotlin (`$0`)** | Capturar el `requestHeaders["Referer"]` real de Chromium y el `window.location.href` real del `<iframe>` en JS, **sin pisarlos con listas en duro**. |
| **2. Evitar que enlaces corruptos cierren ExoPlayer y separar Tráiler vs. Película vs. Vivo** | **Clases Kotlin (`$0`)** | **Clase `StreamManifestProbe` (Sonda Pre-Cast):** Descarga los primeros 4 KB del `.m3u8`, verifica que sea video válido (`#EXTM3U`), detecta si es **En Vivo** (sin `#EXT-X-ENDLIST`) y suma `#EXTINF` para etiquetar **Tráiler (<3 min)** vs. **Película (>20 min)**. |
| **3. Reparar error `403 Forbidden` al reproducir en TV o Celular** | **Clases Kotlin (`$0`)** | **Clase `HeaderSelfHealer`:** Prueba en <1s las 4 combinaciones lógicas de cabeceras (Iframe real $\rightarrow$ `pageUrl` $\rightarrow$ Propio CDN $\rightarrow$ Limpio sin Referer) y guarda en memoria local (`learned_rules.json`) la que dé `HTTP 200`. |
| **4. Limpiar títulos sucios de películas guardadas** | **Clases Kotlin (`$0`)** | **Objeto `MediaTitleCleaner`:** Expresiones regulares (Regex) que limpian *"Ver Película X (2024) Online HD Gratis"* $\rightarrow$ *"X (2024)"*. |
| **5. Sitios nuevos con ofuscación compleja que bloquean incluso al `HeaderSelfHealer`** | **IA Gratuita en Nube (`$0`)** | **Único punto con IA (`ai_stream_healer.py`):** Corre en lote cada 6h en GitHub Actions usando la **capa gratuita de Gemini Flash** (1,500 peticiones/día gratis) sobre dominios fallidos en Supabase y publica la regla en `live_stream_rules.json` (OTA). |


