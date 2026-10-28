# 🗺️ MAPA MAESTRO DEL SISTEMA Y GUÍA DE FLUJO DE CÓDIGO (CALL GRAPH)

> **Documento Canónico para Desarrolladores Humanos y Agentes de IA**  
> **Versión del Sistema:** `v3.5.21` | **Estado:** Producción Activa  
> **Propósito:** Proporcionar una visión integral de 360 grados de todo el código fuente: árbol de directorios, roles de cada módulo, pantallas/páginas, funciones clave y trazabilidad exacta de llamadas archivo por archivo (call graph) para la reproducción de películas, proyección de pantalla y fotos.

---

## 1. Árbol Estructural de Carpetas y Módulos del Proyecto

El proyecto está organizado como un monorepositorio Android Multi-Módulo bajo **Clean Architecture** y **Arquitectura Hexagonal (Ports & Adapters)**:

```text
proyectarAplicacion/
├── core/                               # NÚCLEO PURO COMPARTIDO (Kotlin Multiplatform / Android)
│   ├── domain/                         # Entidades de dominio, modelos puros y puertos hexagonales
│   │   └── src/main/kotlin/com/omnicast/core/domain/
│   │       ├── models/                 # MediaStream, StreamProfile, MediaBranch, DeviceTarget, etc.
│   │       ├── ports/                  # Interfaces: IMediaStreamExtractorPort, IStreamTelemetryFilterPort, etc.
│   │       └── usecases/               # DetectMediaStreamUseCase, StreamManifestProbe, UrlStreamExtractor
│   └── protocol/                       # Protocolo de señalización y control local P2P
│       └── src/main/kotlin/com/omnicast/core/protocol/
│           └── SignalingMessage.kt     # Clases selladas JSON: Offer, Answer, DirectStreamHandoff, PlayControl, etc.
│
├── app-sender/                         # APLICACIÓN EMISORA PARA TELÉFONO MÓVIL (:app-sender)
│   └── src/main/
│       ├── assets/sniffer/             # Scripts JS inyectados en el navegador
│       │   └── universal_sniffer.js    # Interceptor JS de fetch, XHR y HTMLMediaElement en tiempo real
│       ├── cpp/                        # Motor C++ nativo de alto rendimiento
│       │   ├── stream_classifier.h     # Cabeceras del clasificador Bloom Filter / Trie en C++
│       │   └── stream_classifier.cpp   # Implementación C++ de evaluación de URLs en microsegundos
│       └── kotlin/com/omnicast/sender/
│           ├── media/                  # Servidor HTTP local (:8087) para fotos y videos locales
│           ├── nativeclassifier/       # Envoltorio JNI para el clasificador C++
│           ├── network/                # Clientes WebSocket de señalización (SignalingClient, QuickSignalingHelper)
│           ├── overlay/                # Pantallas flotantes, visor de fotos, selector de galería y controles de video
│           ├── playlist/               # Gestor de listas de reproducción y favoritos locales
│           ├── service/                # Foreground Services (MediaProjection, CastSession, TvZoomManager)
│           ├── stream/                 # ActiveStreamHolder, perfiles de streams y persistencia SQLite
│           ├── ui/
│           │   ├── browser/            # NAVEGADOR WEB, ADBLOCKER Y DETECCIÓN DE MEDIOS
│           │   │   ├── extractors/     # Adaptadores hexagonales: OkRu, Fastream, HLS, DASH, YouTube, Telemetría
│           │   │   ├── AdBlocker.kt    # Interceptor de red HTTP en WebView (0ms con 200 OK vacío)
│           │   │   ├── BrowserWebView.kt # Chromium WebView configurado con blindaje anti-popup nativo
│           │   │   ├── InAppBrowserScreen.kt # Pantalla completa del navegador web con barra de herramientas
│           │   │   ├── MediaPlatformResolver.kt # Orquestador hexagonal y preparador de enlaces en caliente
│           │   │   ├── StreamQualityDetector.kt # Analizador asíncrono de variantes HLS/DASH
│           │   │   ├── WvcDomSniffer.kt # Explorador DOM de etiquetas <video> e iframes
│           │   │   ├── WvcShieldSecurityEngine.kt # Blindaje DOM anti-trampas y neutralización de velos
│           │   │   └── WvcVideoListSheet.kt # Selector modal de videos en lista plana 1:1 estándar WVC
│           │   ├── components/         # Componentes Compose: barras de control remoto, tarjetas, diálogos
│           │   ├── SenderMainActivity.kt # Actividad principal del teléfono
│           │   └── SenderMainScreen.kt # Pantalla principal con pestañas (Casteo, Navegador, Galería, Configuración)
│           └── webrtc/                 # Pipeline WebRTC para Screen Mirroring (WebRtcSenderManager)
│
├── app-receiver/                       # APLICACIÓN RECEPTORA PARA ANDROID TV / TV BOX (:app-receiver)
│   └── src/main/
│       ├── assets/                     # Bibliotecas web locales de reproducción
│       │   └── hls.light.min.js        # Librería Hls.js oficial para reproducción HTML5 en Chromium TV
│       └── kotlin/com/omnicast/receiver/
│           ├── audio/                  # DigitalAudioReceiver (reproducción de audio PCM estéreo 48kHz)
│           ├── directstream/           # ExoPlayerTvManager (reproductor de hardware nativo MediaCodec)
│           ├── discovery/              # NsdAdvertiserManager (anuncio mDNS _omnicast._tcp)
│           ├── server/                 # LocalSignalingServer (servidor WebSocket en puerto :8088)
│           ├── ui/
│           │   ├── components/         # Pantallas Compose TV y reproductores especializados:
│           │   │   ├── TvStandbyCleanScreen.kt # Pantalla de reposo limpia con IP y estado WiFi
│           │   │   ├── TvDirectStreamScreen.kt # Vista contenedora de ExoPlayer con controles de foco
│           │   │   ├── TvWebStreamScreen.kt # Reproductor Chromium HTML5 + Hls.js con control D-Pad
│           │   │   ├── TvWebResourceInterceptor.kt # Interceptor nativo Anti-CORS y descarga de fragmentos
│           │   │   ├── TvYouTubeScreen.kt # Reproductor oficial de YouTube sin publicidad invasiva
│           │   │   ├── TvImageScreen.kt # Visor de fotos de alta resolución proyectadas desde el móvil
│           │   │   └── TvScreens.kt # Lienzo de Screen Mirroring WebRTC (Pantalla Completa vs Por App)
│           │   ├── TvReceiverActivity.kt # Actividad principal y máquina de estados de Android TV
│           │   ├── TvReceiverHelper.kt # Enrutador de reproductores, resolución de BaseURL y telemetría HTTP :8087
│           │   └── TvRemoteKeyHandler.kt # Manejo ergonómico del control remoto físico (D-Pad, Zoom, Subs)
│           └── webrtc/                 # Receptor WebRTC por hardware (WebRtcReceiverManager)
│
└── doc/                                # DOCUMENTACIÓN VIVA DEL PROYECTO
    ├── adr/                            # Architecture Decision Records (ADR-001 al ADR-013)
    ├── historias-usuario/              # Contratos de experiencia de usuario (HU-001 al HU-016)
    ├── referencias/                    # Catálogo técnico de variantes de streaming y WVC
    ├── ARCHITECTURE.md                 # Arquitectura de alto nivel (C4 Model)
    └── PROJECT_DASHBOARD.md            # Bitácora viva de versiones y estado del hardware
```

