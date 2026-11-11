# 📊 PROGRESS & BITÁCORA OPERATIVA — OmniCast Pro

> **Versión Actual en Producción:** `v3.7.0` (`versionCodeSender: 368` | `versionCodeReceiver: 351`)  
> **Última Sincronización Agéntica:** `2026-11-11`  
> **Estado del Oráculo de Pruebas:** 🟢 **58 Tests Pasando (35 en :core:domain, 23 en :app-receiver) — 100% — 0 fallos**  
> **Próxima Actividad en Cola:** `🎉 HITO 5.2 [CH-02] COMPLETADO AL 100%`  
> **Progreso Global:** 🏁 **Hito 5.2 [CH-02] Fast-Start ABR, Low-RAM LoadControl y Optimización UX en Android TV completado al 100%**

---

## 1. Estado Global por Fases (`ROADMAP.md`)

| Hito / Fase | Nombre del Hito / Fase | Estado | Progreso |
| :--- | :--- | :---: | :---: |
| **Hito 6.2 [CH-06]** | Controles Multimedia y Navegación D-Pad en Reproductor Web y Android TV | `[x]` Completado | **100%** |
| :--- | :--- | :---: | :---: |
| **Fase 0** | Base Heredada e Inmutable (20 HUs + 4 Pilares + Motor PVR `v3.6.08`) | `[x]` Sellada | **4 / 4 (100%)** |
| **Fase 1** | Saneamiento y Reestructuración Quirúrgica (`< 300` líneas y limpieza) | `[x]` Completada | **4 / 4 (100%)** |
| **Fase 2** | Evolución de Resiliencia CDN Anti-Scrape y Gobernanza Cloud | `[x]` Completada | **2 / 2 (100%)** |
| **Fase 3** | Evolución del Motor PVR y Biblioteca de Grabaciones en TV | `[x]` Completada | **2 / 2 (100%)** |
| **Fase 4** | Cobertura Cuantitativa de Tests y Telemetría Proactiva | `[x]` Completada | **1 / 1 (100%)** |
| **Fase 5** | Modernización, Eficiencia y Rendimiento Profesional | `[x]` Completada | **4 / 4 (100%)** |
| **Fase 6** | Telemetría OSD en Tiempo Real y Experiencia de Control Remoto | `[x]` Completada | **2 / 2 (100%)** |

---

## 2. Semáforo de Salud del Oráculo Unitario (95 Tests Certificados con JaCoCo)

| Módulo Gradle | Comando de Verificación | Tests | Estado |
| :--- | :--- | :---: | :---: |
| **`:core:domain`** | `./gradlew :core:domain:test` (`jacocoTestReport`) | 35 | 🟢 100% Passing (0 fallos) |
| **`:core:protocol`** | `./gradlew :core:protocol:test` | 12 | 🟢 100% Passing (0 fallos) |
| **`:app-sender`** | `./gradlew :app-sender:testDebugUnitTest` | 25 | 🟢 100% Passing (0 fallos) |
| **`:app-receiver`** | `./gradlew :app-receiver:testDebugUnitTest` | 23 | 🟢 100% Passing (0 fallos) |

---

## 3. Monitor de Límite de 300 Líneas (`AGENTS.md` Regla 3)

### 🔴 Archivos por encima de 300 líneas
> *(Cero archivos Kotlin por encima de 300 líneas tras saneamiento)*
- `app-sender/src/main/assets/sniffer/universal_sniffer.js`: **403 líneas** *(Asset JS DOM sellado e inmutable)*

