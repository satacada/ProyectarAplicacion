# 📊 PROGRESS & BITÁCORA OPERATIVA — OmniCast Pro

> **Versión Actual en Producción:** `v3.6.08` (`versionCodeSender: 359` | `versionCodeReceiver: 342`)  
> **Última Sincronización Agéntica:** `2026-11-06`  
> **Estado del Oráculo de Pruebas:** 🟢 **117 Tests Passing (100% — 0 fallos)**  
> **Próxima Actividad en Cola:** `Actividad 3.2 [ver] — Reanudación Inteligente y Telemetría Enriquecida de Grabaciones VOD/PVR`

---

## 1. Estado Global por Fases (`ROADMAP.md`)

| Fase | Nombre de la Fase | Estado | Progreso |
| :--- | :--- | :---: | :---: |
| **Fase 0** | Base Heredada e Inmutable (20 HUs + 4 Pilares + Motor PVR `v3.6.08`) | `[x]` Sellada | **4 / 4 (100%)** |
| **Fase 1** | Saneamiento y Reestructuración Quirúrgica (`< 300` líneas y limpieza) | `[x]` Completada | **4 / 4 (100%)** |
| **Fase 2** | Evolución de Resiliencia CDN Anti-Scrape y Gobernanza Cloud | `[x]` Completada | **2 / 2 (100%)** |
| **Fase 3** | Evolución del Motor PVR y Biblioteca de Grabaciones en TV | `[ ]` En progreso | **1 / 2 (50%)** |
| **Fase 4** | Cobertura Cuantitativa de Tests y Telemetría Proactiva | `[ ]` Pendiente | **0 / 1 (0%)** |

---

## 2. Semáforo de Salud del Oráculo Unitario (117 Tests Passing)

| Módulo Gradle | Comando de Verificación | Tests | Estado |
| :--- | :--- | :---: | :---: |
| **`:core:domain`** | `./gradlew :core:domain:test` | 28 | 🟢 100% Passing (0 fallos) |
| **`:core:protocol`** | `./gradlew :core:protocol:test` | 7 | 🟢 100% Passing (0 fallos) |
| **`:app-sender`** | `./gradlew :app-sender:testDebugUnitTest` | 33 | 🟢 100% Passing (0 fallos) |
| **`:app-receiver`** | `./gradlew :app-receiver:testDebugUnitTest` | 19 | 🟢 100% Passing (0 fallos) |

---

## 3. Monitor de Límite de 300 Líneas (`AGENTS.md` Regla 3)

### 🔴 Archivos por encima de 300 líneas
> *(Cero archivos Kotlin por encima de 300 líneas tras saneamiento 1.2, 1.3 y desahogo modular en Actividad 2.1; `StreamProfileDatabase.kt` se redujo de 295L a 247L y salió de vigilancia)*
- `app-sender/src/main/assets/sniffer/universal_sniffer.js`: **403 líneas** *(Asset JS DOM sellado e inmutable)*

### 🟡 Archivos en Zona de Vigilancia (289 – 300 líneas)
> **Regla para el Desarrollador Quirúrgico:** Si una actividad futura requiere agregar código en cualquiera de estos archivos, es **OBLIGATORIO** extraer primero un subcomponente o helper delegado para que el archivo nunca supere las 300 líneas.

| Archivo | Líneas Actuales | Estado |
| :--- | :---: | :--- |
| `app-sender/src/main/kotlin/com/omnicast/sender/ui/browser/InAppBrowserScreen.kt` | **300** | 🛡️ Sellado (Zona Sagrada) |
| `app-sender/src/main/kotlin/com/omnicast/sender/ui/SenderMainActivity.kt` | **299** | ⚠️ Al límite (extraer helper si se edita) |
| `app-receiver/src/main/kotlin/com/omnicast/receiver/ui/TvReceiverActivity.kt` | **299** | ⚠️ Al límite (extraer helper si se edita) |
| `app-receiver/src/main/kotlin/com/omnicast/receiver/ui/components/TvStandbyCleanScreen.kt` | **299** | 🛡️ Sellado |
| `app-sender/src/main/kotlin/com/omnicast/sender/ui/components/SideMediaDrawerSections.kt` | **298** | ⚠️ Al límite |
| `app-receiver/src/main/kotlin/com/omnicast/receiver/ui/components/TvCleanComponents.kt` | **298** | ⚠️ Al límite |
| `app-sender/src/main/kotlin/com/omnicast/sender/ui/browser/BrowserTopBar.kt` | **297** | ⚠️ Al límite |
| `app-sender/src/main/kotlin/com/omnicast/sender/service/CastMediaNotificationService.kt` | **297** | ⚠️ Al límite |
| `app-sender/src/main/kotlin/com/omnicast/sender/ui/components/MainActionCards.kt` | **295** | ⚠️ Al límite |
| `app-sender/src/main/kotlin/com/omnicast/sender/ui/browser/BrowserWebView.kt` | **294** | 🛡️ Sellado (Zona Sagrada) |
| `app-receiver/src/main/kotlin/com/omnicast/receiver/directstream/PvrStreamRecorder.kt` | **293** | 🛡️ Sellado (`v3.6.08`) |
| `app-sender/src/main/kotlin/com/omnicast/sender/ui/components/RemoteExpandedControls.kt` | **291** | ⚠️ Al límite |
| `app-sender/src/main/kotlin/com/omnicast/sender/ui/SenderMainScreen.kt` | **289** | ⚠️ Al límite |
| `app-receiver/src/main/kotlin/com/omnicast/receiver/ui/components/TvWebStreamScreen.kt` | **289** | 🛡️ Sellado (Zona Sagrada) |

---

## 4. Historial de Sesiones Multi-Agente

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
