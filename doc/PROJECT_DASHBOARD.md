# 📊 PROJECT DASHBOARD — OmniCast Pro

**Última actualización:** 2026-09-17  
**Versión de producción actual:** v3.12 (Sender & Receiver)  
**Estándar de calidad:** AGENTS.md (Archivos <= 300 líneas, Clean Architecture, 100% Offline/Local)

---

## 1. Estado de Módulos y Funcionalidades

| Módulo | Componente Clave | Estado | Capacidades |
|---|---|---|---|
| `:core:domain` | `StreamProfile`, `IP2PTransport` | 🟢 Estable | Modelos puros y puertos desacoplados sin dependencias de Android. |
| `:core:protocol` | `SignalingMessage`, `QualityOptions` | 🟢 Estable | Protocolo de señalización WebSocket JSON tipado. |
| `:app-sender` | `WebRtcSenderManager`, `ProjectionSession` | 🟢 Estable | Espejo WebRTC de ultra baja latencia (<80ms), bitrate dinámico 4-15 Mbps, compatible con todos los SoCs. |
| `:app-sender` | `BrowserWebView`, `WvcDomSniffer` | 🟢 Estable | Interceptor de streams estilo Web Video Caster, popups de anuncios aislados en pestañas secundarias con botón de descarte. |
| `:app-sender` | `FloatingIndicatorService` | 🟢 Estable | Asistente flotante con `ScrollView`, barra lateral izquierda y altura adaptativa. |
| `:app-receiver` | `WebRtcReceiverManager` | 🟢 Estable | Decodificación por hardware H.264 sobre `SurfaceViewRenderer`, soporte de alternancia FIT / FILL (16:9) con botón OK. |
| `:app-receiver` | `ExoPlayerTvManager` | 🟢 Estable | Reproducción directa de streams HLS/MP4 con reintentos limpios y selector de subtítulos. |
| `:app-receiver` | `TvYouTubeScreen` | 🟢 Estable | Reproductor oficial IFrame API sin congelamiento en logo de YouTube, bypass de autoplay y subtítulos desactivados por defecto. |
| `:app-receiver` | `ReceiverLogger` | 🟢 Estable | Servidor HTTP de telemetría en vivo en puerto 8087 (`http://<tv_ip>:8087/logs`). |

---

## 2. Matriz de Compatibilidad Android y Detección Dinámica

| Dispositivo | Versión Mínima (minSdk) | Versión Target (targetSdk) | Estrategia de Adaptación |
|---|---|---|---|
| **Sender (Móvil)** | **Android 8.0 (API 26)** | Android 14 (API 34) | • **Android 10+:** Captura digital de audio interno nativa (`AudioPlaybackCaptureConfiguration`).<br>• **Android 8/9:** Espejo de video fluido sin fallas y fallback seguro de audio. |
| **Receiver (TV Box)** | **Android 7.0 (API 24)** | Android 14 (API 34) | • Compatible con TV Boxes antiguas (Android 7, 8, 9 Amlogic/Rockchip/Allwinner) y Android TV / Google TV modernos (Android 10, 11, 12). |

---

## 3. Catálogo de Releases Archivados (`releases/`)

| Archivo APK | Tamaño | Versión | Notas de Entrega |
|---|---|---|---|
| `OmniCast-Sender-Mobile-v3.12.apk` | ~52.2 MB | **v3.12** | Soporte Android 9/8 (`minSdk=26`), ScrollView con barra izquierda en asistente flotante, fix Pelismart & OK.ru. |
| `OmniCast-Receiver-TVBox-v3.12.apk` | ~53.7 MB | **v3.12** | YouTube con autoplay sin trabas en logo, modo FILL 16:9 para quitar franjas, base WebRTC v3.4 blindada. |
| `OmniCast-Sender-Mobile-v3.11.apk` | ~52.2 MB | v3.11 | Corrección de `beginCapture()` para evitar saturación de encoder. |
| `OmniCast-Receiver-TVBox-v3.4.apk` | ~53.6 MB | v3.4 | Versión de referencia histórica de casteo. |

---

## 4. Gobernanza y Verificación Continua
- **Prueba del Oráculo:** `.\gradlew.bat assembleDebug --no-daemon -x lint` (Validado exitoso en 1m 56s).
- **Límite de líneas:** 100% de archivos en `:app-sender`, `:app-receiver` y `:core` están por debajo de 200-300 líneas.
- **Fail Fast:** Conexiones WebSocket y WebRTC tolerantes a fluctuaciones de red sin cierre abrupto.