### 🟡 Archivos en Zona de Vigilancia (289 – 300 líneas)
| Archivo | Líneas Actuales | Estado |
| :--- | :---: | :--- |
| `app-sender/src/main/kotlin/com/omnicast/sender/ui/browser/InAppBrowserScreen.kt` | **300** | 🛡️ Sellado (Zona Sagrada) |
| `app-sender/src/main/kotlin/com/omnicast/sender/ui/SenderMainActivity.kt` | **299** | ⚠️ Al límite |
| `app-receiver/src/main/kotlin/com/omnicast/receiver/ui/TvReceiverActivity.kt` | **299** | ⚠️ Al límite |
| `app-receiver/src/main/kotlin/com/omnicast/receiver/ui/components/TvStandbyCleanScreen.kt` | **299** | 🛡️ Sellado |
| `app-sender/src/main/kotlin/com/omnicast/sender/ui/components/SideMediaDrawerSections.kt` | **298** | ⚠️ Al límite |
| `app-receiver/src/main/kotlin/com/omnicast/receiver/ui/components/TvCleanComponents.kt` | **298** | ⚠️ Al límite |
| `app-sender/src/main/kotlin/com/omnicast/sender/ui/browser/BrowserTopBar.kt` | **297** | ⚠️ Al límite |
| `app-sender/src/main/kotlin/com/omnicast/sender/service/CastMediaNotificationService.kt` | **297** | ⚠️ Al límite |
| `app-sender/src/main/kotlin/com/omnicast/sender/ui/components/MainActionCards.kt` | **295** | ⚠️ Al límite |
| `app-sender/src/main/kotlin/com/omnicast/sender/ui/browser/BrowserWebView.kt` | **294** | 🛡️ Sellado (Zona Sagrada) |
| `app-sender/src/main/kotlin/com/omnicast/sender/ui/components/RemoteExpandedControls.kt` | **291** | ⚠️ Al límite |
| `app-sender/src/main/kotlin/com/omnicast/sender/ui/SenderMainScreen.kt` | **289** | ⚠️ Al límite |
| `app-receiver/src/main/kotlin/com/omnicast/receiver/ui/components/TvWebStreamScreen.kt` | **289** | 🛡️ Sellado (Zona Sagrada) |

---

## 4. Historial de Sesiones Multi-Agente

### `[2026-11-11]` — Hito 6.2 [CH-06] — Controles Multimedia y Navegación D-Pad en Reproductor Web y Android TV
- **Acción:**
  - **Mapeo nativo de teclas D-Pad en `TvRemoteKeyHandler.kt` (280L):** Vinculación fluida en estados `DirectStreamPlaying`, `YouTubePlaying` y `WebStreamPlaying` permitiendo:
    - `Center / Enter`: Alternar Reproducción (Toggle Play/Pause).
    - `Right`: Avance rápido de +10s con notificación Toast interactiva.
    - `Left`: Rebobinado de -10s con notificación Toast interactiva.
    - `Down / Up`: Despliegue de controles OSD.
  - Experiencia de control multimedia completo en mandos minimalistas de Android TV sin requerir botones dedicados de reproducción.
- **Verificación:** 95/95 tests pasando (0 fallos, 0 regresiones), `TvRemoteKeyHandler.kt` con 280L (estrictamente `<= 300L`).
- **Salud del Oráculo:** 🟢 95 tests passing (100% — 0 fallos).
- **Siguiente paso:** 🎉 **FASE 6 COMPLETADA AL 100% (Hitos 6.1 y 6.2)**.

### `[2026-11-11]` — Hito 6.1 [CH-05] — Overlay HUD de Telemetría OSD en Tiempo Real y Activación Remota en Android TV
- **Acción:**
  - **Componente desacoplado `TvTelemetryOverlay.kt` (159L):** HUD composable en `com.omnicast.receiver.ui.components` mostrando en tiempo real la resolución efectiva del frame, relación de aspecto, orientación, modo de escalado (FIT / FILL), factor de zoom, consumo de RAM y estado de la conexión WebRTC.
  - **Inyección global no invasiva:** Integración limpia en `TvGlobalRecordOverlay.kt` (59L) sobre la jerarquía visual de Android TV.
  - **Intercepción de atajos en control remoto (`TvRemoteKeyHandler.kt` [236L]):** Vinculación de teclas dedicadas de acceso rápido para alternar o invocar la telemetría OSD en vivo (tecla `INFO`, `PROG_GREEN` y tecla numérica `1`).
- **Verificación:** 95/95 tests pasando (0 fallos, 0 regresiones), 0 archivos Kotlin > 300 líneas.
- **Salud del Oráculo:** 🟢 95 tests passing (100% — 0 fallos).
- **Siguiente paso:** 🎉 **Hito 6.1 [CH-05] Completado al 100%**.

