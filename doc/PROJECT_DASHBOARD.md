# 📊 PROJECT DASHBOARD — OmniCast Pro

**Última actualización:** 2026-09-17  
**Versión de producción actual:** v3.19 (Sender) / v3.19 (Receiver)  
**Estándar de calidad:** AGENTS.md (Archivos <= 300 líneas, Clean Architecture, 100% Offline/Local)

---

## 1. Estado de Módulos y Funcionalidades

| Módulo | Componente Clave | Estado | Capacidades |
|---|---|---|---|
| `:core:domain` | `StreamProfile`, `UrlStreamExtractor` | 🟢 Estable | 4 perfiles seleccionables: 1280p@30fps, 720p@45fps, 540p@60fps y 1280p@60fps experimental. |
| `:core:protocol` | `SignalingMessage`, `QualityOptions` | 🟢 Estable | Protocolo de señalización WebSocket JSON tipado con soporte de fotos y zoom táctil. |
| `:app-sender` | `WebRtcSenderManager`, `ProjectionSession` | 🟢 Estable | Insignia visual **v3.19** en barra superior y menú flotante; universal `createScreenCaptureIntent()` restaurado para activar el diálogo de Google en cualquier Android/MIUI. |
| `:app-sender` | `ScreenMirrorSection`, `SenderMainScreen` | 🟢 Estable | Selector de Proporción TV: "📺 Llenar 16:9 (TV)" vs "📱 Original (Fit)", interactivo y persistente. |
| `:app-sender` | `PhotoCastViewerActivity`, `LocalImageServer` | 🟢 Estable | Envío de fotos vía HTTP Server local (IP WiFi `192.168.0.*`) compatible 100% con Receiver. |
| `:app-sender` | `FloatingIndicatorService`, `FloatingViewBuilder` | 🟢 Estable | Botón "📺 Cambiar Modo TV (16:9 / Fit)" y badge `OmniCast v3.19`. |
| `:app-sender` | `InAppBrowserScreen`, `AdBlocker`, `ActiveStreamHolder` | 🟢 Estable | Purga de modales y banners clickjacking (Pelismart), limpieza automática al cambiar de web. |
| `:app-receiver` | `WebRtcReceiverManager` | 🟢 Estable | **Eliminado `setFpsReduction(0f)`** (que forzaba `isRenderingPaused = true` en WebRTC causando pantalla negra); `setZOrderMediaOverlay(true)` idéntico a v3.4. |
| `:app-receiver` | `TvYouTubeScreen` | 🟢 Estable | YouTube IFrame Player oficial sin Error 152-4, control remoto (Play/Pausa/Seek) y reporte de tiempo. |
| `:app-receiver` | `TvScreens`, `TvReceiverActivity` | 🟢 Estable | Insignia visual **v3.19** prominente en pantalla de espera y conectado; log de inicio `[INIT] === OmniCast TV Box Receiver v3.19 Iniciado ===`. |

---

## 2. Matriz de Compatibilidad Android y Detección Dinámica

| Dispositivo | Versión Mínima (minSdk) | Versión Target (targetSdk) | Estrategia de Adaptación |
|---|---|---|---|
| **Sender (Móvil)** | **Android 8.0 (API 26)** | Android 14 (API 34) | • **Android 10+:** Captura digital de audio interno nativa.<br>• **Android 8/9:** Fallback automático seguro. |
| **Receiver (TV Box)** | **Android 7.0 (API 24)** | Android 14 (API 34) | • Compatible con cualquier Android TV Box. |

---

## 3. Catálogo de Releases Archivados (`releases/`)

| Archivo APK | Tamaño | Versión | Notas de Entrega |
|---|---|---|---|
| `OmniCast-Sender-Mobile-v3.19.apk` | ~52.4 MB | **v3.19** | **Identificador Visual v3.19 y Fix Diálogo de Google:** Badge prominente v3.19 en cabecera y botón flotante; restaurado `createScreenCaptureIntent()` canónico sin `MediaProjectionConfig` para que el diálogo del sistema aparezca inmediatamente en Xiaomi/MIUI y cualquier Android. |
| `OmniCast-Receiver-TVBox-v3.19.apk` | ~53.7 MB | **v3.19** | **Fix Definitivo Pantalla Negra:** Eliminada la llamada a `setFpsReduction(0f)` que ponía `isRenderingPaused = true` descartando el 100% de los frames; badge `v3.19` en pantalla de TV y log de inicio HTTP en `:8087`. |
| `OmniCast-Sender-Mobile-v3.17.apk` | ~52.4 MB | **v3.17** | Selector de 4 perfiles a demanda (1280p@30, 720p@45, 540p@60 y 1280p@60 experimental), persistencia en SharedPreferences y escala adaptativa. |
| `OmniCast-Sender-Mobile-v3.16.apk` | ~52.4 MB | **v3.16** | Fix de colapso de encoder Snapdragon 685 (fijado a 30fps / 3.5Mbps). |
| `OmniCast-Sender-Mobile-v3.15.apk` | ~52.4 MB | **v3.15** | Compatibilidad v3.4/v3.11 en casteo (sin pantalla negra), LocalImageServer para fotos, purga de modales spam en Pelismart y limpieza de URLs entre páginas. |
| `OmniCast-Receiver-TVBox-v3.15.apk` | ~53.7 MB | **v3.15** | Versión receiver sincronizada con v3.15. |
| `OmniCast-Sender-Mobile-v3.14.apk` | ~52.4 MB | **v3.14** | Sincronización de volumen y controles en barra inferior, botón "Desactivar Asistente", envío de fotos Base64. |
| `OmniCast-Receiver-TVBox-v3.14.apk` | ~53.7 MB | **v3.14** | Controles Play/Pausa/Seek en YouTube TV, volumen real del televisor con `AudioManager`, fix pantalla negra SurfaceView. |
| `OmniCast-Sender-Mobile-v3.13.apk` | ~52.4 MB | **v3.13** | WebRTC v3.4 exacto, fix botón verde congelado, servidor de fotos autoiniciado. |
| `OmniCast-Receiver-TVBox-v3.13.apk` | ~53.6 MB | **v3.13** | Eliminado Error 152-4 de YouTube, decodificación WebRTC v3.4 blindada. |

---

## 4. Referencia de Ingeniería Inversa Permanente
- Documentación estática registrada en `docs/references/OMNICAST_V3.4_REFERENCE.md` para evitar consumo de tokens en tareas repetitivas.

---

## 5. Gobernanza y Verificación Continua
- **Prueba del Oráculo:** `.\gradlew.bat assembleDebug` y `:core:domain:test` validados con éxito (código 0).
- **Límite de líneas:** 100% de archivos en `:app-sender`, `:app-receiver` y `:core` están por debajo de 200-300 líneas.
- **Fail Fast:** Conexiones WebSocket y WebRTC tolerantes a fluctuaciones de red sin cierre abrupto.
