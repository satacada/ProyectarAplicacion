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

### C. Estrategia de Auto-Renovación de Enlaces Expirados en TV (HU-006 / ADR-003)
- **Problema Abordado:** Enlaces de video que caducan en la web (tokens temporales HTTP 403 / 404 de Fastream, Streamtape, etc.).
- **Directiva de Criterio Humano (Anti-Videos Basura):** Se descarta la resolución automática ciega por WebView en la TV para evitar la captura accidental de anuncios preroll de 15s o selección errónea de idioma.
- **Mecanismo por Código QR:**
  1. Si `ExoPlayer` en la TV detecta error HTTP 403 / 404 al reproducir un elemento guardado, despliega un diálogo modal amigable con un **Código QR dinámico** que apunta a la `pageUrl` original de la película con la posición guardada.
  2. El usuario escanea el QR con su teléfono, abriendo OmniCast en la web de la película.
  3. El usuario selecciona el reproductor e idioma correcto y presiona *"Transmitir a TV"*.
- **Sustitución Automática del Enlace en TV (Aprovechamiento de TTL):**
  - La TV recibe el nuevo flujo y **reemplaza la URL vieja caducada por la nueva URL activa** en su base de datos (`TvPlaylistsManager`), preservando el minuto exacto `lastPositionMs`, el título y la carpeta.
  - Durante la vigencia del nuevo token (6 a 12 horas), el usuario puede salir, apagar la TV y volver más tarde, y la película se reproducirá de inmediato **sin volver a pedir el código QR**.
- **Opción Manual:** En el menú contextual de la TV (`TvVideoActionDialog`), el botón `🔄 Renovar enlace (QR)` permite generar el código QR en cualquier momento.

### D. Arquitectura Colectiva de Auto-Recuperación y Auto-Curación por IA (OTA)
- **Problema Abordado:** Los sitios de streaming y servidores CDN (ej. Lulustream, Fastream, Streamwish) cambian periódicamente sus cabeceras anti-hotlink (`Referer`, `Origin`, tokens de embed) provocando errores HTTP 403 Forbidden. Codificar parches manuales por cada sitio viola los principios de escalabilidad.
- **Directiva de Costo Cero ($0) e Inteligencia Colectiva:**
  1. No se invoca la IA en el teléfono del usuario ni se requiere que cada usuario configure claves de API.
  2. La inteligencia es centralizada: una única inferencia en la nube resuelve el problema para **todos los usuarios del mundo**.
- **Componentes del Ecosistema:**
  1. **Motor Heurístico On-Device (`StreamIntelligenceEngine.kt`):**
     - Discrimina dinámicamente entre flujos legítimos y anuncios según duración (<60s es penalizado), tipo de manifiesto (HLS/DASH) y resolución.
     - Resuelve y asocia en caliente las cabeceras requeridas (`Referer`, `Origin`, `Sec-Fetch-*`) sin recompilar la app.
  2. **Sincronizador Remoto y Telemetría (`StreamRuleSyncManager.kt`):**
     - Descarga en segundo plano las reglas vivas desde `rules/live_stream_rules.json` en GitHub.
     - Ante cualquier flujo desconocido o bloqueado con 403, despacha un reporte anónimo silencioso a Supabase (`UNRESOLVED_STREAM`).
  3. **Auto-Sanador en la Nube (`scripts/ai_stream_healer.py` & `.github/workflows/ai_stream_healer.yml`):**
     - Se ejecuta automáticamente cada 4 horas en GitHub Actions.
     - Lee los reportes de Supabase y consulta a **Google Gemini 1.5 Flash** (usando el secret `GEMINI_API_KEY`).
     - Gemini deduce la combinación exacta de CDN, Referer de embed y cabeceras necesarias.
     - El workflow actualiza `rules/live_stream_rules.json` y hace commit directo a la rama `main`.
  4. **Distribución Instantánea (Over The Air - OTA):**
     - Todas las aplicaciones móviles en uso descargan el nuevo archivo JSON (~5 KB) en su siguiente inicio o navegación, restaurando la reproducción inmediatamente sin intervención del usuario.