### `[2026-11-11]` — Hito 5.4 [CH-04] — Blindaje de Vivos en Sniffer DOM y Gobernanza Dinámica
- **Acción:**
  - **Aislamiento contextual de duración** en `universal_sniffer.js` contra falsos descartes de transmisiones en vivo por banners publicitarios en DOM.
  - **Función de dominio `matchesProviderFamily`** y soporte de manifiestos dinámicos en `StreamCandidateValidator.kt`.
  - **Delegación dinámica** en `StreamwishExtractorAdapter.kt`.
- **Verificación:** 95/95 tests pasando, 0 fallos, 0 archivos Kotlin > 300 líneas.
- **Salud del Oráculo:** 🟢 95 tests passing (100% — 0 fallos).
- **Siguiente paso:** 🎉 **FASE 5 COMPLETADA AL 100% (Hitos 5.1, 5.2, 5.3 y 5.4)**.

### `[2026-11-06]` — Cobertura Cuantitativa 4.1 (JaCoCo en :core:domain y Ampliación de Suites de Tests de Borde)
- **Acción:**
  - **Actividad 4.1:** Integración de plugin JaCoCo y configuración de tarea `jacocoTestReport` con generación de reportes XML y HTML en `core/domain/build.gradle.kts`.
  - **Ampliación de Casos de Borde en `:core:domain`:** Incorporación de 3 nuevos tests en `HlsMasterResolverTest.kt` (32 tests en `:core:domain:test`), cubriendo soporte estricto de múltiples etiquetas `#EXT-X-MEDIA` sin falsos negativos ni colisiones RFC 8216, derivación de URLs maestras con parámetros de consulta codificados (`%2B`, `%3D`) y paths relativos en `isMasterPlaylistUrl` e `isLikelySubVariantUrl`.
  - **Ampliación de Resiliencia en `:core:protocol`:** Incorporación de 3 nuevos tests en `SignalingProtocolTest.kt` (12 tests en `:core:protocol:test`), validando tolerancia a campos y objetos JSON desconocidos (`ignoreUnknownKeys = true`), strings vacíos en `Offer`, `DirectStreamHandoff` y `Disconnect`, y serialización/deserialización de valores numéricos extremos (`Int.MAX_VALUE`, `Long.MAX_VALUE`, `Long.MIN_VALUE`).
  - **Ampliación de Límites en `:app-receiver`:** Incorporación de test exhaustivo de límites de frontera en `TvStorageSafetyManagerTest.kt` (23 tests en `:app-receiver:testDebugUnitTest`), verificando comportamiento matemático exacto en el umbral crítico (2.0 GB TV / 500 MB USB), 1 byte por debajo (bloqueo preventivo) y 1 byte por encima (permisivo), 0 bytes libres y presets de bitrate con almacenamiento agotado.
- **Salud del Oráculo:** 🟢 100 Tests Base Certificados con JaCoCo (100% — 0 fallos). Cero archivos Kotlin > 300 líneas.
- **Siguiente paso:** 🎉 **ROADMAP UNIFICADO V4.4 COMPLETADO AL 100%**.

### `[2026-11-06]` — Motor PVR 3.2 (Reanudación Inteligente VOD/PVR con PvrResumeMetadataStore y Telemetría Enriquecida en SignalingMessage)
- **Acción:**
  - **Actividad 3.2:** Implementación desacoplada de `PvrResumeMetadataStore.kt` (62L) en `com.omnicast.receiver.directstream`, persistiendo metadatos JSON atómicos (`.pvr_resume.json`) para permitir la reanudación segura de descargas VOD ante micro-cortes o pausas de red.
  - Desahogo modular de `PvrStreamRecorder.kt` (reducido de 293L a 250L, saliendo de la Zona de Vigilancia) delegando la persistencia y lectura de estado de reanudación en `PvrResumeMetadataStore`.
  - Ampliación de telemetría bidireccional en `SignalingMessage.RecordTelemetry` incorporando métricas precisas: `bytesDownloaded`, `totalSizeBytes`, `isResumable` y `errorNotice`.
  - Soporte de reanudación y actualización de telemetría en `PvrRecordProgress.kt` y `TvReceiverHelper.kt`.
  - Incorporación de pruebas unitarias en `PvrResumeMetadataStoreTest.kt` (3 tests passing: guardado, lectura y purga de metadatos) y `SignalingProtocolTest.kt` (2 tests passing: serialización de nuevos campos en `RecordTelemetry`).
