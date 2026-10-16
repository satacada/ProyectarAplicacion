# CONTEXTO DEL PROYECTO Y ARQUITECTURA DE COSTO CERO ($0) — OMNICAST PRO

> **Documento de Contexto General para Humanos e IA:**
> Este archivo resume la identidad, los módulos, la arquitectura realista de Costo Cero (`$0`) y el catálogo completo de documentos del proyecto **OmniCast Pro (`proyectarAplicacion`)**.

---

## 1. Identidad y Misión del Proyecto
- **Propósito:** Ecosistema nativo Android (Emisor Móvil `:app-sender` + Receptor Android TV `:app-receiver` + Núcleo Hexagonal `:core`) para Screen Mirroring WebRTC de baja latencia (<60ms), proyección de galerías locales y casteo web directo (HLS/MP4/DASH) con bloqueador de anuncios C++ y reproducción autónoma en TV.
- **Stack Tecnológico Principal:** Kotlin, Jetpack Compose (Mobile & TV Leanback), WebRTC (`MediaCodec` H.264), Media3 ExoPlayer, C++ JNI (`AdBlocker`), Ktor/WebSockets LAN.
- **Comando Oráculo de Compilación:** `./gradlew clean build` (o `./gradlew assembleRelease testReleaseUnitTest`).

---

## 2. Propuesta Realista y Acotada de Costo Cero ($0): Clases Deterministas vs. IA Gratuita

Para garantizar **costo `$0` perpetuo** sin pagar suscripciones ni APIs de IA, el sistema reemplaza el uso innecesario de IA en tiempo real por **Objetos y Clases Kotlin deterministas** dentro de la app (que resuelven el **90% de los escenarios en milisegundos y gratis**), dejando la **bolsa gratuita de IA (Google Gemini Flash Free Tier en GitHub Actions)** únicamente para el **10% de casos extremos en segundo plano**:

### A. Lo que se resuelve 100% con Objetos y Clases Kotlin (`$0`, Cero Tokens, Instantáneo)
1. **Captura Fiel de la Fuente Real (Cero "Cosas en Duro"):**
   - **Problema:** Inventar fuentes en duro (`dropcdn.io -> fastream.to`) rompe la reproducción cuando otro servidor usa el mismo CDN.
   - **Solución con Clases:** Capturar siempre el `requestHeaders["Referer"]` real de Chromium (`shouldInterceptRequest`) y el `window.location.href` del `<iframe>` reproductor en JavaScript (`WvcDomSniffer`), pasándolo intacto a la TV sin pisarlo con `if-else` en duro.
2. **Sonda Pre-Cast de Manifiestos (`StreamManifestProbe`):**
   - **Problema:** Se detectan 2 o 3 enlaces en la web (un tráiler, un enlace corrupto que cierra ExoPlayer y la película real).
   - **Solución con Clases:** Antes de mostrar el enlace o enviarlo a ExoPlayer, una clase Kotlin descarga los primeros 4 KB del `.m3u8`:
     - Si no es un manifiesto válido (`#EXTM3U`) o devuelve error irremediable $\rightarrow$ **Se descarta automáticamente (evita el cierre de ExoPlayer).**
     - Si **no** tiene `#EXT-X-ENDLIST` $\rightarrow$ Se clasifica como **Stream en Vivo (`LIVE`)** (no se descarta por duración y siempre arranca en el borde en vivo).
     - Suma los `#EXTINF`: si dura `< 3 min` lo etiqueta como **"Tráiler / Clip"**; si dura `> 20 min` lo marca como **"Película Principal ⭐"**.
3. **Auto-Reparador Local de Error 403 (`HeaderSelfHealer`):**
   - **Problema:** Un servidor CDN rechaza el enlace con `HTTP 403`.
   - **Solución con Clases:** En menos de 1 segundo, un objeto Kotlin prueba las 4 permutaciones lógicas reales:
     1) `Referer`/`Origin` del `<iframe>` que pidió el video.
     2) `Referer`/`Origin` de la página principal (`pageUrl`).
     3) `Referer` del propio dominio del CDN.
     4) Petición limpia sin `Referer`/`Origin`.
     - La combinación que responda `HTTP 200` se guarda en una memoria local (`learned_rules.json`) en el celular y en la TV para no volver a fallar.
4. **Vencimiento de Token en "Ver más tarde / Favoritos" (Celular y TV):**
   - **Problema:** Los enlaces `.m3u8` guardados vencen a las pocas horas.
   - **Solución con Clases:**
     - Siempre se guarda `pageUrl` (la página exacta de la película) tanto en el celular (`PlaylistManager`) como en la TV (`TvPlaylistManager`).
     - Si al darle Play el token ya venció (`HTTP 403/404/410`), primero intenta auto-extraer el nuevo flujo desde `pageUrl` en segundo plano.
     - **Respaldo sin re-búsqueda:** Solo si falla la extracción y reproducción automática, abre directamente la `pageUrl` guardada para que el usuario no tenga que volver a buscar la película.
5. **Limpiador de Títulos (`MediaTitleCleaner`):**
   - **Solución con Clases:** Expresiones regulares (Regex) que limpian *"Ver Película X (2024) Online Gratis HD"* $\rightarrow$ *"X (2024)"*.

