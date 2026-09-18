# 📊 PROJECT DASHBOARD — OmniCast Pro

**Última actualización:** 2026-09-17  
**Versión de producción actual:** v3.20 (Sender) / v3.20 (Receiver)  
**Estándar de calidad:** AGENTS.md (Archivos <= 300 líneas, Clean Architecture, 100% Offline/Local)

---

## 1. Estado de Módulos y Funcionalidades

| Módulo | Componente Clave | Estado | Capacidades |
|---|---|---|---|
| `:core:domain` | `StreamProfile`, `UrlStreamExtractor` | 🟢 Estable | 4 perfiles seleccionables: 1280p@30fps, 720p@45fps, 540p@60fps y 1280p@60fps experimental. |
| `:core:protocol` | `SignalingMessage`, `QualityOptions` | 🟢 Estable | Protocolo de señalización WebSocket JSON tipado con soporte de candidatos ICE bidireccionales y fotos. |
| `:app-sender` | `WebRtcSenderManager`, `ProjectionSession` | 🟢 Estable | Insignia visual **v3.20** en barra superior y menú flotante; cálculo exacto de proporciones en `calcCaptureDimensions()` preservando aspect ratio nativo del teléfono (evita marcos recortados); reconexión/cierre de ICE limpio sin zombies. |
| `:app-sender` | `ScreenMirrorSection`, `SenderMainScreen` | 🟢 Estable | Selector de Proporción TV: "📺 Llenar 16:9 (TV)" vs "📱 Original (Fit)", interactivo y persistente. |
| `:app-sender` | `PhotoCastViewerActivity`, `LocalImageServer` | 🟢 Estable | Envío de fotos vía HTTP Server local (IP WiFi `192.168.0.*`) compatible 100% con Receiver. |
| `:app-sender` | `FloatingIndicatorService`, `FloatingViewBuilder` | 🟢 Estable | Botón "📺 Cambiar Modo TV (16:9 / Fit)" y badge `OmniCast v3.20`. |
| `:app-sender` | `InAppBrowserScreen`, `AdBlocker`, `ActiveStreamHolder` | 🟢 Estable | Purga de modales y banners clickjacking (Pelismart), limpieza automática al cambiar de web. |
| `:app-receiver` | `TvReceiverActivity`, `WebRtcReceiverManager` | 🟢 Estable | **Candidatos ICE bidireccionales restaurados (`onLocalIceCandidate`)** evitando congelamientos y caídas a "Listo para conectar"; `setZOrderMediaOverlay(true)` idéntico a v3.4. |
| `:app-receiver` | `TvYouTubeScreen` | 🟢 Estable | YouTube IFrame Player oficial sin Error 152-4, control remoto (Play/Pausa/Seek) y reporte de tiempo. |
| `:app-receiver` | `TvScreens`, `TvReceiverActivity` | 🟢 Estable | Insignia visual **v3.20** prominente en pantalla de espera y conectado; log de inicio `[INIT] === OmniCast TV Box Receiver v3.20 Iniciado ===`. |

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
| `OmniCast-Sender-Mobile-v3.20.apk` | ~52.4 MB | **v3.20** | **Fix Rotación y Seguridad Android 14:** Eliminado `OrientationEventListener` y llamadas a `changeCaptureFormat()` en caliente para prevenir la `SecurityException` de Android 14; rotación nativa fluida sin cortes; delegación de control de escala (Fit / 16:9) al receptor vía WebSocket. Badge visual **v3.20**. |
| `OmniCast-Receiver-TVBox-v3.20.apk` | ~53.7 MB | **v3.20** | **Fix 16:9 Real y Candidatos ICE:** Cálculo matemático del factor de escala ($AR_{frame} / AR_{tv}$) en `applyDisplayTransform()` para que el modo 16:9 expanda y llene la pantalla completa eliminando las barras negras superior/inferior en celulares 20:9, y el modo Fit mantenga el 100% nativo. Candidatos ICE bidireccionales restaurados. |
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