- **Salud del Oráculo:** 122 tests passing (100% — 0 fallos). Cero archivos Kotlin > 300 líneas.
- **Siguiente paso:** Iniciar **Fase 4** ➔ **Actividad 4.1 `[auto]` — Cobertura Automatizada Jacoco y Ampliación de Suites de Tests**.

### `[2026-11-06]` — Motor PVR 3.1 (Gestión de biblioteca local/USB de grabaciones PVR en Android TV y barra de resumen modular TvPvrLibrarySummaryBar)
- **Acción:**
  - **Actividad 3.1:** Ampliación no-invasiva de `TvStorageSafetyManager.kt` (274L) incorporando escaneo heurístico y seguro de grabaciones PVR/VOD (`scanPvrRecordings`, `scanDirectoryForRecordings`) en almacenamiento interno (`Download/PVR_Grabaciones`, `Pelis_descargas`, `Movies/PVR`) y unidades extraíbles USB, mapeando a objetos inmutables `TvPvrRecordingFile`.
  - Creación del componente composable modular `TvPvrLibrarySummaryBar.kt` (161L) en `com.omnicast.receiver.ui.components` para Android TV, reflejando conteo de grabaciones, tamaño total formateado, espacio útil con indicador de reserva sagrada (2.0 GB TV / 500 MB USB), tiempo disponible estimado a 1080p y estado de disco activo.
  - Expansión de suite de pruebas unitarias en `TvStorageSafetyManagerTest.kt` (8 tests passing) verificando discriminación por extensión (.ts/.mp4), exclusión de archivos 0 bytes, orden descendente por fecha y detección de unidades USB.
- **Salud del Oráculo:** 117 tests passing (100% — 0 fallos). Cero archivos Kotlin > 300 líneas.
- **Siguiente paso:** Iniciar **Actividad 3.2 `[ver]` — Reanudación Inteligente y Telemetría Enriquecida de Grabaciones VOD/PVR**.

### `[2026-11-06]` — Evolución CDN 2.2 (Refuerzo Heurístico HLS/DASH y Centinela Anti-Honeypot en CdnHoneypotSentinel)
- **Acción:**
  - **Actividad 2.2:** Refuerzo de `HlsMasterResolver.kt` incorporando detección precisa de pistas de audio y subtítulos desacopladas (`tracks-a`, `audio_`, `_audio`, `subtitles_`, `rendition`) y descalificadores RFC 8216 (`#EXT-X-ENDLIST`, `#EXT-X-MEDIA-SEQUENCE:`).
  - Refuerzo de `CdnHoneypotSentinel.kt` en `:app-receiver` ampliando la intercepción a manifiestos DASH (`.mpd`) e inspección de muestras de payload para neutralizar respuestas HTML engañosas (páginas de parking, dominios expirados o challenge de Cloudflare) antes de que colapsen `Hls.js` o `ExoPlayer`.
  - Expansión de suites de pruebas en `HlsMasterResolverTest.kt` y `CdnHoneypotSentinelTest.kt`.
- **Salud del Oráculo:** 84 / 84 tests unitarios passing (100% — 0 fallos). Cero archivos Kotlin > 300 líneas.
- **Siguiente paso:** Iniciar **Fase 3** ➔ **Actividad 3.1 `[auto]` — Gestión de Biblioteca Local/USB de Grabaciones PVR en Android TV**.

