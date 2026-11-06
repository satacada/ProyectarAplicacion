# 🗺️ MAPA MAESTRO DEL SISTEMA Y GUÍA DE FLUJO DE CÓDIGO (CALL GRAPH)

> **Documento Canónico para Desarrolladores Humanos y Agentes de IA**  
> **Versión del Sistema:** `v3.6.08` | **Estado:** Producción Activa (20 HUs + 19 ADRs Sellados)  
> **Propósito:** Proporcionar una visión integral de 360 grados de todo el código fuente: árbol de directorios, roles de cada módulo, pantallas/páginas, funciones clave y trazabilidad exacta de llamadas archivo por archivo (call graph) para la reproducción de películas, grabación PVR, proyección de pantalla y fotos.

---

## 1. Árbol Estructural de Carpetas y Módulos del Proyecto

El proyecto está organizado como un monorepositorio Android Multi-Módulo bajo **Clean Architecture** y **Arquitectura Hexagonal (Ports & Adapters)**:

```text
proyectarAplicacion/
├── core/                               # NÚCLEO PURO COMPARTIDO (Kotlin JVM 17 Puro)
│   ├── domain/                         # Entidades de dominio, modelos puros y puertos hexagonales
│   │   └── src/main/kotlin/com/omnicast/core/domain/
│   │       ├── config/                 # Configuración Cloud centralizada: SupabaseCloudConfig
│   │       ├── model/                  # MediaStream, StreamProfile, MediaBranch, DeviceTarget, etc.
│   │       ├── port/                   # Interfaces: IMediaStreamExtractorPort, IPlayerTargetDecisionPort, etc.
│   │       └── usecase/                # DetectMediaStreamUseCase, StreamCandidateValidator, HlsMasterResolver, StreamManifestProbe
│   └── protocol/                       # Protocolo de señalización y control local P2P
│       └── src/main/kotlin/com/omnicast/core/protocol/
│           └── SignalingMessage.kt     # Clases selladas JSON: Offer, Answer, DirectStreamHandoff, PlayControl, RecordTelemetry
│
├── app-sender/                         # APLICACIÓN EMISORA PARA TELÉFONO MÓVIL (:app-sender)
│   └── src/main/
│       ├── assets/sniffer/             # Scripts JS inyectados en el navegador
│       │   └── universal_sniffer.js    # Interceptor JS de fetch, XHR y HTMLMediaElement en tiempo real
│       ├── cpp/                        # Motor C++20 nativo Zero-Allocation (JNI)
│       │   ├── stream_classifier.cpp   # Clasificador C++20 de evaluación de URLs en microsegundos
│       │   ├── dean_edwards_unpacker.cpp # Desempaquetador C++20 de scripts ofuscados Dean Edwards (<0.8ms)
│       │   └── ad_pattern_matcher.cpp  # Motor C++20 Aho-Corasick de bloqueo publicitario (<5μs)
│       └── kotlin/com/omnicast/sender/
│           ├── media/                  # Servidor HTTP local (:8087) para fotos y videos locales
│           ├── nativeclassifier/       # Envoltorio JNI para el motor C++20 (NativeStreamClassifier, DeanEdwardsUnpacker, AhoCorasickAdMatcher)
│           ├── network/                # Clientes WebSocket de señalización (SignalingClient, QuickSignalingHelper, SenderLogger)
│           ├── overlay/                # Pantallas flotantes, PhotoCastViewerActivity, GalleryPickerActivity (GalleryPermissionHelper.kt), VideoCastPlayerActivity (VideoCastPlayerControls.kt)
│           ├── playlist/               # Gestor de listas de reproducción y favoritos locales
│           ├── service/                # Foreground Services (ProjectionForegroundService, CastMediaNotificationService, FloatingIndicatorService)
│           ├── stream/                 # ActiveStreamHolder, StreamProfileDatabase.kt (247L), StreamProfileDbSchema.kt (79L) y CloudStreamRuleSyncManager.kt (Supabase)
│           ├── ui/
│           │   ├── browser/            # NAVEGADOR WEB, ADBLOCKER Y DETECCIÓN DE MEDIOS
│           │   │   ├── extractors/     # Adaptadores hexagonales 'Poner y Sacar': OkRu, Fastream, Doodstream, LuluStream, Streamwish, HLS, DASH, MP4, YouTube
│           │   │   ├── AdBlocker.kt & AdBlockDatabase.kt # Interceptor de red HTTP + SQLite + sincronización Supabase
│           │   │   ├── BrowserWebView.kt # Chromium WebView con brazo de red shouldInterceptRequest y supresión de popups
│           │   │   ├── InAppBrowserScreen.kt # Pantalla completa del navegador web (ciclo de captura SELLADO)
│           │   │   ├── MediaPlatformResolver.kt # Orquestador hexagonal y preparador de enlaces en caliente
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
│           ├── directstream/           # REPRODUCTOR HW Y MOTOR PVR DUAL (v3.6.08)
│           │   ├── ExoPlayerTvManager.kt # Reproductor de hardware nativo MediaCodec (MP4, HLS, DASH)
│           │   ├── PvrStreamRecorder.kt # Grabador PVR con Ring Buffer en RAM y bloques de 128 KB
│           │   ├── PvrHlsMasterParser.kt # Resolutor de variantes HLS (.ts) y fMP4 (#EXT-X-MAP:URI="init.mp4")
│           │   ├── TvWebPvrBridge.kt   # Puente Hexagonal PVR para Variante 1 (DropCDN/Fastream vía Chromium BoringSSL)
│           │   └── TvStorageSafetyManager.kt # Guardián anti-brick (reserva sagrada 2.0 GB TV / 500 MB USB y purga 0 bytes)
│           ├── discovery/              # NsdAdvertiserManager (anuncio mDNS _omnicast._tcp)
│           ├── server/                 # LocalSignalingServer (WebSocket :8088) y ReceiverLogger (:8087/logs + Supabase)
│           ├── ui/
│           │   ├── components/         # Pantallas Compose TV y reproductores especializados:
│           │   │   ├── TvStandbyCleanScreen.kt # Pantalla de reposo limpia con IP, QR y estado WiFi
│           │   │   ├── TvDirectStreamScreen.kt # Vista contenedora de ExoPlayer con controles de foco y PVR
│           │   │   ├── TvWebStreamScreen.kt # Reproductor Chromium HTML5 + Hls.js con control D-Pad y TvWebPvrBridge
│           │   │   ├── TvWebResourceInterceptor.kt # Interceptor nativo Anti-CORS (Access-Control-Allow-Origin: *)
│           │   │   ├── TvRecordExitConfirmDialog.kt # Guardián de confirmación al salir durante grabación PVR activa
│           │   │   ├── TvYouTubeScreen.kt # Reproductor oficial de YouTube IFrame inmune a 403 Botguard
│           │   │   ├── TvImageScreen.kt # Visor de fotos de alta resolución proyectadas desde el móvil
│           │   │   └── TvScreens.kt # Lienzo de Screen Mirroring WebRTC SELLADO (Pantalla Completa vs Por App)
│           │   ├── TvReceiverActivity.kt # Actividad principal y máquina de estados de Android TV
│           │   ├── TvReceiverHelper.kt # Enrutador de reproductores, resolución de BaseURL y telemetría HTTP :8087
│           │   └── TvRemoteKeyHandler.kt # Manejo ergonómico del control remoto físico (D-Pad, Zoom, Subs, REC)
│           └── webrtc/                 # Receptor WebRTC por hardware SELLADO (WebRtcReceiverManager)
│
├── .agents/skills/                     # ESCUADRÓN DE 9 AGENTES ESPECIALIZADOS (Pipeline Fast-Track)
├── doc/                                # DOCUMENTACIÓN VIVA DEL PROYECTO
│   ├── adr/                            # Architecture Decision Records (ADR-000 al ADR-018)
│   ├── historias-usuario/              # Contratos de experiencia de usuario (HU-001 al HU-020)
│   ├── referencias/                    # Catálogo técnico de variantes de streaming y WVC
│   ├── ARCHITECTURE.md                 # Arquitectura de alto nivel (C4 Model)
│   └── PROJECT_DASHBOARD.md            # Bitácora viva de versiones y estado del hardware
├── AGENTS.md                           # Leyes inviolables del repositorio y gobernanza multi-agente
├── ROADMAP.md                          # Hoja de ruta maestra unificada (Fases 0 a 4)
└── PROGRESS.md                         # Estado vivo, semáforo de 78 tests y monitor de 300 líneas
```

