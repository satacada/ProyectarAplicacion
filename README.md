# 📺 OmniCast Pro — Proyección Universal y Web Video Caster (Móvil a TV Box)

> **Solución Definitiva de Proyección de Pantalla, Navegador Web con AdBlocker Nativo y Receptor Autónomo para Android TV**  
> Proyecta **cualquier contenido** desde tu teléfono Android hacia tu TV Box (Android TV / AOSP / Google TV / Fire TV): Screen Mirroring de baja latencia (<60ms), transmisión directa de películas web (PelisPlus, OK.ru, PelisGO, YouTube) sin consumo de batería en el móvil y sin proxy local.

**Versión Canónica de Producción:** `v3.5.21` (Sender: 272 | Receiver: 255) — Octubre 2026.

---

## 🌟 Los 4 Pilares del Sistema

1. **Screen Mirroring de Baja Latencia (<60ms):**
   - Transmisión en tiempo real vía **WebRTC P2P** con codificación y decodificación por hardware (`MediaCodec` H.264).
   - Audio interno digital estéreo a 48kHz (`AudioPlaybackCapture`).
   - Dos modalidades exclusivas e independientes: **Pantalla Completa** (100% altura TV con factor 1.075f anti-barras) y **Por Aplicación** (lienzo libre 16:9 con rotación dinámica y zoom gradual 75%–130%).

2. **Navegador Web con Triple Blindaje de Publicidad:**
   - **Motor C++ Nativo:** Filtrado en tiempo real con Bloom Filters y árboles Trie evaluando millones de reglas EasyList/uBlock Origin en 0ms.
   - **Supresión Nativa de Popups:** `onCreateWindow` retorna `false` nativo, previniendo saturación de memoria RAM y crashes `OutOfMemoryError`.
   - **Interceptor de Red:** Bloqueo de redirecciones y dominios de trackers/malware con respuesta vacía instantánea.

3. **Captura Universal de Streams (Arquitectura Hexagonal & Lista Plana WVC):**
   - **Sniffer JavaScript Inyectado (`universal_sniffer.js`):** Intercepta llamadas `fetch`, `XHR` y eventos `HTMLMediaElement.play` en tiempo real.
   - **Lista Plana 1:1 Estándar Web Video Caster:** Muestra transparentemente cada variante legítima (`.m3u8`, `.mpd`, `.mp4`) sin agrupar en tarjetas colapsadas que inventen calidades inexistentes ni corrompan URLs con `.urlset`.
   - **Extractores Aislados (Principio "Poner y Sacar"):** Soporte desacoplado para OK.ru (DASH y calidades múltiples), Fastream, LuluStream, YouTube y Dailymotion.

4. **Receptor Autónomo TV Box (Enrutamiento Híbrido Dual Cero-Proxy):**
   - **ExoPlayer por Hardware:** Motor primario para MP4 directos, IPTV, emisiones en vivo y DASH (`.mpd` de OK.ru).
   - **TvWebStreamScreen (HTML5 + Hls.js):** Motor especializado para CDNs protegidas por tokens HMAC y anti-scraping (DropCDN / PelisPlus, Fastream).
   - **Interceptor Nativo Anti-CORS (`TvWebResourceInterceptor`):** Descarga manifiestos y fragmentos preservando el `Referer` canónico de embed (`https://dr0pstream.com/e/<id>`) y User-Agent móvil, inyectando `Access-Control-Allow-Origin: *` para erradicar cualquier error 403 o bloqueo CORS.

---

## 📑 Documentación Canónica del Proyecto

Para que cualquier desarrollador humano o agente de Inteligencia Artificial comprenda y mantenga el proyecto sin provocar regresiones, consulte los siguientes documentos de referencia:

1. 🤖 **[Directrices Técnicas e Inviolables (`AGENTS.md`)](file:///home/sataca/Descargas/APLICACIONES/proyectarAplicacion/AGENTS.md):**  
   Leyes de hardware, reglas de no-regresión, checklist pre-vuelo obligatorio y arquitectura hexagonal.

2. 🏛️ **[Arquitectura Global del Sistema (`doc/ARCHITECTURE.md`)](file:///home/sataca/Descargas/APLICACIONES/proyectarAplicacion/doc/ARCHITECTURE.md):**  
   Diagramas de contenedores C4, los 4 escudos del navegador, flujo hexagonal de 6 ramas y ciclo de vida TV Box.

3. 📊 **[Tablero de Control y Estado Vivo (`doc/PROJECT_DASHBOARD.md`)](file:///home/sataca/Descargas/APLICACIONES/proyectarAplicacion/doc/PROJECT_DASHBOARD.md):**  
   Bitácora viva de versiones, telemetría de TV, incidencias resueltas y lecciones aprendidas.

4. 📋 **[Catálogo de Variantes de Streaming (`doc/referencias/CATALOGO_VARIANTES_STREAMING.md`)](file:///home/sataca/Descargas/APLICACIONES/proyectarAplicacion/doc/referencias/CATALOGO_VARIANTES_STREAMING.md):**  
   Base de conocimiento técnico sobre tokens efímeros, firmas TLS, matriz de CDNs (DropCDN, Fastream, OK.ru) y asignación de reproductores.

5. 📜 **[Registro de Decisiones de Arquitectura (`doc/adr/`)](file:///home/sataca/Descargas/APLICACIONES/proyectarAplicacion/doc/adr/):**  
   - [ADR-007: Receptor Web HTML5 (Chromium / Hls.js) y Resiliencia CDN](file:///home/sataca/Descargas/APLICACIONES/proyectarAplicacion/doc/adr/ADR-007-receptor-web-html5-tv-hlsjs-resiliencia-cdn.md)
   - [ADR-010: Supresión Nativa de Popups Anti-OOM y Control Dual D-Pad](file:///home/sataca/Descargas/APLICACIONES/proyectarAplicacion/doc/adr/ADR-010-supresion-nativa-popups-anti-oom-control-dual-dpad.md)
   - [ADR-012: Filtro Centralizado de Higiene de Red y Purificación Hexagonal](file:///home/sataca/Descargas/APLICACIONES/proyectarAplicacion/doc/adr/ADR-012-filtro-centralizado-higiene-streams-y-adaptadores-limpios.md)
   - [ADR-013: Coexistencia DropCDN/OK.ru, Lista Plana y Blindaje de Cabeceras en TV](file:///home/sataca/Descargas/APLICACIONES/proyectarAplicacion/doc/adr/ADR-013-coexistencia-dropcdn-okru-y-estandar-lista-plana.md)

6. 🎯 **[Historias de Usuario y Contratos Inmutables (`doc/historias-usuario/`)](file:///home/sataca/Descargas/APLICACIONES/proyectarAplicacion/doc/historias-usuario/):**  
   - [HU-005: Detección Hexagonal y Principio "Poner y Sacar"](file:///home/sataca/Descargas/APLICACIONES/proyectarAplicacion/doc/historias-usuario/HU-005-extraccion-hexagonal-streams.md)
   - [HU-012: Receptor Web HTML5 y Resiliencia ante CDNs Protegidas](file:///home/sataca/Descargas/APLICACIONES/proyectarAplicacion/doc/historias-usuario/HU-012-receptor-web-html5-resiliencia-cdn-tv.md)
   - [HU-013: Screen Mirroring (Pantalla Completa vs Por Aplicación)](file:///home/sataca/Descargas/APLICACIONES/proyectarAplicacion/doc/historias-usuario/HU-013-screen-mirroring-proyeccion-pantalla-completa.md)

---

## 🛠️ Matriz de Puertos y Servicios en Red Local

| Puerto | Protocolo | Componente | Descripción |
|---|---|---|---|
| `:8088` | WebSocket / TCP | TV Box | Servidor de señalización P2P, comandos de reproducción remota y sincronización de estado. |
| `:8087` | HTTP / TCP | TV Box | Servidor de telemetría y logs en tiempo real (`http://<ip-tv>:8087/logs`). |
| `:8087` | HTTP / TCP | Emisor Móvil | Servidor multimedia local para fotos y videos de la galería con soporte Range 206. |
| Dinámico | UDP / mDNS | Ambos | Descubrimiento de red local cero-configuración (`_omnicast._tcp`). |