---

## 2. Los 4 Subsistemas Clave y sus Módulos

### 1. Núcleo Hexagonal (`:core:domain` y `:core:protocol`)
- **Regla Fundamental:** Cero dependencias de Android framework o UI en `:core:domain`.
- **`DetectMediaStreamUseCase.kt`:** Orquesta la detección. Primero ejecuta el filtro de telemetría e higiene (`isTelemetryOrGarbage`) y luego consulta a los adaptadores registrados en `StreamExtractorRegistry`.
- **`SignalingMessage.kt`:** Vocabulario común cliente-servidor serializado con Kotlinx Serialization:
  - `Offer` / `Answer` / `IceCandidate`: Negociación WebRTC para duplicar pantalla.
  - `DirectStreamHandoff`: Entrega la URL de una película a la TV Box con su mapa de cabeceras (`Referer`, `User-Agent`), pistas de subtítulos y rama multimedia.
  - `PlayControl`: Comandos de reproducción (`PLAY`, `PAUSE`, `SEEK`, `STOP`, `SET_VOLUME`).
  - `PlaybackStatus`: Retroalimentación de la TV hacia el teléfono (`position`, `duration`, `state`).

### 2. Emisor Móvil (`:app-sender`)
- **Navegación Web y AdBlock:** Combina un interceptor C++ nativo con el interceptor Java `AdBlocker.kt`. Suprime popups sin crear WebViews auxiliares (`onCreateWindow` retorna `false`).
- **Capturador de Medios:** Inyecta `universal_sniffer.js` en Chromium, atrapando peticiones de red transparentemente.
- **Selector 1:1:** Presenta la lista en `WvcVideoListSheet.kt` respetando cada variante detectada sin alterar las URLs legítimas ni inventar calidades ficticias.
- **Screen Mirroring:** Captura pantalla mediante `MediaProjection` y audio mediante `AudioPlaybackCapture`, codificando en hardware H.264 hacia WebRTC.