---

## 2. Los 4 Subsistemas Clave y sus Módulos

### 1. Núcleo Hexagonal (`:core:domain` y `:core:protocol`)
- **Regla Fundamental:** Cero dependencias de Android framework o UI en `:core:domain`.
- **`SupabaseCloudConfig.kt`:** Fuente única de verdad centralizada para la configuración de Supabase Cloud (`BASE_URL`, endpoints REST `omnicast_system_logs` y `omnicast_stream_rules`, y `ANON_KEY`) sin duplicación textual entre emisor y receptor.
- **`DetectMediaStreamUseCase.kt`:** Orquesta la detección. Primero ejecuta el filtro de telemetría e higiene (`isTelemetryOrGarbage`) y luego consulta a los adaptadores registrados en `StreamExtractorRegistry`.
- **`HlsMasterResolver.kt`:** Resolutor heurístico de variantes maestras HLS según RFC 8216. Descalifica playlists de segmentos mediante etiquetas de lista cerrada (`#EXT-X-ENDLIST`, `#EXT-X-MEDIA-SEQUENCE:`) y filtra patrones de pistas desacopladas de audio y subtítulos (`tracks-a`, `audio_`, `_audio`, `subtitles_`, `rendition`).
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
- **Centinela Anti-Honeypot (`CdnHoneypotSentinel.kt`):** Satélite modular desacoplado en TV Box ("Poner y Sacar"). Intercepta y descarta en 0 ms respuestas de CDN que devuelven HTML (páginas de error, dominios en parking o retos de Cloudflare) simulando extensiones `.m3u8`, `.ts`, `.mp4` o `.mpd` mediante inspección de payload en memoria, protegiendo a `Hls.js` y `ExoPlayer` de errores de parseo (`manifestParsingError code=200`).
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
| **Selector y Permisos de Galería** | [GalleryPickerActivity.kt](file:///home/sataca/Descargas/APLICACIONES/proyectarAplicacion/app-sender/src/main/kotlin/com/omnicast/sender/overlay/GalleryPickerActivity.kt) & [GalleryPermissionHelper.kt](file:///home/sataca/Descargas/APLICACIONES/proyectarAplicacion/app-sender/src/main/kotlin/com/omnicast/sender/overlay/GalleryPermissionHelper.kt) | `:app-sender` | Explorador y selector de fotos/videos locales con resolución desacoplada de permisos de almacenamiento/cámara y URIs. |
| **Reproductor Local y Controles de Video** | [VideoCastPlayerActivity.kt](file:///home/sataca/Descargas/APLICACIONES/proyectarAplicacion/app-sender/src/main/kotlin/com/omnicast/sender/overlay/VideoCastPlayerActivity.kt) & [VideoCastPlayerControls.kt](file:///home/sataca/Descargas/APLICACIONES/proyectarAplicacion/app-sender/src/main/kotlin/com/omnicast/sender/overlay/VideoCastPlayerControls.kt) | `:app-sender` | Reproductor local sincronizado con controles táctiles delegados (Play/Pausa, Seekbar, saltos ±10s) al transmitir video local a la TV. |
| **Pantalla de Reposo TV** | [TvStandbyCleanScreen.kt](file:///home/sataca/Descargas/APLICACIONES/proyectarAplicacion/app-receiver/src/main/kotlin/com/omnicast/receiver/ui/components/TvStandbyCleanScreen.kt) | `:app-receiver` | Pantalla limpia de espera en la TV Box mostrando la dirección IP de conexión (`192.168.0.x`), código PIN y estado del WiFi. |
| **Reproductor Web TV (HTML5)** | [TvWebStreamScreen.kt](file:///home/sataca/Descargas/APLICACIONES/proyectarAplicacion/app-receiver/src/main/kotlin/com/omnicast/receiver/ui/components/TvWebStreamScreen.kt) | `:app-receiver` | WebView en Android TV con Hls.js oficial, `baseUrl` fijado al Referer canónico y User-Agent móvil para CDNs con tokens estrictos. |
| **Interceptor Anti-CORS TV** | [TvWebResourceInterceptor.kt](file:///home/sataca/Descargas/APLICACIONES/proyectarAplicacion/app-receiver/src/main/kotlin/com/omnicast/receiver/ui/components/TvWebResourceInterceptor.kt) | `:app-receiver` | Intercepta descargas de `.m3u8` y `.ts` en la TV, inyectando `Access-Control-Allow-Origin: *` y respondiendo preflights OPTIONS en 0ms. |
| **Reproductor Hardware TV** | [TvDirectStreamScreen.kt](file:///home/sataca/Descargas/APLICACIONES/proyectarAplicacion/app-receiver/src/main/kotlin/com/omnicast/receiver/ui/components/TvDirectStreamScreen.kt) | `:app-receiver` | Lienzo de Compose que aloja `ExoPlayerTvManager` para decodificar MP4 y DASH a 60 fps sin carga de CPU. |
| **Lienzo Screen Mirroring TV** | [TvScreens.kt](file:///home/sataca/Descargas/APLICACIONES/proyectarAplicacion/app-receiver/src/main/kotlin/com/omnicast/receiver/ui/components/TvScreens.kt) | `:app-receiver` | Superficie `SurfaceViewRenderer` WebRTC para duplicar pantalla del teléfono con modos Pantalla Completa y Por Aplicación. |

---

## 4. Trazabilidad de Código Paso a Paso (Call Graph de Archivo a Archivo)

#### FLUJO A: Reproducción de una Película Web (Web Video Caster - Arquitectura en Tenaza Dual)
Desde que el usuario navega a un portal (ej. `cinecalidad.men`, `pelisplus.hair` o `ok.ru`) hasta que la película se decodifica en la TV Box:

```text
[1. Usuario navega o pulsa Play en la película]
        │
        ├──► BRAZO 1: INTERCEPCIÓN EN RED NATIVA (Iframes Cerrados / CDNs Ofuscadas)
        │    [BrowserWebView.kt -> shouldInterceptRequest]
        │      ├─► Filtro 1: if (r.isForMainFrame) -> Deja pasar la página HTML base
        │      ├─► Filtro 2: AdBlocker / NativeAdBlockEngine C++20 -> Descarta anuncios (0ms)
        │      ├─► Filtro 3: NativeStreamClassifier.shouldDiscard() -> Descarta telemetría y assets
        │      └─► Construye StreamCandidate(url, pageUrl=Referer, headers=r.requestHeaders)
        │            │
        │            ▼
        └──► BRAZO 2: INSPECCIÓN EN DOM JAVASCRIPT (Metadatos Visuales / Anti-Prerolls / SPAs)
             [universal_sniffer.js]
               ├─► monitorVideo(): Evalúa video.duration (<60s es comercial y se descarta)
               ├─► sniffPlayerApis(): Extrae calidades 1080p/720p de JWPlayer/Clappr
               └─► reportMedia(): Despacha OMNICAST_STREAM: JSON hacia BrowserWebView.kt
                     │
        ┌────────────┴────────────────────────┐
        ▼                                     ▼
[DetectMediaStreamUseCase.kt] (Núcleo Hexagonal :core:domain)
  │  (Orquestación limpia sin dependencias Android)
  ├─► Paso 1: TelemetryFilterAdapter.isTelemetryOrGarbage(url)
  └─► Paso 2: Evalúa extractores registrados ordenados por prioridad:
              for (extractor in sortedExtractors) {
                  if (extractor.canHandle(candidate)) -> return extractor.extract(candidate)
              }
        │
        ├─► Delegación obligatoria en [StreamCandidateValidator.kt]:
        │     ├─► isWebDocumentOrPlayerRoute(): Descarta .html, .js, .css, /embed/, /e/, /cdn-cgi/
        │     └─► isBinaryMediaStream(): Certifica .m3u8, .mp4, .mpd, /hls/, /pass/,
        │                                y patrón estructural: (~ + token= + expiry=)
        │
        ▼
[Adaptadores Modulares Especializados ('Poner y Sacar')]
  ├─► UniversalMp4ExtractorAdapter: Extrae MP4, WEBM, MKV
  ├─► UniversalHlsExtractorAdapter: Extrae listas HLS .m3u8 y chunklists
  ├─► UniversalDashExtractorAdapter: Extrae manifiestos .mpd
  ├─► DoodstreamExtractorAdapter: Reconoce Doodstream / PlayMogo / CloudataCDN
  │                               e inyecta Referer legítimo (https://playmogo.com/)
  ├─► LuluStreamExtractorAdapter: Reconoce CDN LuluStream / TNMR (*.tnmr.org, luluvdo)
  │                               e inyecta Referer legítimo canónico
  ├─► StreamwishExtractorAdapter: Reconoce CDN Streamwish / Vidhide / Wish
  │                               e inyecta User-Agent Desktop y cabeceras de origen
  └─► FastreamExtractorAdapter: Inyecta User-Agent Desktop (UA_FASTREAM)
        │
        ▼
[InAppBrowserScreen.kt / WvcVideoListSheet.kt]
  │  (Muestra el botón flotante con contador de videos >= 1)
  ├─► Abre automáticamente el selector de casteo
  └─► El usuario pulsa Transmitir -> MediaPlatformResolver.prepareStreamForPlayback()
        │
        ▼
[StreamProfileDatabase.kt (SQLite) & CloudStreamRuleSyncManager.kt (Supabase)]
  ├─► SQLite Local: Guarda host + Referer + TargetPlayer (0ms)
  └─► Nube: Reporta regla validada a Supabase (omnicast_stream_rules) tras 2 min continuos
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
[1. Usuario selecciona fotos en GalleryPickerActivity.kt (gestión de permisos vía GalleryPermissionHelper.kt)]
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
6. **Preservación Fiel del Host Público de CDN (Anti-Private RFC 1918 Leak / v3.5.58):**
   - Queda terminantemente PROHIBIDO mutar o sustituir el host público de CDNs (`r*.dropcdn.io`, `ds*.dropcdn.io`) mediante parámetros query (`srv=`).
   - `BrowserUrlHelper.sanitizeMediaUrl` y `StreamManifestProbe.sanitizeMediaUrl` se limitan a `url.trim()`. Sustituir el host por `srv=ds1i` desvía la conexión a una IP privada interna del datacenter (`192.168.10.23:443`), provocando fallo de conexión `code=0` en la TV Box.
7. **TV Box 100% Pasiva y Agnóstica (`ExoPlayerTvManager.kt`):**
   - Prohibido llamar a `sanitizeMediaUrl` o reescribir URLs en la TV Box. La TV Box reproduce de forma pasiva lo que despacha el Emisor con aceleración por hardware o mediante `TvWebStreamScreen`.
8. **Gobernanza Dinámica por SQLite y Resiliencia Offline (`StreamProfileDatabase.kt`, `CloudStreamRuleSyncManager.kt`):**
   - Las reglas de identidad (`User-Agent`) y asignación de reproductor (`X-Target-Player: CHROMIUM_ANTI_CORS`) se leen de la tabla `stream_profiles` de la base de datos local SQLite.
   - `CloudStreamRuleSyncManager` sincroniza con Supabase (`omnicast_stream_rules`) descargando el JSON íntegro de `headers`, manteniendo resiliencia total offline si no hay internet o cae la nube.
