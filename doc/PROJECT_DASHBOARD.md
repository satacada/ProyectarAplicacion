# 📊 PROJECT DASHBOARD — OmniCast Pro

**Última actualización:** 2026-09-18  
**Versión de producción actual:** v3.22 (Sender) / v3.22 (Receiver)  
**Estándar de calidad:** AGENTS.md (Archivos <= 300 líneas, Clean Architecture, 100% Offline/Local)

---

## 1. Estado de Módulos y Funcionalidades

| Módulo | Componente Clave | Estado | Capacidades |
|---|---|---|---|
| `:core:domain` | `StreamProfile`, `QualityMode` | 🟢 Estable | 5 perfiles seleccionables: 1080p@30fps Cine, 1280p@30fps, 720p@45fps, 540p@60fps y 1280p@60fps. |
| `:core:protocol` | `SignalingMessage`, `SignalingProtocolTest` | 🟢 Estable | Soporte para `smoothBuffer` en `Offer` WebRTC, candidatos ICE bidireccionales y comandos de control de reproducción. |
| `:app-sender` | `WebRtcSenderManager`, `ProjectionSession` | 🟢 Estable | Insignia visual **v3.22**; integración de buffer elástico anti-tirones configurable; bitrate dinámico adaptativo. |
| `:app-sender` | `ScreenMirrorSection`, `SenderMainScreen` | 🟢 Estable | Organización en 3 pestañas (`🌐 Web / Video`, `📱 Proyección TV`, `🖼️ Galería`), TopBar y Conexión TV siempre fijas. |
| `:app-sender` | `BrowserTopBar`, `InAppBrowserScreen`, `WvcDomSniffer` | 🟢 Estable | Captura ok.ru y YouTube sin colapsos de JSON; renovación de streams sin cortar transmisión activa de TV; favoritos protegidos en historial. |
| `:app-sender` | `LocalImageServer`, `PhotoCastViewerActivity` | 🟢 Estable | Envío de fotos y videos locales vía copia en caché física (resuelve SecurityException de Android 14); soporte HTTP 206 Range. |
| `:app-sender` | `FloatingViewBuilder`, `FloatingIndicatorService` | 🟢 Estable | Menú de asistente flotante reordenado exactamente (8 opciones, con acceso a Proyección Total y Browser Omni). |
| `:app-receiver` | `TvReceiverActivity`, `WebRtcReceiverManager` | 🟢 Estable | Insignia visual **v3.22**; `PeerConnectionFactory.initializeFieldTrials` dinámico para jitter buffer. |
| `:app-receiver` | `ReceiverLogger`, `TvScreens` | 🟢 Estable | Log HTTP con timestamps en `:8087` y pantalla de bienvenida con distintivo **v3.22**. |

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
| `OmniCast-Sender-Mobile-v3.22.apk` | ~52.3 MB | **v3.22** | **Rediseño por Pestañas, Fix ok.ru, Fotos/Videos Locales y Menú Asistente:**<br>• **Rediseño con Pestañas:** Organización modular en 3 pestañas (`🌐 Web / Video`, `📱 Proyección TV`, `🖼️ Galería`), manteniendo fijos la cabecera, el banner verde y la tarjeta de conexión a TV.<br>• **Fix Captura ok.ru:** Eliminado el bug de parseo JSON (`JSONException` por prefijo `OMNICAST_STREAM:`), ampliado el umbral de detección de fuentes múltiples de video hasta 15 y añadido hook al setter `HTMLMediaElement.prototype.src`.<br>• **Cero bloqueo al navegar:** Los streams detectados no quedan pegados al navegar a nuevos videos en ok.ru o YouTube; `ActiveStreamHolder.clearDetectedStream()` renueva la captura sin interrumpir la reproducción en la TV.<br>• **Fix Envío de Fotos y Videos locales:** Guardado y servicio seguro desde archivo en caché física en `LocalImageServer`, resolviendo definitivamente la `SecurityException` por revocación de permisos URI en Android 14.<br>• **Reordenamiento Asistente Flotante:** Reordenado a 8 opciones según especificación, incluyendo Proyectar Pantalla, Cambiar Modo TV y Abrir Browser Omni.<br>• **Historial con Favoritos Protegidos:** Botón ⭐ para marcar favoritos permanentes en el historial; limpieza segura que conserva favoritos. |
| `OmniCast-Receiver-TVBox-v3.22.apk` | ~53.6 MB | **v3.22** | **Sincronización v3.22 y Soporte Multimedia Local:**<br>• Insignia visual **v3.22** en pantalla de inicio de TV y en logs HTTP `:8087`.<br>• Compatible con streaming de video web, fotos y videos locales con streaming de rango HTTP 206 y jitter buffer WebRTC anti-tirones. |
| `OmniCast-Sender-Mobile-v3.21.apk` | ~52.4 MB | **v3.21** | Versión preliminar con buffer elástico. |
| `OmniCast-Receiver-TVBox-v3.21.apk` | ~53.7 MB | **v3.21** | **Jitter Buffer Anti-Tirones y Badge v3.21:**<br>• Soporte de `smoothBuffer` vía SDP/WebRTC con `PeerConnectionFactory.initializeFieldTrials("WebRTC-ForcePlayoutDelay/min_ms:100,max_ms:250/")` para absorber fluctuaciones de WiFi y micro-cortes sin lag perceptibles.<br>• Identificador visual **v3.21** en pantalla inicial y logs. |
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
