# 📊 PROJECT DASHBOARD — OmniCast Pro

**Última actualización:** 2026-09-18  
**Versión de producción actual:** v3.27 (Sender) / v3.27 (Receiver)  
**Estándar de calidad:** AGENTS.md (Archivos <= 300 líneas, Clean Architecture, 100% Offline/Local)

---

## 1. Estado de Módulos y Funcionalidades

| Módulo | Componente Clave | Estado | Capacidades |
|---|---|---|---|
| `:core:domain` | `StreamProfile`, `QualityMode` | 🟢 Estable | 5 perfiles seleccionables: 1080p@30fps Cine, 1280p@30fps, 720p@45fps, 540p@60fps y 1280p@60fps. |
| `:core:protocol` | `SignalingMessage`, `SignalingProtocolTest` | 🟢 Estable | Soporte para `smoothBuffer` en `Offer` WebRTC, candidatos ICE bidireccionales y comandos de control de reproducción. |
| `:app-sender` | `CastMediaNotificationService`, `MediaSession` | 🟢 Estable | Widget nativo en pantalla de bloqueo (`MediaStyle`) con barra de progreso interactiva, [X Cerrar], [-10s], [Play/Pause], [+10s]. |
| `:app-sender` | `NativeAdBlockEngine`, `DynamicSnifferManager` | 🟢 Estable | Motor C++ nativo AdBlockPlus (`libadblock-client.so`) para filtrado de sub-milisegundos de popups; sniffer dinámico con preservación de blobs (`window.URL.revokeObjectURL`). |
| `:app-sender` | `LocalImageServer`, `PhotoCastViewerActivity` | 🟢 Estable | Servidor HTTP local para fotos de alta resolución (`port 8090`), eliminando sobrecarga Base64 en WebSocket y permitiendo proyección instantánea. |
| `:app-sender` | `WebRtcSenderManager`, `ProjectionSession` | 🟢 Estable | Insignia visual **v3.27**; buffer elástico anti-tirones configurable; bitrate dinámico adaptativo; zoom gradual a TV. |
| `:app-receiver` | `TvPlaybackHistoryManager`, `TvHistorySection` | 🟢 Estable | Historial persistente en Android TV ("Continuar Viendo") en pantalla standby con soporte de control remoto para reproducir sin el teléfono. |
| `:app-receiver` | `PlaybackRecoveryManager`, `TvAspectRatioHelper` | 🟢 Estable | Resiliencia contra micro-cortes y `BehindLiveWindowException`; alternancia en caliente de relación de aspecto (Fit, 16:9 Llenar, Zoom). |
| `:app-receiver` | `ReceiverLogger`, `TvScreens` | 🟢 Estable | Log HTTP con timestamps en `:8087` y pantalla de bienvenida con distintivo **v3.27**. |

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
| `OmniCast-Sender-v3.27.0.apk` | ~58.7 MB | **v3.27** | **AdBlock C++ Nativo, Widget en Pantalla de Bloqueo, Fotos HTTP y Sniffer Dinámico:**<br>• **Controles en Pantalla de Bloqueo:** Notificación nativa `MediaStyle` y `MediaSessionCompat` con [Cerrar X], [-10s], [Play/Pause], [+10s] y barra de progreso sincronizada al apagar la pantalla del móvil.<br>• **Motor AdBlock C++ Nativo:** Integración de `libadblock-client.so` (núcleo AdBlockPlus en C++) interceptando peticiones en `shouldInterceptRequest` de WebView a nivel de milisegundos, bloqueando popups y ventanas trampa.<br>• **Preservación de Blobs y Sniffer Dinámico:** Interceptación de `window.URL.revokeObjectURL` para aplazar la destrucción de streams en memoria y carga dinámica de script de sniffing.<br>• **Fix Proyección de Fotos:** Servidor local HTTP (`LocalImageServer`) en puerto 8090 que reemplaza la sobrecarga de Base64, permitiendo proyección rápida y estable de fotos de cualquier tamaño.<br>• **Insignia v3.27.** |
| `OmniCast-Receiver-v3.27.0.apk` | ~53.7 MB | **v3.27** | **Historial Autónomo en TV ("Continuar Viendo"), Resiliencia de Streaming y Relación de Aspecto en Caliente:**<br>• **Historial en Pantalla Standby:** Fila "Continuar Viendo (Historial en TV)" con tarjetas navegables con D-Pad que permite reanudar o repetir reproducciones directamente desde la TV Box sin necesidad del teléfono.<br>• **Playback Recovery Manager:** Recuperación automática ante `BehindLiveWindowException` o cortes de red en streams web HLS con backoff exponencial.<br>• **Ajuste de Aspecto en Caliente:** Alternancia entre Fit, 16:9 Estirar y Zoom mediante mando a distancia.<br>• **Insignia v3.27.** |
| `OmniCast-Sender-v3.26.0.apk` | ~52.5 MB | **v3.26** | Versión previa con listas de reproducción estilo YouTube y borrado de streams. |
| `OmniCast-Receiver-v3.26.0.apk` | ~53.7 MB | **v3.26** | Versión previa de receptor. |
| `OmniCast-Sender-Mobile-v3.25.apk` | ~52.5 MB | **v3.25** | Versión previa de rotación dinámica. |
| `OmniCast-Receiver-TVBox-v3.25.apk` | ~53.7 MB | **v3.25** | Versión previa de TV Box. |
| `OmniCast-Sender-Mobile-v3.24.apk` | ~52.4 MB | **v3.24** | Versión intermedia de galería persistente y zoom. |
| `OmniCast-Receiver-TVBox-v3.24.apk` | ~53.7 MB | **v3.24** | Versión intermedia de receptor. |
| `OmniCast-Sender-Mobile-v3.23.apk` | ~52.3 MB | **v3.23** | **Persistencia de Navegador, Fotos Base64 y Fix Videos Locales:**<br>• **Persistencia y Rotación del Navegador:** Configurado `configChanges` en `AndroidManifest.xml` para que el giro del móvil no destruya el navegador; la última URL navegada se guarda en `SharedPreferences` (`saved_browser_last_url`) y se recarga automáticamente al volver a abrir el browser sin necesidad de buscarla nuevamente.<br>• **Transmisión Inmediata de Fotos:** Las fotos de la galería se codifican en Base64 JPEG optimizado a 1080p y se envían directamente por WebSocket al TV (`SignalingMessage.ShowImage`), eliminando bloqueos de puertos HTTP locales o firewall.<br>• **Fix Videos de Galería:** `LocalImageServer` ahora atiende peticiones `HEAD` de ExoPlayer sin escribir cuerpo de respuesta y selecciona la IP en la misma subred de la TV, permitiendo la reproducción fluida de videos locales.<br>• **Insignia visual v3.23** en cabecera y asistente flotante. |
| `OmniCast-Receiver-TVBox-v3.23.apk` | ~53.6 MB | **v3.23** | **Controles de Mando TV Separados y Pantalla Completa 16:9 en Cualquier Video:**<br>• **Mando TV Separado:** El botón OK / Play / Center ahora controla exclusivamente **Play / Pausa** en ExoPlayer y YouTube. Flecha derecha adelanta +10s y flecha izquierda retrocede -10s.<br>• **Modo 16:9 Llenar Pantalla vs Fit para Todos los Videos:** Con Flecha Arriba, Abajo o Tecla Menú del mando, el usuario alterna entre llenar la pantalla completa (Zoom 16:9 sin barras negras) y ajustar proporción original (Fit) en cualquier stream directo de la web o video local de galería.<br>• **YouTube sin Subtítulos por Defecto:** Los subtítulos automáticos quedan desactivados al reproducir (`unloadModule`), y pueden reactivarse/alternarse a demanda con la tecla Arriba / Menú del control remoto.<br>• **Insignia visual v3.23** en bienvenida y logs de TV. |
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
- Roadmap y auditoría de la APK Seekee registrado en `doc/references/SEEKEE_REVERSE_ENGINEERING_ROADMAP.md`.

---

## 5. Gobernanza y Verificación Continua
- **Prueba del Oráculo:** `.\gradlew.bat assembleDebug` y `:core:domain:test` validados con éxito (código 0).
- **Límite de líneas:** 100% de archivos en `:app-sender`, `:app-receiver` y `:core` están por debajo de 200-300 líneas.
- **Fail Fast:** Conexiones WebSocket y WebRTC tolerantes a fluctuaciones de red sin cierre abrupto.
