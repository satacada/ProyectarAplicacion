# 📊 PROJECT DASHBOARD — OmniCast Pro

**Última actualización:** 2026-09-17  
**Versión de producción actual:** v3.13 (Sender & Receiver)  
**Estándar de calidad:** AGENTS.md (Archivos <= 300 líneas, Clean Architecture, 100% Offline/Local)

---

## 1. Estado de Módulos y Funcionalidades

| Módulo | Componente Clave | Estado | Capacidades |
|---|---|---|---|
| `:core:domain` | `StreamProfile`, `UrlStreamExtractor` | 🟢 Estable | Extracción y tests automáticos validados para ok.ru, Dailymotion, Pelismart, HLS y MP4. |
| `:core:protocol` | `SignalingMessage`, `QualityOptions` | 🟢 Estable | Protocolo de señalización WebSocket JSON tipado con soporte de fotos y zoom táctil. |
| `:app-sender` | `WebRtcSenderManager`, `ProjectionSession` | 🟢 Estable | Configuración exacta v3.4 restaurada: High Profile HW, arranque inmediato y escala 1920. |
| `:app-sender` | `LocalImageServer`, `PhotoCastViewerActivity` | 🟢 Estable | Servidor local HTTP autoiniciado para fotos de galería y transmisión continua de zoom/paneo. |
| `:app-sender` | `FloatingIndicatorService`, `FloatingViewBuilder` | 🟢 Estable | TouchListener corregido (ACTION_DOWN = true), arrastre suave y menú sin congelamiento. |
| `:app-receiver` | `WebRtcReceiverManager` | 🟢 Estable | Decodificación por hardware H.264 sobre `SurfaceViewRenderer`, alternancia FIT / FILL 16:9. |
| `:app-receiver` | `TvYouTubeScreen` | 🟢 Estable | Reproductor youtube-nocookie embed limpio sin interceptor que detone Error 152-4. |

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
| `OmniCast-Sender-Mobile-v3.13.apk` | ~52.4 MB | **v3.13** | WebRTC v3.4 exacto, fix botón verde congelado, servidor de fotos autoiniciado. |
| `OmniCast-Receiver-TVBox-v3.13.apk` | ~53.6 MB | **v3.13** | Eliminado Error 152-4 de YouTube, decodificación WebRTC v3.4 blindada. |

---

## 4. Gobernanza y Verificación Continua
- **Prueba del Oráculo:** `.\gradlew.bat assembleDebug --no-daemon -x lint` (Validado exitoso en 1m 56s).
- **Límite de líneas:** 100% de archivos en `:app-sender`, `:app-receiver` y `:core` están por debajo de 200-300 líneas.
- **Fail Fast:** Conexiones WebSocket y WebRTC tolerantes a fluctuaciones de red sin cierre abrupto.
