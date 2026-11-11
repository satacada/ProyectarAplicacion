# 🗺️ ROADMAP MAESTRO UNIFICADO — OmniCast Pro (`v4.4` Multi-Agente)

> **Versión Base de Rescate:** `v3.7.0` (`versionCodeSender: 368` | `versionCodeReceiver: 351`)  
> **Política Operativa `v4.4`:** Micro-comandos `[auto]` y `[ver]` orquestados por el Planificador (`Argon`) invocando subagentes aislados (`flash` / `flash_lite`) | **Prohibido disparar Explorador Forense en tareas mapeadas** (todas las rutas están en este archivo y en [`doc/MAPA_DEL_SISTEMA_Y_FLUJO_DE_CODIGO.md`](file:///home/sataca/Descargas/APLICACIONES/proyectarAplicacion/doc/MAPA_DEL_SISTEMA_Y_FLUJO_DE_CODIGO.md)).  
> **Estado Global:** 🎉 **FASE 7 COMPLETADA AL 100% (Hitos 7.1, 7.2, 7.3 y 7.4 — PVR Inteligente, Touchpad, Navegación Inteligente y Stealth Chrome en Android TV — 95 Tests Passing — 0 Archivos Kotlin > 300L)**

---

## 💾 FASE 7 — PVR Inteligente, Touchpad, Navegación y Stealth Chrome (`[x]` — 4 / 4 completados — 100%)

- `[x]` **Hito 7.1 [CH-07] — Auto-Detección Preferencial USB y Purga de Huérfanos en Android TV (`app-receiver/src/main/kotlin/com/omnicast/receiver/directstream/TvStorageSafetyManager.kt`, `app-receiver/src/main/kotlin/com/omnicast/receiver/directstream/PvrStreamRecorder.kt`):**
  - **Objetivo:** Detectar automáticamente memorias USB conectadas con espacio holgado (`>= 1.5 GB`), aplicar la constante `SAFE_RESERVE_USB_BYTES` (500 MB) y purgar en segundo plano archivos temporales huérfanos (`.tmp`, `.part`, 0-byte) con más de 3 minutos de antigüedad, garantizando descargas y grabaciones PVR estables sin desbordar el almacenamiento de la TV Box.
  - **Archivos Objetivo Exactos:**
    - `app-receiver/src/main/kotlin/com/omnicast/receiver/directstream/TvStorageSafetyManager.kt` (290 líneas)
    - `app-receiver/src/main/kotlin/com/omnicast/receiver/directstream/PvrStreamRecorder.kt` (260 líneas)
  - **Agentes Involucrados:** `planner-architect` (`Argon`) ➔ `specialist-tv-player-pvr` (`flash`) ➔ `surgical-developer` (`flash`) ➔ `qa-oracle-tester` (`flash`) ➔ `sync-documenter` (`flash`) ➔ `git-ota-releaser` (`flash_lite`).

- `[x]` **Hito 7.2 [CH-08] — Modo Touchpad / Puntero Virtual en TV vía WebRTC/WebSocket (`core/protocol/src/main/kotlin/com/omnicast/core/protocol/SignalingMessage.kt`, `app-receiver/src/main/kotlin/com/omnicast/receiver/ui/components/TvVirtualPointerOverlay.kt`, `app-receiver/src/main/kotlin/com/omnicast/receiver/ui/components/TvGlobalRecordOverlay.kt`, `app-receiver/src/main/kotlin/com/omnicast/receiver/ui/TvReceiverActivity.kt`):**
  - **Objetivo:** Implementar control remoto tipo touchpad y puntero virtual interactivo en la pantalla del Android TV mediante señalización WebRTC/WebSocket, permitiendo navegación precisa y feedback visual de clics.
  - **Archivos Objetivo Exactos:**
    - `core/protocol/src/main/kotlin/com/omnicast/core/protocol/SignalingMessage.kt` (ampliado con `PointerControl`: 169 líneas)
    - `app-receiver/src/main/kotlin/com/omnicast/receiver/ui/components/TvVirtualPointerOverlay.kt` (nuevo HUD composable de puntero virtual: 93 líneas)
    - `app-receiver/src/main/kotlin/com/omnicast/receiver/ui/components/TvGlobalRecordOverlay.kt` (inyección global no invasiva: 60 líneas)
    - `app-receiver/src/main/kotlin/com/omnicast/receiver/ui/TvReceiverActivity.kt` (recepción síncrona de comandos de puntero: 296 líneas, estrictamente `< 300` líneas)
  - **Agentes Involucrados:** `planner-architect` (`Argon`) ➔ `specialist-tv-player-pvr` (`flash`) ➔ `surgical-developer` (`flash`) ➔ `qa-oracle-tester` (`flash`) ➔ `sync-documenter` (`flash`) ➔ `git-ota-releaser` (`flash_lite`).

- `[x]` **Hito 7.3 [CH-09] — Desacoplamiento de window.open y Navegación Inteligente de Mismo Origen (`app-sender/src/main/assets/sniffer/universal_sniffer.js`, `app-sender/src/main/kotlin/com/omnicast/sender/browser/WvcShieldSecurityEngine.kt`):**
  - **Objetivo:** Eliminar el hook ciego redundante de `window.open` en `universal_sniffer.js` que interfería con la heurística de seguridad de `WvcShieldSecurityEngine.kt`, incorporando trazas de auditoría (`OMNICAST_NAV_SAME_ORIGIN`) para clics legítimos en portales de deportes en vivo (LiveSports808) gobernados por SQLite (`AdBlockDatabase`) y C++20 (`NativeAdBlockEngine`).
  - **Archivos Objetivo Exactos:**
    - `app-sender/src/main/assets/sniffer/universal_sniffer.js` (hook redundante eliminado)
    - `app-sender/src/main/kotlin/com/omnicast/sender/browser/WvcShieldSecurityEngine.kt` (log de trazabilidad: 217 líneas)
  - **Agentes Involucrados:** `planner-architect` (`Argon`) ➔ `specialist-tv-player-pvr` (`flash`) ➔ `surgical-developer` (`flash`) ➔ `qa-oracle-tester` (`flash`) ➔ `sync-documenter` (`flash`) ➔ `git-ota-releaser` (`flash_lite`).

- `[x]` **Hito 7.4 [CH-10] — Modo Stealth Chrome Nativo Anti-Detección de WebView (`app-sender/src/main/kotlin/com/omnicast/sender/browser/WvcShieldSecurityEngine.kt`):**
  - **Objetivo:** Suprimir la cabecera `X-Requested-With` mediante AndroidX WebKit (`setRequestedWithHeaderOriginAllowList` con `emptySet`) e inyectar polyfills JavaScript en `WvcShieldSecurityEngine.kt` para spoofing de `window.chrome` y `navigator.userAgentData` con marcas nativas de Google Chrome, evadiendo defensas de Cloudflare y portales como SoloLatino.
  - **Archivos Objetivo Exactos:**
    - `app-sender/src/main/kotlin/com/omnicast/sender/browser/WvcShieldSecurityEngine.kt` (supresión de header y polyfill Stealth JS: 217 líneas)
  - **Agentes Involucrados:** `planner-architect` (`Argon`) ➔ `specialist-tv-player-pvr` (`flash`) ➔ `surgical-developer` (`flash`) ➔ `qa-oracle-tester` (`flash`) ➔ `sync-documenter` (`flash`) ➔ `git-ota-releaser` (`flash_lite`).

---

## 📺 FASE 6 — Telemetría OSD en Tiempo Real y Experiencia de Control Remoto (Android TV) (`[x]` — 2 / 2 completados — 100%)

- `[x]` **Hito 6.1 [CH-05] — Overlay HUD de Telemetría OSD en Tiempo Real y Activación Remota en Android TV (`app-receiver/src/main/kotlin/com/omnicast/receiver/ui/components/TvTelemetryOverlay.kt`, `app-receiver/src/main/kotlin/com/omnicast/receiver/ui/components/TvGlobalRecordOverlay.kt`, `app-receiver/src/main/kotlin/com/omnicast/receiver/ui/TvRemoteKeyHandler.kt`):**
  - **Objetivo:** Proveer visualización instantánea en pantalla (OSD HUD) de métricas técnicas de streaming, reproducción y hardware en Android TV, permitiendo a los operadores e ingenieros diagnosticar la calidad de video sin interrumpir la reproducción.
  - **Archivos Objetivo Exactos:**
    - `app-receiver/src/main/kotlin/com/omnicast/receiver/ui/components/TvTelemetryOverlay.kt` (nuevo componente HUD composable: 159 líneas)
    - `app-receiver/src/main/kotlin/com/omnicast/receiver/ui/components/TvGlobalRecordOverlay.kt` (inyección global no invasiva: 59 líneas)
    - `app-receiver/src/main/kotlin/com/omnicast/receiver/ui/TvRemoteKeyHandler.kt` (intercepción de atajos en control remoto: teclas `INFO`, `PROG_GREEN` y `1`: 236 líneas)
  - **Agentes Involucrados:** `planner-architect` (`Argon`) ➔ `specialist-tv-player-pvr` (`flash`) ➔ `surgical-developer` (`flash`) ➔ `qa-oracle-tester` (`flash`) ➔ `sync-documenter` (`flash`) ➔ `git-ota-releaser` (`flash_lite`).

- `[x]` **Hito 6.2 [CH-06] — Controles Multimedia y Navegación D-Pad en Reproductor Web y Android TV (`app-receiver/src/main/kotlin/com/omnicast/receiver/ui/TvRemoteKeyHandler.kt`):**
  - **Objetivo:** Mapear nativamente las teclas D-Pad del control remoto (`Center/Enter` para Toggle Play, `Right` para avance rápido +10s con Toast, `Left` para rebobinado -10s con Toast, `Down/Up` para OSD) en estados `DirectStreamPlaying`, `YouTubePlaying` y `WebStreamPlaying`, permitiendo control multimedia fluido en mandos minimalistas de Android TV sin requerir botones dedicados.
  - **Archivos Objetivo Exactos:**
    - `app-receiver/src/main/kotlin/com/omnicast/receiver/ui/TvRemoteKeyHandler.kt` (ampliado a 280 líneas, cumpliendo estrictamente con la regla `< 300` líneas).
  - **Agentes Involucrados:** `planner-architect` (`Argon`) ➔ `specialist-tv-player-pvr` (`flash`) ➔ `surgical-developer` (`flash`) ➔ `qa-oracle-tester` (`flash`) ➔ `sync-documenter` (`flash`) ➔ `git-ota-releaser` (`flash_lite`).

---

## 🛡️ FASE 0 — Base Heredada e Inmutable (`[x]` — 100% Sellada bajo Ley de Intocabilidad)

Todo lo listado en la Fase 0 se encuentra en producción, validado por 78 tests unitarios (100% passing) y **protegido contra refactorizaciones no autorizadas**:

- `[x]` **0.1 Núcleo Hexagonal y Protocolo de Señalización (`:core:domain`, `:core:protocol`):**
  - *Archivos sellados:*
    - `core/domain/src/main/kotlin/com/omnicast/core/domain/usecases/DetectMediaStreamUseCase.kt`
    - `core/domain/src/main/kotlin/com/omnicast/core/domain/usecases/StreamCandidateValidator.kt`
    - `core/domain/src/main/kotlin/com/omnicast/core/domain/usecases/HlsMasterResolver.kt`
    - `core/domain/src/main/kotlin/com/omnicast/core/domain/usecases/StreamManifestProbe.kt`
    - `core/domain/src/main/kotlin/com/omnicast/core/domain/ports/IMediaStreamExtractorPort.kt`
    - `core/protocol/src/main/kotlin/com/omnicast/core/protocol/SignalingMessage.kt`
  - *Cobertura:* 26 tests en `:core:domain:test` + 7 tests en `:core:protocol:test`.

- `[x]` **0.2 Emisor Móvil, Tenaza Dual (Red + DOM), Motor C++20 y Gobernanza SQLite/Supabase (`:app-sender`):**
  - *Archivos sellados:*
    - `app-sender/src/main/kotlin/com/omnicast/sender/ui/browser/InAppBrowserScreen.kt` (ciclo de captura y selector sellado)
    - `app-sender/src/main/kotlin/com/omnicast/sender/ui/browser/BrowserWebView.kt` (foco nativo sin `setOnTouchListener` + aislamiento Cloudflare)
    - `app-sender/src/main/kotlin/com/omnicast/sender/ui/browser/WvcVideoListSheet.kt` (lista plana 1:1)
    - `app-sender/src/main/assets/sniffer/universal_sniffer.js`
    - `app-sender/src/main/cpp/stream_classifier.cpp`, `dean_edwards_unpacker.cpp`, `ad_pattern_matcher.cpp` (C++20 Zero-Allocation)
    - `app-sender/src/main/kotlin/com/omnicast/sender/stream/StreamProfileDatabase.kt` y `CloudStreamRuleSyncManager.kt`
    - `app-sender/src/main/kotlin/com/omnicast/sender/ui/browser/AdBlockDatabase.kt` y `AdBlockRuleSyncManager.kt`
  - *Contratos cubiertos:* `HU-001`, `HU-003`, `HU-004`, `HU-005`, `HU-007`, `HU-008`, `HU-009`, `HU-010`, `HU-011`, `HU-014`, `HU-015`, `HU-016`, `HU-018`, `HU-020`.
  - *Cobertura:* 31 tests en `:app-sender:testDebugUnitTest`.

- `[x]` **0.3 Receptor Android TV, Screen Mirroring Dual y Reproductor Híbrido Anti-CORS (`:app-receiver`):**
  - *Archivos sellados:*
    - `app-receiver/src/main/kotlin/com/omnicast/receiver/ui/components/TvScreens.kt` (Pantalla Completa vs Por Aplicación)
    - `app-receiver/src/main/kotlin/com/omnicast/receiver/webrtc/WebRtcReceiverManager.kt` (escala `1.075f` vertical FIT / `1.0f` horizontal, STOP `< 5 ms`)
    - `app-receiver/src/main/kotlin/com/omnicast/receiver/directstream/ExoPlayerTvManager.kt`
    - `app-receiver/src/main/kotlin/com/omnicast/receiver/ui/components/TvWebStreamScreen.kt`
    - `app-receiver/src/main/kotlin/com/omnicast/receiver/ui/components/TvWebResourceInterceptor.kt` (`Access-Control-Allow-Origin: *`)
    - `app-receiver/src/main/kotlin/com/omnicast/receiver/ui/components/TvYouTubeScreen.kt` (IFrame oficial inmune a 403 Botguard)
  - *Contratos cubiertos:* `HU-002`, `HU-006`, `HU-012`, `HU-013`.

- `[x]` **0.4 Motor PVR Dual (ExoPlayer + Puente Chromium BoringSSL) y Guardián Anti-Brick (`v3.5.59` – `v3.6.08`):**
  - *Archivos sellados:*
    - `app-receiver/src/main/kotlin/com/omnicast/receiver/directstream/PvrStreamRecorder.kt`
    - `app-receiver/src/main/kotlin/com/omnicast/receiver/directstream/PvrHlsMasterParser.kt` (soporte `.ts` y fMP4 `#EXT-X-MAP`)
    - `app-receiver/src/main/kotlin/com/omnicast/receiver/directstream/TvWebPvrBridge.kt` (Variante 1 DropCDN/Fastream)
    - `app-receiver/src/main/kotlin/com/omnicast/receiver/directstream/TvStorageSafetyManager.kt` (reserva sagrada 2.0 GB TV / 500 MB USB y purga de 0 bytes)
    - `app-receiver/src/main/kotlin/com/omnicast/receiver/ui/components/TvRecordExitConfirmDialog.kt`
  - *Contratos cubiertos:* `HU-017`, `HU-019` y entrega `v3.6.08`.
  - *Cobertura:* 14 tests en `:app-receiver:testDebugUnitTest`.

---

## 🧹 FASE 1 — Saneamiento y Reestructuración Quirúrgica (`[x]` — 4 / 4 completadas — 100%)

Actividades acotadas de Cirugía Mínima para resolver la deuda técnica detectada en la auditoría forense sin alterar ninguna lógica funcional:

- `[x]` **Actividad 1.1 `[auto]` — Limpieza Forense de Raíz, Blindaje `.gitignore` y Purga de Directorios Vacíos (`.gitignore`, `phone_dump.txt`, `phone_logcat_*.log`, `log_all_recent.txt`, `screen_v*.png`, `rules/`):**
  - **Objetivo:** Eliminar los >60 MB de volcados y capturas temporales de depuración en la raíz del repositorio (`phone_dump.txt`, `phone_logcat_1204.log`, `phone_logcat_1130.log`, `log_all_recent.txt`, `screen_v17*.png` a `screen_v21.png`), eliminar la carpeta vacía `rules/` y reforzar `.gitignore` para impedir que se rastreen logs o dumps de ADB.
  - **Archivos Objetivo Exactos:**
    - `.gitignore`
    - Raíz del proyecto (`phone_dump.txt`, `phone_logcat_*.log`, `log_all_recent.txt`, `screen_v*.png`, `rules/`)
  - **Agentes Involucrados:** `planner-architect` (`Argon`) ➔ `surgical-developer` (`flash`) ➔ `qa-oracle-tester` (`flash`) ➔ `sync-documenter` (`flash`) ➔ `git-ota-releaser` (`flash_lite`).

- `[x]` **Actividad 1.2 `[auto]` — Cirugía de Límite de 300 Líneas en `VideoCastPlayerActivity.kt` (342L ➔ 159L) (`app-sender/src/main/kotlin/com/omnicast/sender/overlay/VideoCastPlayerActivity.kt`, `app-sender/src/main/kotlin/com/omnicast/sender/overlay/VideoCastPlayerControls.kt` [233L]):**
  - **Objetivo:** Cumplir la Regla 3 de `AGENTS.md` (`<= 300` líneas) extrayendo los subcomponentes visuales de controles del reproductor local a un archivo delegado dentro del mismo paquete `com.omnicast.sender.overlay`, preservando intacta la lógica de reproducción y casteo.
  - **Archivos Objetivo Exactos:**
    - `app-sender/src/main/kotlin/com/omnicast/sender/overlay/VideoCastPlayerActivity.kt` (reducido a 159 líneas)
    - `app-sender/src/main/kotlin/com/omnicast/sender/overlay/VideoCastPlayerControls.kt` (nuevo archivo delegado: 233 líneas)
  - **Agentes Involucrados:** `planner-architect` (`Argon`) ➔ `surgical-developer` (`flash`) ➔ `qa-oracle-tester` (`flash`) ➔ `sync-documenter` (`flash`) ➔ `git-ota-releaser` (`flash_lite`).

- `[x]` **Actividad 1.3 `[auto]` — Cirugía de Límite de 300 Líneas en `GalleryPickerActivity.kt` (326L ➔ 235L) (`app-sender/src/main/kotlin/com/omnicast/sender/overlay/GalleryPickerActivity.kt`, `app-sender/src/main/kotlin/com/omnicast/sender/overlay/GalleryPermissionHelper.kt` [108L]):**
  - **Objetivo:** Cumplir la Regla 3 de `AGENTS.md` (`<= 300` líneas) extrayendo los helpers de permisos y resolución de rutas de galería a un archivo auxiliar en `com.omnicast.sender.overlay`, manteniendo intacto el flujo de galería continua (`HU-001`) y el test `GalleryRouteNavigationTest.kt`.
  - **Archivos Objetivo Exactos:**
    - `app-sender/src/main/kotlin/com/omnicast/sender/overlay/GalleryPickerActivity.kt` (reducido a 235 líneas)
    - `app-sender/src/main/kotlin/com/omnicast/sender/overlay/GalleryPermissionHelper.kt` (nuevo archivo delegado: 108 líneas)
    - `app-sender/src/test/kotlin/com/omnicast/sender/gallery/GalleryRouteNavigationTest.kt` (verificación de no-regresión)
  - **Agentes Involucrados:** `planner-architect` (`Argon`) ➔ `surgical-developer` (`flash`) ➔ `qa-oracle-tester` (`flash`) ➔ `sync-documenter` (`flash`) ➔ `git-ota-releaser` (`flash_lite`).

- `[x]` **Actividad 1.4 `[ver]` — Unificación de Configuración Cloud Supabase y Script Canónico de Release OTA (`core/domain/src/main/kotlin/com/omnicast/core/domain/config/SupabaseCloudConfig.kt`, `app-sender/src/main/kotlin/com/omnicast/sender/stream/CloudStreamRuleSyncManager.kt`, `app-sender/src/main/kotlin/com/omnicast/sender/ui/browser/AdBlockRuleSyncManager.kt`, `app-sender/src/main/kotlin/com/omnicast/sender/log/SenderLogger.kt`, `app-receiver/src/main/kotlin/com/omnicast/receiver/log/ReceiverLogger.kt`, `scratch/publish_release.py`, `scripts/publish_release.py`):**
  - **Objetivo:** Eliminar la duplicación textual de la URL y `SUPABASE_ANON_KEY` en 4 archivos de telemetría/sincronización y alinear `scratch/publish_release.py` con `scripts/publish_release.py` para garantizar despliegues OTA consistentes.
  - **Archivos Objetivo Exactos:**
    - `core/domain/src/main/kotlin/com/omnicast/core/domain/config/SupabaseCloudConfig.kt` (nuevo objeto centralizado en `:core:domain`)
    - `app-sender/src/main/kotlin/com/omnicast/sender/stream/CloudStreamRuleSyncManager.kt`
    - `app-sender/src/main/kotlin/com/omnicast/sender/ui/browser/AdBlockRuleSyncManager.kt`
    - `app-sender/src/main/kotlin/com/omnicast/sender/log/SenderLogger.kt`
    - `app-receiver/src/main/kotlin/com/omnicast/receiver/log/ReceiverLogger.kt`
    - `scratch/publish_release.py` y `scripts/publish_release.py`
  - **Agentes Involucrados:** `planner-architect` (`Argon`) ➔ `specialist-stream-extraction` (`flash`) ➔ `surgical-developer` (`flash`) ➔ `qa-oracle-tester` (`flash`) ➔ `sync-documenter` (`flash`) ➔ `git-ota-releaser` (`flash_lite`).

---

## 🌐 FASE 2 — Evolución de Resiliencia CDN Anti-Scrape y Gobernanza Cloud (`[x]` — 2 / 2 completadas — 100%)

- `[x]` **Actividad 2.1 `[ver]` — Nuevos Adaptadores Modulares "Poner y Sacar" (`IMediaStreamExtractorPort`) y Reglas Dinámicas (`core/domain/src/main/kotlin/com/omnicast/core/domain/usecases/StreamCandidateValidator.kt`, `app-sender/src/main/kotlin/com/omnicast/sender/ui/browser/extractors/LuluStreamExtractorAdapter.kt`, `app-sender/src/main/kotlin/com/omnicast/sender/ui/browser/extractors/StreamwishExtractorAdapter.kt`, `app-sender/src/main/kotlin/com/omnicast/sender/stream/StreamProfileDbSchema.kt`, `app-sender/src/main/kotlin/com/omnicast/sender/stream/StreamProfileDatabase.kt`, `app-sender/src/test/kotlin/com/omnicast/sender/ui/browser/MediaPlatformRegressionTest.kt`):**
  - **Objetivo:** Incorporar soporte para nuevas CDNs de portales de cine y deportes en vivo como adaptadores aislados delegando la validación binaria en `StreamCandidateValidator.isValidCdnStream()`, sin tocar extractores universales ni `InAppBrowserScreen.kt`.
  - **Archivos Objetivo Exactos:**
    - `core/domain/src/main/kotlin/com/omnicast/core/domain/usecases/StreamCandidateValidator.kt`
    - `app-sender/src/main/kotlin/com/omnicast/sender/ui/browser/extractors/LuluStreamExtractorAdapter.kt` (nuevo adaptador modular: 61 líneas)
    - `app-sender/src/main/kotlin/com/omnicast/sender/ui/browser/extractors/StreamwishExtractorAdapter.kt` (nuevo adaptador modular: 63 líneas)
    - `app-sender/src/main/kotlin/com/omnicast/sender/stream/StreamProfileDbSchema.kt` (nuevo esquema SQLite delegado: 79 líneas)
    - `app-sender/src/main/kotlin/com/omnicast/sender/stream/StreamProfileDatabase.kt` (reducido a 247 líneas)
    - `app-sender/src/main/kotlin/com/omnicast/sender/ui/browser/extractors/StreamExtractorRegistry.kt`
    - `app-sender/src/test/kotlin/com/omnicast/sender/ui/browser/MediaPlatformRegressionTest.kt`
    - `core/domain/src/test/kotlin/com/omnicast/core/domain/usecases/StreamCandidateValidatorTest.kt`
  - **Agentes Involucrados:** `planner-architect` (`Argon`) ➔ `specialist-stream-extraction` (`flash`) ➔ `surgical-developer` (`flash`) ➔ `qa-oracle-tester` (`flash`) ➔ `sync-documenter` (`flash`) ➔ `git-ota-releaser` (`flash_lite`).

- `[x]` **Actividad 2.2 `[ver]` — Refuerzo Heurístico de Variantes HLS/DASH y Centinela Anti-Honeypot (`core/domain/src/main/kotlin/com/omnicast/core/domain/usecases/HlsMasterResolver.kt`, `core/domain/src/main/kotlin/com/omnicast/core/domain/usecases/StreamManifestProbe.kt`, `app-receiver/src/test/kotlin/com/omnicast/receiver/ui/components/sentinel/CdnHoneypotSentinelTest.kt`):**
  - **Objetivo:** Ampliar la detección de pistas de audio/subtítulos desacopladas y escudos anti-honeypot en manifiestos HLS compuestos respetando la Ley Anti-Colisión RFC 8216.
  - **Archivos Objetivo Exactos:**
    - `core/domain/src/main/kotlin/com/omnicast/core/domain/usecases/HlsMasterResolver.kt`
    - `core/domain/src/main/kotlin/com/omnicast/core/domain/usecases/StreamManifestProbe.kt`
    - `app-receiver/src/main/kotlin/com/omnicast/receiver/ui/components/sentinel/CdnHoneypotSentinel.kt`
    - `app-receiver/src/test/kotlin/com/omnicast/receiver/ui/components/sentinel/CdnHoneypotSentinelTest.kt`
  - **Agentes Involucrados:** `planner-architect` (`Argon`) ➔ `specialist-stream-extraction` (`flash`) ➔ `surgical-developer` (`flash`) ➔ `qa-oracle-tester` (`flash`) ➔ `sync-documenter` (`flash`) ➔ `git-ota-releaser` (`flash_lite`).

---

## 📼 FASE 3 — Evolución del Motor PVR y Biblioteca de Grabaciones en TV (`[x]` — 2 / 2 completadas — 100%)

- `[x]` **Actividad 3.1 `[auto]` — Gestión de Biblioteca Local/USB de Grabaciones PVR en Android TV (`app-receiver/src/main/kotlin/com/omnicast/receiver/directstream/TvStorageSafetyManager.kt`, `app-receiver/src/main/kotlin/com/omnicast/receiver/ui/components/TvPvrLibrarySummaryBar.kt`, `app-receiver/src/test/kotlin/com/omnicast/receiver/directstream/TvStorageSafetyManagerTest.kt`):**
  - **Objetivo:** Permitir explorar, reproducir y administrar las grabaciones `.ts`/`.mp4` almacenadas en `/Download/PVR_Grabaciones` y `Pelis_descargas` (memoria interna o USB) desde la interfaz de TV respetando el límite `< 300` líneas y la reserva sagrada de `TvStorageSafetyManager.kt`.
  - **Archivos Objetivo Exactos:**
    - `app-receiver/src/main/kotlin/com/omnicast/receiver/directstream/TvStorageSafetyManager.kt` (ampliado con `scanPvrRecordings`, `scanDirectoryForRecordings` y modelo `TvPvrRecordingFile`: 274 líneas)
    - `app-receiver/src/main/kotlin/com/omnicast/receiver/ui/components/TvPvrLibrarySummaryBar.kt` (nuevo composable modular: 161 líneas)
    - `app-receiver/src/test/kotlin/com/omnicast/receiver/directstream/TvStorageSafetyManagerTest.kt` (8 tests unitarios de escaneo, reserva y cálculo de bitrate)
  - **Agentes Involucrados:** `planner-architect` (`Argon`) ➔ `specialist-tv-player-pvr` (`flash`) ➔ `surgical-developer` (`flash`) ➔ `qa-oracle-tester` (`flash`) ➔ `sync-documenter` (`flash`) ➔ `git-ota-releaser` (`flash_lite`).

- `[x]` **Actividad 3.2 `[ver]` — Reanudación Inteligente y Telemetría Enriquecida de Descargas VOD/PVR (`core/protocol/src/main/kotlin/com/omnicast/core/protocol/SignalingMessage.kt`, `app-receiver/src/main/kotlin/com/omnicast/receiver/directstream/PvrResumeMetadataStore.kt`, `app-receiver/src/main/kotlin/com/omnicast/receiver/directstream/PvrStreamRecorder.kt`, `app-receiver/src/test/kotlin/com/omnicast/receiver/directstream/PvrResumeMetadataStoreTest.kt`):**
  - **Objetivo:** Persistir metadatos de progreso de descarga y permitir reanudación segura ante micro-cortes de red en flujos VOD desacoplando la persistencia de metadatos en un archivo independiente.
  - **Archivos Objetivo Exactos:**
    - `core/protocol/src/main/kotlin/com/omnicast/core/protocol/SignalingMessage.kt` (ampliado con `bytesDownloaded`, `totalSizeBytes`, `isResumable` y `errorNotice` en `RecordTelemetry`)
    - `app-receiver/src/main/kotlin/com/omnicast/receiver/directstream/PvrResumeMetadataStore.kt` (nuevo almacén atómico JSON de metadatos de reanudación: 62 líneas)
    - `app-receiver/src/main/kotlin/com/omnicast/receiver/directstream/PvrStreamRecorder.kt` (reducido a 250 líneas al delegar metadatos y reanudación)
    - `app-receiver/src/main/kotlin/com/omnicast/receiver/directstream/PvrRecordProgress.kt` y `TvReceiverHelper.kt` (soporte de bytes y reanudación VOD)
    - `app-receiver/src/test/kotlin/com/omnicast/receiver/directstream/PvrResumeMetadataStoreTest.kt` (3 tests unitarios de persistencia, carga y purga)
    - `core/protocol/src/test/kotlin/com/omnicast/core/protocol/SignalingProtocolTest.kt` (2 tests unitarios de serialización de nuevos campos)
  - **Agentes Involucrados:** `planner-architect` (`Argon`) ➔ `specialist-tv-player-pvr` (`flash`) ➔ `surgical-developer` (`flash`) ➔ `qa-oracle-tester` (`flash`) ➔ `sync-documenter` (`flash`) ➔ `git-ota-releaser` (`flash_lite`).

---

## 🧪 FASE 4 — Cobertura Cuantitativa de Tests y Telemetría Proactiva (`[x]` — 1 / 1 completada — 100%)

- `[x]` **Actividad 4.1 `[auto]` — Ampliación del Oráculo Unitario y Métricas de Regresión por Pilar (`core/domain/build.gradle.kts`, `core/domain/src/test/kotlin/com/omnicast/core/domain/usecases/HlsMasterResolverTest.kt`, `core/protocol/src/test/kotlin/com/omnicast/core/protocol/SignalingProtocolTest.kt`, `app-receiver/src/test/kotlin/com/omnicast/receiver/directstream/TvStorageSafetyManagerTest.kt`):**
  - **Objetivo:** Incorporar reportes cuantitativos de cobertura unitaria con JaCoCo en `:core:domain` (`jacocoTestReport`) y expandir casos de prueba de bordes en `:core:domain` (múltiples etiquetas HLS `#EXT-X-MEDIA`, query parameters codificados y URLs relativas), `:core:protocol` (claves JSON desconocidas, strings vacíos y valores numéricos extremos) y `:app-receiver` (límites de frontera matemática en reserva sagrada 2.0GB/500MB, cálculo seguro de minutos y purga de 0 bytes).
  - **Archivos Objetivo Exactos:**
    - `core/domain/build.gradle.kts` (plugin JaCoCo y tarea `jacocoTestReport` con salida XML y HTML)
    - `core/domain/src/test/kotlin/com/omnicast/core/domain/usecases/HlsMasterResolverTest.kt` (ampliado con 3 tests de borde: 187 líneas)
    - `core/protocol/src/test/kotlin/com/omnicast/core/protocol/SignalingProtocolTest.kt` (ampliado con 3 tests de resiliencia: 268 líneas)
    - `app-receiver/src/test/kotlin/com/omnicast/receiver/directstream/TvStorageSafetyManagerTest.kt` (ampliado con test de límites de frontera: 213 líneas)
  - **Agentes Involucrados:** `planner-architect` (`Argon`) ➔ `qa-oracle-tester` (`flash`) ➔ `surgical-developer` (`flash`) ➔ `sync-documenter` (`flash`) ➔ `git-ota-releaser` (`flash_lite`).