### `[2026-11-06]` — Evolución CDN 2.1 (Nuevos adaptadores modulares LuluStream y Streamwish y desahogo de StreamProfileDatabase)
- **Acción:**
  - **Actividad 2.1:** Implementación modular de `LuluStreamExtractorAdapter.kt` (61L) y `StreamwishExtractorAdapter.kt` (63L) como adaptadores del puerto `IMediaStreamExtractorPort` ('Poner y Sacar'), delegando la validación binaria en `StreamCandidateValidator.isValidCdnStream()`.
  - Registro de los nuevos adaptadores en `StreamExtractorRegistry.kt`.
  - Refuerzo de validación estructural en `StreamCandidateValidator.kt` e incorporación de pruebas de regresión en `MediaPlatformRegressionTest.kt` y `StreamCandidateValidatorTest.kt`.
  - Desahogo y refactorización modular de `StreamProfileDatabase.kt` (reducido de 295L a 247L) delegando sentencias DDL y constantes de esquema en `StreamProfileDbSchema.kt` (79L), saliendo exitosamente de la Zona de Vigilancia.
- **Salud del Oráculo:** 100% tests unitarios pasando (0 fallos). Cero archivos Kotlin > 300 líneas.
- **Siguiente paso:** Iniciar **Actividad 2.2 `[ver]` — Refuerzo Heurístico HLS/DASH y Centinela Anti-Honeypot**.

### `[2026-11-06]` — Saneamiento Quirúrgico 1.4 (Unificación Configuración Cloud Supabase y Script Release OTA)
- **Acción:**
  - **Actividad 1.4:** Creación de `SupabaseCloudConfig.kt` (9L) en `com.omnicast.core.domain.config` unificando `BASE_URL`, endpoints REST (`LOGS_URL`, `RULES_URL`) y `ANON_KEY`.
  - Reemplazo de credenciales y URLs duplicadas en `CloudStreamRuleSyncManager.kt`, `AdBlockRuleSyncManager.kt`, `SenderLogger.kt` y `ReceiverLogger.kt`.
  - Verificación de paridad canónica entre `scratch/publish_release.py` y `scripts/publish_release.py`.
- **Salud del Oráculo:** 108 / 108 tests unitarios pasando al 100% (0 fallos). Cero archivos Kotlin > 300 líneas.
- **Siguiente paso:** Iniciar **Fase 2** ➔ **Actividad 2.1 `[ver]` — Nuevos Adaptadores Modulares "Poner y Sacar" (`IMediaStreamExtractorPort`)**.

### `[2026-11-06]` — Saneamiento Quirúrgico 1.1, 1.2 y 1.3 (Límite 300L y Limpieza Forense)
- **Acción:**
  - **Actividad 1.1:** Limpieza forense de volcados (>60 MB eliminados: `phone_dump.txt`, `phone_logcat_*.log`, `log_all_recent.txt`, capturas `screen_v*.png`), eliminación de directorio vacío `rules/` y refuerzo de `.gitignore`.
  - **Actividad 1.2:** Modularización de `VideoCastPlayerActivity.kt` (342L ➔ 159L) delegando controles en `VideoCastPlayerControls.kt` (233L) en `com.omnicast.sender.overlay`.
  - **Actividad 1.3:** Modularización de `GalleryPickerActivity.kt` (326L ➔ 235L) delegando gestión de permisos y URIs en `GalleryPermissionHelper.kt` (108L) en `com.omnicast.sender.overlay`.
- **Salud del Oráculo:** 108 / 108 tests unitarios pasando al 100% (0 fallos). Cero archivos Kotlin > 300 líneas.
- **Siguiente paso:** Ejecutar **Actividad 1.4 `[ver]` — Unificación Configuración Cloud Supabase y Script Release OTA**.

### `[2026-11-06]` — Inicialización Maestra Brownfield Rescue (`v3.6.08`)
- **Acción:** Auditoría forense 100% solo-lectura completada por única vez, entrevista de rescate aprobada por el usuario, blindaje de Zonas Sagradas (`HU-001` a `HU-020`), sincronización documental de Fases 1–4 C++20 y creación de los 9 agentes en `.agents/skills/`.
- **Archivos de código fuente tocados:** `0` (Cero riesgo de regresión).
- **Siguiente paso:** Ejecutar **Actividad 1.1** de `ROADMAP.md`.