### B. El Único Punto Crítico donde se usa la Bolsa Gratuita de IA (`$0` en Lote)
- **Auto-Sanador Colectivo OTA (`scripts/ai_stream_healer.py` en GitHub Actions):**
  - **Cuándo actúa:** Solo cuando aparece una web nueva con protecciones atípicas donde las 4 permutaciones de `HeaderSelfHealer` fallaron.
  - **Por qué cuesta `$0`:** No se llama por cada usuario ni en cada clic. Se ejecuta en lote en GitHub Actions (cada 6 horas) usando el nivel gratuito de **Google Gemini Flash (1,500 llamadas/día sin costo)**, analiza los dominios fallidos reportados en Supabase y actualiza `rules/live_stream_rules.json` para todos los dispositivos.

---

## 3. Mapa Completo de Documentos Legibles por Humanos en el Repositorio

### Documentos Principales de Arquitectura y Diseño
1. [`doc/DESIGN.md`](file:///home/sataca/Descargas/APLICACIONES/proyectarAplicacion/doc/DESIGN.md) — **Documento Canónico de Diseño y Requerimientos Funcionales** (4 Pilares, Listas Guardadas, Vencimiento de Tokens, Streams en Vivo y Propuesta Realista `$0`).
2. [`doc/ARCHITECTURE.md`](file:///home/sataca/Descargas/APLICACIONES/proyectarAplicacion/doc/ARCHITECTURE.md) — **Arquitectura C4 del Sistema y Diagrama de Flujo de Decisión `$0`**.
3. [`PROJECT_CONTEXT.md`](file:///home/sataca/Descargas/APLICACIONES/proyectarAplicacion/PROJECT_CONTEXT.md) — **Este documento** (Contexto ejecutivo y separación entre Clases Kotlin e IA Gratuita).
4. [`AGENTS.md`](file:///home/sataca/Descargas/APLICACIONES/proyectarAplicacion/AGENTS.md) — **Leyes Técnicas Inviolables del Proyecto** (Arquitectura Hexagonal, Límite de 300 líneas, Reglas de Hardware).
5. [`doc/PROJECT_DASHBOARD.md`](file:///home/sataca/Descargas/APLICACIONES/proyectarAplicacion/doc/PROJECT_DASHBOARD.md) — **Estado Actual del Proyecto y Historial de Versiones Desplegadas**.
6. [`doc/RUNBOOK.md`](file:///home/sataca/Descargas/APLICACIONES/proyectarAplicacion/doc/RUNBOOK.md) — **Manual Operativo de Compilación, Despliegue ADB y Diagnóstico**.

### Decisiones de Arquitectura (ADRs)
7. [`doc/adr/ADR-001-stack-inicial.md`](file:///home/sataca/Descargas/APLICACIONES/proyectarAplicacion/doc/adr/ADR-001-stack-inicial.md) — Elección de WebRTC, ExoPlayer y Jetpack Compose.
8. [`doc/adr/ADR-002-telemetria-tv-logs.md`](file:///home/sataca/Descargas/APLICACIONES/proyectarAplicacion/doc/adr/ADR-002-telemetria-tv-logs.md) — Telemetría y envío de logs desde la TV Box por red local.
9. [`doc/adr/ADR-003-estrategia-renovacion-enlaces-tv.md`](file:///home/sataca/Descargas/APLICACIONES/proyectarAplicacion/doc/adr/ADR-003-estrategia-renovacion-enlaces-tv.md) — Renovación de enlaces expirados en TV (`pageUrl`, QR y sustitución TTL).
10. [`doc/adr/ADR-004-arquitectura-auto-recuperacion-ia-ota.md`](file:///home/sataca/Descargas/APLICACIONES/proyectarAplicacion/doc/adr/ADR-004-arquitectura-auto-recuperacion-ia-ota.md) — Arquitectura escalonada de Costo `$0` (Clases Deterministas + IA Gratuita OTA).

### Historias de Usuario (Contratos Funcionales)
11. [`doc/historias-usuario/README.md`](file:///home/sataca/Descargas/APLICACIONES/proyectarAplicacion/doc/historias-usuario/README.md) y [`ARBOL_DE_RELACIONES.md`](file:///home/sataca/Descargas/APLICACIONES/proyectarAplicacion/doc/historias-usuario/ARBOL_DE_RELACIONES.md)
12. [`HU-001`](file:///home/sataca/Descargas/APLICACIONES/proyectarAplicacion/doc/historias-usuario/HU-001-galeria-fotos-continua.md) (Galería de Fotos), [`HU-002`](file:///home/sataca/Descargas/APLICACIONES/proyectarAplicacion/doc/historias-usuario/HU-002-standby-limpio-tv.md) (Standby Limpio en TV), [`HU-003`](file:///home/sataca/Descargas/APLICACIONES/proyectarAplicacion/doc/historias-usuario/HU-003-menu-lateral-historial.md) (Menú Lateral e Historial), [`HU-004`](file:///home/sataca/Descargas/APLICACIONES/proyectarAplicacion/doc/historias-usuario/HU-004-deduplicacion-streams-web.md) (Deduplicación de Streams), [`HU-005`](file:///home/sataca/Descargas/APLICACIONES/proyectarAplicacion/doc/historias-usuario/HU-005-extraccion-hexagonal-streams.md) (Extracción Hexagonal), [`HU-006`](file:///home/sataca/Descargas/APLICACIONES/proyectarAplicacion/doc/historias-usuario/HU-006-renovacion-enlaces-expirados-tv.md) (Renovación de Enlaces Expirados).
