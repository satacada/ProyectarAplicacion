# 🗺️ ROADMAP MAESTRO UNIFICADO — OmniCast Pro (`v4.4` Multi-Agente)

> **Versión Base de Rescate:** `v3.6.08` (`versionCodeSender: 359` | `versionCodeReceiver: 342`)  
> **Política Operativa `v4.4`:** Micro-comandos `[auto]` y `[ver]` orquestados por el Planificador (`Argon`) invocando subagentes aislados (`flash` / `flash_lite`) | **Prohibido disparar Explorador Forense en tareas mapeadas** (todas las rutas están en este archivo y en [`doc/MAPA_DEL_SISTEMA_Y_FLUJO_DE_CODIGO.md`](file:///home/sataca/Descargas/APLICACIONES/proyectarAplicacion/doc/MAPA_DEL_SISTEMA_Y_FLUJO_DE_CODIGO.md)).

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

## 🧹 FASE 1 — Saneamiento y Reestructuración Quirúrgica (`3 / 4 completadas — 75%`)

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

- `[ ]` **Actividad 1.4 `[ver]` — Unificación de Configuración Cloud Supabase y Script Canónico de Release OTA (`core/domain/src/main/kotlin/com/omnicast/core/domain/config/SupabaseCloudConfig.kt`, `app-sender/src/main/kotlin/com/omnicast/sender/stream/CloudStreamRuleSyncManager.kt`, `app-sender/src/main/kotlin/com/omnicast/sender/ui/browser/AdBlockRuleSyncManager.kt`, `app-sender/src/main/kotlin/com/omnicast/sender/log/SenderLogger.kt`, `app-receiver/src/main/kotlin/com/omnicast/receiver/log/ReceiverLogger.kt`, `scratch/publish_release.py`, `scripts/publish_release.py`):**
  - **Objetivo:** Eliminar la duplicación textual de la URL y `SUPABASE_ANON_KEY` en 4 archivos de telemetría/sincronización y alinear `scratch/publish_release.py` con `scripts/publish_release.py` para garantizar despliegues OTA consistentes.
  - **Archivos Objetivo Exactos:**
    - `core/domain/src/main/kotlin/com/omnicast/core/domain/config/SupabaseCloudConfig.kt`
    - `app-sender/src/main/kotlin/com/omnicast/sender/stream/CloudStreamRuleSyncManager.kt`
    - `app-sender/src/main/kotlin/com/omnicast/sender/ui/browser/AdBlockRuleSyncManager.kt`
    - `app-sender/src/main/kotlin/com/omnicast/sender/log/SenderLogger.kt`
    - `app-receiver/src/main/kotlin/com/omnicast/receiver/log/ReceiverLogger.kt`
    - `scratch/publish_release.py` y `scripts/publish_release.py`
  - **Agentes Involucrados:** `planner-architect` (`Argon`) ➔ `specialist-stream-extraction` (`flash`) ➔ `surgical-developer` (`flash`) ➔ `qa-oracle-tester` (`flash`) ➔ `sync-documenter` (`flash`) ➔ `git-ota-releaser` (`flash_lite`).

---

## 🌐 FASE 2 — Evolución de Resiliencia CDN Anti-Scrape y Gobernanza Cloud (`[ ]`)

- `[ ]` **Actividad 2.1 `[ver]` — Nuevos Adaptadores Modulares "Poner y Sacar" (`IMediaStreamExtractorPort`) y Reglas Dinámicas (`core/domain/src/main/kotlin/com/omnicast/core/domain/usecases/StreamCandidateValidator.kt`, `app-sender/src/main/kotlin/com/omnicast/sender/ui/browser/extractors/`, `app-sender/src/main/kotlin/com/omnicast/sender/stream/StreamProfileDatabase.kt`, `app-sender/src/test/kotlin/com/omnicast/sender/ui/browser/MediaPlatformRegressionTest.kt`):**
  - **Objetivo:** Incorporar soporte para nuevas CDNs de portales de cine y deportes en vivo como adaptadores aislados delegando la validación binaria en `StreamCandidateValidator.isValidCdnStream()`, sin tocar extractores universales ni `InAppBrowserScreen.kt`.
  - **Archivos Objetivo Exactos:**
    - `core/domain/src/main/kotlin/com/omnicast/core/domain/usecases/StreamCandidateValidator.kt`
    - `core/domain/src/main/kotlin/com/omnicast/core/domain/extractor/` y `app-sender/src/main/kotlin/com/omnicast/sender/ui/browser/extractors/`
    - `app-sender/src/main/kotlin/com/omnicast/sender/stream/StreamProfileDatabase.kt`
    - `app-sender/src/test/kotlin/com/omnicast/sender/ui/browser/MediaPlatformRegressionTest.kt`
  - **Agentes Involucrados:** `planner-architect` (`Argon`) ➔ `specialist-stream-extraction` (`flash`) ➔ `surgical-developer` (`flash`) ➔ `qa-oracle-tester` (`flash`) ➔ `sync-documenter` (`flash`) ➔ `git-ota-releaser` (`flash_lite`).

- `[ ]` **Actividad 2.2 `[ver]` — Refuerzo Heurístico de Variantes HLS/DASH y Centinela Anti-Honeypot (`core/domain/src/main/kotlin/com/omnicast/core/domain/usecases/HlsMasterResolver.kt`, `core/domain/src/main/kotlin/com/omnicast/core/domain/usecases/StreamManifestProbe.kt`, `app-receiver/src/test/kotlin/com/omnicast/receiver/ui/components/sentinel/CdnHoneypotSentinelTest.kt`):**
  - **Objetivo:** Ampliar la detección de pistas de audio/subtítulos desacopladas y escudos anti-honeypot en manifiestos HLS compuestos respetando la Ley Anti-Colisión RFC 8216.
  - **Archivos Objetivo Exactos:**
    - `core/domain/src/main/kotlin/com/omnicast/core/domain/usecases/HlsMasterResolver.kt`
    - `core/domain/src/main/kotlin/com/omnicast/core/domain/usecases/StreamManifestProbe.kt`
    - `app-receiver/src/main/kotlin/com/omnicast/receiver/ui/components/sentinel/CdnHoneypotSentinel.kt`
    - `app-receiver/src/test/kotlin/com/omnicast/receiver/ui/components/sentinel/CdnHoneypotSentinelTest.kt`
  - **Agentes Involucrados:** `planner-architect` (`Argon`) ➔ `specialist-stream-extraction` (`flash`) ➔ `surgical-developer` (`flash`) ➔ `qa-oracle-tester` (`flash`) ➔ `sync-documenter` (`flash`) ➔ `git-ota-releaser` (`flash_lite`).

---

## 📼 FASE 3 — Evolución del Motor PVR y Biblioteca de Grabaciones en TV (`[ ]`)

- `[ ]` **Actividad 3.1 `[auto]` — Gestión de Biblioteca Local/USB de Grabaciones PVR en Android TV (`app-receiver/src/main/kotlin/com/omnicast/receiver/directstream/TvStorageSafetyManager.kt`, `app-receiver/src/main/kotlin/com/omnicast/receiver/ui/components/TvPvrLibrarySummaryBar.kt`, `app-receiver/src/test/kotlin/com/omnicast/receiver/directstream/TvStorageSafetyManagerTest.kt`):**
  - **Objetivo:** Permitir explorar, reproducir y administrar las grabaciones `.ts`/`.mp4` almacenadas en `/Download/PVR_Grabaciones` y `Pelis_descargas` (memoria interna o USB) desde la interfaz de TV respetando el límite `< 300` líneas y la reserva sagrada de `TvStorageSafetyManager.kt`.
  - **Archivos Objetivo Exactos:**
    - `app-receiver/src/main/kotlin/com/omnicast/receiver/directstream/TvStorageSafetyManager.kt`
    - `app-receiver/src/main/kotlin/com/omnicast/receiver/ui/components/TvPvrLibrarySummaryBar.kt` (nuevo composable modular `< 250` líneas)
    - `app-receiver/src/test/kotlin/com/omnicast/receiver/directstream/TvStorageSafetyManagerTest.kt`
  - **Agentes Involucrados:** `planner-architect` (`Argon`) ➔ `specialist-tv-player-pvr` (`flash`) ➔ `surgical-developer` (`flash`) ➔ `qa-oracle-tester` (`flash`) ➔ `sync-documenter` (`flash`) ➔ `git-ota-releaser` (`flash_lite`).

- `[ ]` **Actividad 3.2 `[ver]` — Reanudación Inteligente y Telemetría Enriquecida de Descargas VOD/PVR (`core/protocol/src/main/kotlin/com/omnicast/core/protocol/SignalingMessage.kt`, `app-receiver/src/main/kotlin/com/omnicast/receiver/directstream/PvrStreamRecorder.kt`, `app-receiver/src/main/kotlin/com/omnicast/receiver/directstream/TvWebPvrBridge.kt`):**
  - **Objetivo:** Persistir metadatos de progreso de descarga y permitir reanudación segura ante micro-cortes de red en flujos VOD.
  - **Archivos Objetivo Exactos:**
    - `core/protocol/src/main/kotlin/com/omnicast/core/protocol/SignalingMessage.kt`
    - `app-receiver/src/main/kotlin/com/omnicast/receiver/directstream/PvrResumeMetadataStore.kt`
    - `app-receiver/src/main/kotlin/com/omnicast/receiver/directstream/PvrStreamRecorder.kt`
    - `app-receiver/src/main/kotlin/com/omnicast/receiver/directstream/TvWebPvrBridge.kt`
  - **Agentes Involucrados:** `planner-architect` (`Argon`) ➔ `specialist-tv-player-pvr` (`flash`) ➔ `surgical-developer` (`flash`) ➔ `qa-oracle-tester` (`flash`) ➔ `sync-documenter` (`flash`) ➔ `git-ota-releaser` (`flash_lite`).

---

## 🧪 FASE 4 — Cobertura Cuantitativa de Tests y Telemetría Proactiva (`[ ]`)

- `[ ]` **Actividad 4.1 `[auto]` — Ampliación del Oráculo Unitario y Métricas de Regresión por Pilar (`build.gradle.kts`, `core/domain/src/test/kotlin/com/omnicast/core/domain/`, `app-sender/src/test/kotlin/com/omnicast/sender/`, `app-receiver/src/test/kotlin/com/omnicast/receiver/`):**
  - **Objetivo:** Incorporar reportes cuantitativos de cobertura unitaria y expandir los casos de prueba de bordes en `:core:domain`, `:app-sender` y `:app-receiver`.
  - **Archivos Objetivo Exactos:**
    - `build.gradle.kts`
    - `core/domain/src/test/kotlin/com/omnicast/core/domain/`
    - `app-sender/src/test/kotlin/com/omnicast/sender/`
    - `app-receiver/src/test/kotlin/com/omnicast/receiver/`
  - **Agentes Involucrados:** `planner-architect` (`Argon`) ➔ `qa-oracle-tester` (`flash`) ➔ `surgical-developer` (`flash`) ➔ `sync-documenter` (`flash`) ➔ `git-ota-releaser` (`flash_lite`).