### 3. Receptor Android TV (`:app-receiver`)
- **Servidor WebSocket Local (`LocalSignalingServer.kt`):** Escucha en el puerto `8088`. No requiere internet; opera en red WiFi doméstica.
- **Reproductor de Hardware (`ExoPlayerTvManager.kt`):** Acelerado por hardware para flujos MP4 directos, IPTV y DASH (`.mpd` de OK.ru con VP9 y Opus).
- **Reproductor Web Híbrido (`TvWebStreamScreen.kt` + `TvWebResourceInterceptor.kt`):** Ejecuta Chromium con `Hls.js` para CDNs protegidas (DropCDN de PelisPlus, Fastream). Descarga fragmentos inyectando `Access-Control-Allow-Origin: *` y conservando las cabeceras exactas de navegación.
- **Telemetría en Vivo (`:8087/logs`):** Servidor HTTP local para diagnosticar fallos en la TV Box con timestamps en milisegundos.

---

## 3. Catálogo Exhaustivo de Pantallas y Componentes

| Pantalla / Componente | Archivo Fuente | Módulo | Propósito y Comportamiento |
|---|---|---|---|
| **Pantalla Principal Móvil** | [SenderMainScreen.kt](file:///home/sataca/Descargas/APLICACIONES/proyectarAplicacion/app-sender/src/main/kotlin/com/omnicast/sender/ui/SenderMainScreen.kt) | `:app-sender` | Contenedor principal con pestañas para iniciar duplicación de pantalla, abrir el navegador web o proyectar fotos de la galería. |
| **Navegador Web Integrado** | [InAppBrowserScreen.kt](file:///home/sataca/Descargas/APLICACIONES/proyectarAplicacion/app-sender/src/main/kotlin/com/omnicast/sender/ui/browser/InAppBrowserScreen.kt) | `:app-sender` | Pantalla de navegación con barra superior compacta, botón Cast naranja, botón de Play directo y botón flotante de videos detectados. |
| **Lienzo WebView de Navegación** | [BrowserWebView.kt](file:///home/sataca/Descargas/APLICACIONES/proyectarAplicacion/app-sender/src/main/kotlin/com/omnicast/sender/ui/browser/BrowserWebView.kt) | `:app-sender` | Cliente Chromium blindado. Suprime popups en 0ms (`onCreateWindow = false`), bloquea publicidad en `shouldInterceptRequest` y aisla captchas de Cloudflare. |
| **Selector de Videos WVC 1:1** | [WvcVideoListSheet.kt](file:///home/sataca/Descargas/APLICACIONES/proyectarAplicacion/app-sender/src/main/kotlin/com/omnicast/sender/ui/browser/WvcVideoListSheet.kt) | `:app-sender` | Hoja modal que lista de manera transparente cada flujo capturado (`master.m3u8`, `index-v1-a1.m3u8`, etc.) con su resolución, bitrate y botón de proyección directa. |
| **Barra de Control Remoto** | [RemoteControlBar.kt](file:///home/sataca/Descargas/APLICACIONES/proyectarAplicacion/app-sender/src/main/kotlin/com/omnicast/sender/ui/components/RemoteControlBar.kt) | `:app-sender` | Barra flotante inferior en el móvil para pausar, reanudar o detener el video proyectado en la TV Box sin salir de la navegación web. |
| **Controles Remotos Expandidos** | [RemoteExpandedControls.kt](file:///home/sataca/Descargas/APLICACIONES/proyectarAplicacion/app-sender/src/main/kotlin/com/omnicast/sender/ui/components/RemoteExpandedControls.kt) | `:app-sender` | Panel completo con barra de tiempo (seek bar), volumen, alternar subtítulos VTT y ciclar calidades HLS. |
| **Visor de Fotos Proyectadas** | [PhotoCastViewerActivity.kt](file:///home/sataca/Descargas/APLICACIONES/proyectarAplicacion/app-sender/src/main/kotlin/com/omnicast/sender/overlay/PhotoCastViewerActivity.kt) | `:app-sender` | Permite deslizar fotos con swipe en el móvil mientras se proyectan instantáneamente en la TV vía WebSocket. |
| **Pantalla de Reposo TV** | [TvStandbyCleanScreen.kt](file:///home/sataca/Descargas/APLICACIONES/proyectarAplicacion/app-receiver/src/main/kotlin/com/omnicast/receiver/ui/components/TvStandbyCleanScreen.kt) | `:app-receiver` | Pantalla limpia de espera en la TV Box mostrando la dirección IP de conexión (`192.168.0.x`), código PIN y estado del WiFi. |
| **Reproductor Web TV (HTML5)** | [TvWebStreamScreen.kt](file:///home/sataca/Descargas/APLICACIONES/proyectarAplicacion/app-receiver/src/main/kotlin/com/omnicast/receiver/ui/components/TvWebStreamScreen.kt) | `:app-receiver` | WebView en Android TV con Hls.js oficial, `baseUrl` fijado al Referer canónico y User-Agent móvil para CDNs con tokens estrictos. |
| **Interceptor Anti-CORS TV** | [TvWebResourceInterceptor.kt](file:///home/sataca/Descargas/APLICACIONES/proyectarAplicacion/app-receiver/src/main/kotlin/com/omnicast/receiver/ui/components/TvWebResourceInterceptor.kt) | `:app-receiver` | Intercepta descargas de `.m3u8` y `.ts` en la TV, inyectando `Access-Control-Allow-Origin: *` y respondiendo preflights OPTIONS en 0ms. |
| **Reproductor Hardware TV** | [TvDirectStreamScreen.kt](file:///home/sataca/Descargas/APLICACIONES/proyectarAplicacion/app-receiver/src/main/kotlin/com/omnicast/receiver/ui/components/TvDirectStreamScreen.kt) | `:app-receiver` | Lienzo de Compose que aloja `ExoPlayerTvManager` para decodificar MP4 y DASH a 60 fps sin carga de CPU. |
| **Lienzo Screen Mirroring TV** | [TvScreens.kt](file:///home/sataca/Descargas/APLICACIONES/proyectarAplicacion/app-receiver/src/main/kotlin/com/omnicast/receiver/ui/components/TvScreens.kt) | `:app-receiver` | Superficie `SurfaceViewRenderer` WebRTC para duplicar pantalla del teléfono con modos Pantalla Completa y Por Aplicación. |

---

## 4. Trazabilidad de Código Paso a Paso (Call Graph de Archivo a Archivo)

### FLUJO A: Reproducción de una Película Web (Web Video Caster)
Desde que el usuario navega a un portal (ej. `pelisplus.hair` o `ok.ru`) hasta que la película se decodifica en la TV Box:

```text
[1. Usuario toca Play en la web]
        │
        ▼
[BrowserWebView.kt]
  │  (Chromium WebView intercepta peticiones en shouldInterceptRequest)
  ├─► Consulta AdBlocker.kt / NativeAdBlockEngine.kt (Descarta anuncios en 0ms)
  └─► Inyecta assets/sniffer/universal_sniffer.js en onPageStarted / onPageFinished
        │
        ▼
[universal_sniffer.js]
  │  (Hook transparente sobre window.fetch, XMLHttpRequest.open y HTMLMediaElement.play)
  ├─► Captura la URL del manifiesto (.m3u8, .mpd) o archivo (.mp4) junto con sus headers
  └─► Invoca el puente nativo: window.OmniCastSniffer.onMediaDetected(url, mime, headersJson)
        │
        ▼
[DynamicSnifferManager.kt / WvcDomSniffer.kt]
  │  (Recibe la llamada del puente JavaScript)
  └─► Notifica a InAppBrowserScreen.kt mediante el callback onMediaDetected
        │
        ▼
[DetectMediaStreamUseCase.kt] (Núcleo Hexagonal :core:domain)
  │  (Analiza la URL candidata)
  ├─► Paso 1: TelemetryFilterAdapter.isTelemetryOrGarbage(url)
  │          Descarta /cdn-cgi/, trackers, telemetría y páginas iframe sin extensión
  └─► Paso 2: StreamExtractorRegistry.findExtractor(url)
             Enruta hacia OkRuSiteExtractorAdapter, FastreamExtractorAdapter o UniversalHlsExtractorAdapter
        │
        ▼
[StreamQualityDetector.kt]
  │  (Sondeo asíncrono y enriquecimiento de metadatos)
  ├─► Inspecciona las variantes HLS/DASH (#EXT-X-STREAM-INF) sin quemar el token
  └─► Añade la opción limpia a la lista de ActiveStreamHolder
        │
        ▼
[InAppBrowserScreen.kt / WvcVideoListSheet.kt]
  │  (Muestra el botón flotante FAB con contador de videos detectados)
  ├─► El usuario abre la hoja modal y selecciona el video deseado
  └─► Invoca MediaPlatformResolver.prepareStreamForPlayback(selectedStream)
        │
        ▼
[MediaPlatformResolver.kt]
  │  (Renovación síncrona en 0ms y preservación de cabeceras)
  ├─► Si es Fastream/DropCDN: Configura Referer canónico de embed y User-Agent adecuado
  ├─► Si es OK.ru: Inyecta Referer oficial y preserva opciones DASH/Progresivas
  └─► Entrega el flujo listo al cliente de red
        │
        ▼
[SignalingClient.kt / QuickSignalingHelper.kt]
  │  (Envía paquete JSON por socket WebSocket a la TV Box en el puerto 8088)
  └─► SignalingMessage.DirectStreamHandoff(url, headers, subtitles, branch)
        │
   [RED LOCAL WI-FI / PUERTO :8088]
        │
        ▼
[LocalSignalingServer.kt] (Android TV Box)
  │  (Recibe el paquete JSON del WebSocket y lo deserializa)
  └─► Notifica a TvReceiverActivity.kt vía onMessageReceived
        │
        ▼
[TvReceiverActivity.kt]
  │  (Decide el reproductor adecuado mediante TvReceiverHelper.shouldUseWebPlayer)
  │
  ├─► CASO 1: Si es flujo estándar, DASH (.mpd de OK.ru) o MP4 directo:
  │     │
  │     ▼
  │   [ExoPlayerTvManager.kt]
  │     ├─► Configura MediaItem con MimeType (APPLICATION_MPD o APPLICATION_M3U8)
  │     ├─► Aplica filterSafeHeaders(headers) preservando Referer
  │     └─► Transiciona estado a ReceiverUiState.ExoPlaying (Decodificación MediaCodec HW a 60 fps)
  │
  └─► CASO 2: Si es DropCDN (PelisPlus), Fastream o token HMAC protegido:
        │
        ▼
      [TvReceiverHelper.kt]
        ├─► Resuelve baseUrl = headers["Referer"] (ej. https://dr0pstream.com/e/<id>)
        │
        ▼
      [TvWebStreamScreen.kt]
        ├─► Transiciona estado a ReceiverUiState.WebStreamPlaying
        ├─► Inyecta settings.userAgentString = headers["User-Agent"] ?: UA_MOBILE
        ├─► Inicializa WebView con hls.light.min.js y baseUrl canónico
        │
        ▼
      [TvWebResourceInterceptor.kt]
        ├─► Intercepta shouldInterceptRequest de manifiestos y fragmentos .ts
        ├─► Inyecta cabecera HTTP "Access-Control-Allow-Origin: *" (Anti-CORS)
        └─► Descarga los bytes con socket nativo y alimenta a Hls.js sin error 403
```

---

### FLUJO B: Duplicación de Pantalla en Vivo (Screen Mirroring WebRTC)

```text
[1. Usuario pulsa "Pantalla Completa" o "Por Aplicación" en SenderMainScreen.kt]
        │
        ▼
[ProjectionForegroundService.kt]
  │  (Inicia servicio en primer plano y obtiene token MediaProjection)
  └─► Invoca WebRtcSenderManager.startCapture(mediaProjection, isAppMode)
        │
        ▼
[WebRtcSenderManager.kt]
  │  (Crea VirtualDisplay y codificador MediaCodec H.264 por hardware)
  ├─► Inicializa PeerConnection WebRTC local
  ├─► Captura audio interno con AudioPlaybackCaptureConfiguration (Android 10+)
  └─► Genera SDP Offer y lo envía vía SignalingClient.send(SignalingMessage.Offer)
        │
   [RED LOCAL WI-FI / PUERTO :8088]
        │
        ▼
[LocalSignalingServer.kt -> TvReceiverActivity.kt]
  │  (Recibe SDP Offer y transiciona a ReceiverUiState.ScreenMirroring)
  └─► Entrega la oferta a WebRtcReceiverManager.kt
        │
        ▼
[WebRtcReceiverManager.kt]
  │  (Establece RemoteDescription, genera SDP Answer y lo responde al teléfono)
  ├─► Inicializa decodificador por hardware H.264 en SurfaceViewRenderer
  ├─► Aplica escala matemática:
  │     - Si es Vertical en FIT: Escala 1.075f cubriendo el 100% de la altura sin barras
  │     - Si es Horizontal o Por Aplicación: Escala 1.0f libre en 16:9 con soporte de zoom
  └─► Reproduce video con latencia <60ms y audio estéreo digital a 48kHz en DigitalAudioReceiver.kt
        │
        ▼
[TvScreens.kt]
  └─► Renderiza el SurfaceView en pantalla completa en la Android TV
```

---

### FLUJO C: Proyección de Fotos y Videos de la Galería

```text
[1. Usuario selecciona fotos en GalleryPickerActivity.kt]
        │
        ▼
[LocalImageServer.kt] (Emisor Móvil en puerto :8087)
  │  (Servidor HTTP embebido en el teléfono para servir fotos y videos locales)
  └─► Asigna URL local: http://<ip-celular>:8087/image?id=... con soporte HTTP 206
        │
        ▼
[SignalingClient.kt]
  └─► Envía SignalingMessage.ShowImage(imageUrl, rotation, width, height) a la TV Box
        │
        ▼
[TvReceiverActivity.kt -> TvImageScreen.kt]
  │  (Transiciona a ReceiverUiState.PhotoViewing)
  └─► Descarga y renderiza la foto en alta resolución con animaciones de transición suaves
```

---

### FLUJO D: Control Remoto Bidireccional (Pausa, Seek, Volumen, Calidad)

```text
[1. Acción de Control Remoto]
  ├─► Opción A: Botón en el Celular (RemoteControlBar / RemoteExpandedControls)
  │     └─► Envía SignalingMessage.PlayControl("PAUSE" / "SEEK" / "TOGGLE_SUBTITLES")
  │
  └─► Opción B: Tecla en el Mando Físico de la TV Box (D-Pad)
        └─► Capturada por TvRemoteKeyHandler.kt en TvReceiverActivity.kt
              │
              ▼
[TvRemoteKeyHandler.kt / TvReceiverActivity.kt]
  ├─► Si está activo ExoPlayer:
  │     ├─► D-Pad Center: exoplayer.pause() / play()
  │     ├─► D-Pad Left/Right: exoplayer.seekTo(current ± 10000)
  │     └─► D-Pad Abajo: TvTrackManager.cycleQuality()
  │
  └─► Si está activo TvWebStreamScreen (HTML5):
        ├─► D-Pad Center: webView.evaluateJavascript("vid.paused ? vid.play() : vid.pause()")
        ├─► D-Pad Arriba: Alterna subtítulos VTT en hls.subtitleTrack
        └─► D-Pad Abajo: Cicla calidades en hls.currentLevel mostrando badge flotante de 2.5s
```

---

## 5. Reglas Técnicas Inviolables y Zonas de Peligro (Anti-Regresiones)

Para que ningún cambio futuro rompa lo que ya está funcionando, respete estrictamente estas leyes:

1. **Inviolabilidad de la Lista Plana 1:1 (`WvcVideoListSheet.kt`):**
   - NUNCA reintroducir tarjetas agrupadas (`WvcVideoCardItem`) que colapsen variantes o sinteticen resoluciones inexistentes (`720p`). Cada enlace capturado debe mostrarse transparente.
2. **Aislamiento de OK.ru y CDNs (`IMediaStreamExtractorPort`):**
   - La síntesis de múltiples calidades de OK.ru debe vivir EXCLUSIVAMENTE dentro de su adaptador. NUNCA aplicar `.urlset` a DropCDN (`dropcdn.io`) ni alterar los extractores universales.
3. **Preservación de Cabeceras en TV (`TvReceiverHelper.kt`):**
   - `resolveBaseUrl` NUNCA debe sobreescribir el origen si existe un `Referer` legítimo de embed (`https://dr0pstream.com/e/<id>`).
   - `TvWebStreamScreen.kt` debe inyectar `settings.userAgentString = headers["User-Agent"] ?: UA_MOBILE`. Sin esto, DropCDN devuelve `HTTP 403 Forbidden`.
4. **Leyes de Hardware en WebRTC (`WebRtcSenderManager.kt`):**
   - Prohibido llamar a `changeCaptureFormat()` en caliente al rotar la pantalla; destruye el `VirtualDisplay` de Android 14.
   - `IceConnectionState.DISCONNECTED` es temporal; NUNCA debe cerrar la sesión.
   - El comando `STOP` debe obedecerse de inmediato volviendo a Standby en menos de 5ms.
5. **Supresión Nativa de Popups (`BrowserWebView.kt`):**
   - `WebChromeClient.onCreateWindow` DEBE retornar `false` nativo. Prohibido crear WebViews temporales para evaluar popups, ya que agotan los 512 MB de memoria RAM del teléfono provocando cierres inesperados.
