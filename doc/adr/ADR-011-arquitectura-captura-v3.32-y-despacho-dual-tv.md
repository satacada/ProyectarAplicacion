# ADR-011: Arquitectura Fiel de Captura v3.32.94 y Despacho Dual de Reproductores en TV Box

## Estado
Aprobado (Validado en Producción v3.4.94 para PelisPlus, PelisGO y Reypelis)

## Fecha
2026-10-26

## Contexto
1. **Sensibilidad de CDNs ante Inspección de Tráfico (PelisPlus / DropCDN):**
   Las plataformas de streaming protegidas (DropCDN, Fastream, Cloudflare) rechazan peticiones realizadas por intermediarios Java (`HttpURLConnection`, interceptores intermedios) o con cabeceras `Cookie`/`Origin` inadecuadas, respondiendo con `HTTP 403 Forbidden` y consumiendo ("quemando") el token efímero HMAC de un solo uso.
2. **Estabilidad Comprobada del Motor v3.32.94:**
   La versión v3.32.94 implementó un pipeline de intercepción reactiva en red (`shouldInterceptRequest`), inyección DOM (`wvc_sniffer.js`) y receptor web puro en Chromium con `Hls.js` sin capas intermedias, garantizando máxima compatibilidad sin inventar proxies ni mecanismos artificiales.
3. **Necesidad de Aislamiento Estricto:**
   Los módulos de Screen Mirroring WebRTC (HU-013) y Galería de Fotos/Videos (HU-001) son modernos, estables y deben convivir intactos con la captura fiel de medios web.

## Decisión Técnica

### 1. Pipeline de Captura en el Emisor Móvil (`:app-sender`)
* **Doble Sensor (Red + DOM):**
  1. *Sensor de Red:* `BrowserWebView.shouldInterceptRequest` filtra anuncios mediante `NativeAdBlockEngine` (C++) y detecta manifiestos `.m3u8` y archivos `.mp4` en microsegundos vía `BrowserUrlHelper.isMediaRequest`.
  2. *Sensor DOM:* Scripts `wvc_sniffer.js` y `universal_sniffer.js` interceptan llamadas a `HTMLMediaElement.prototype.play`, etiquetas `<video>` y fuentes dinámicas mediante el canal `@JavascriptInterface` `__wvc_sniffer`.
* **Resolución y Normalización de Cabeceras (`MediaPlatformResolver`):**
  * Para DropCDN (`dr0pstream.com`): Asigna `Referer: https://dr0pstream.com/e/<id>` y `UA_MOBILE`.
  * Para Fastream (`fastream.to`): Asigna `Referer: https://fastream.to/embed-$id.html` y `UA_FASTREAM` (Escritorio).
  * Limpieza absoluta de cookies foráneas y supresión de cabecera `Origin` para evitar bloqueos CORS.
* **Detección de Calidades sin Quemado de Token (`StreamQualityDetector`):**
  * Flujos firmados HMAC generan variantes en memoria (0ms) sin sondas Java previas.
  * Flujos abiertos analizan el master manifiesto con `StreamManifestProbe`.
* **Presentación UI (`WvcVideoListSheet`):**
  * Despliega la hoja modal con diseño clásico de 4 líneas, miniatura de 60dp, duración, calidad y botón de casteo directo a TV.

### 2. Protocolo de Señalización LAN (`:core:protocol`)
* Transmisión directa por WebSocket en el puerto local `:8088` mediante `SignalingMessage.DirectStreamHandoff(url, headers, isLandscape, branch, subtitles, title)`.
* Cero proxies locales (eliminados puertos 8092 y servidores HTTP en el móvil).

### 3. Matriz de Reproductores en la TV Box (`:app-receiver`)
La TV Box despacha en `TvReceiverActivity.kt` hacia 4 motores autónomos especializados:

| Reproductor | Tecnología | Criterio de Selección | Casos Validados en Producción |
|---|---|---|---|
| **`TvWebStreamScreen`** | Chromium Nativo + `Hls.js` | Tokens firmados HMAC (`hasSignedSecurityToken`), CDNs protegidas o fallback de ExoPlayer. | **PelisPlus** (`pelisplus.hair` / DropCDN - *Mad Max*) |
| **`ExoPlayerTvManager`** | AndroidX Media3 (MediaCodec HW) | Flujos directos HLS VOD, MP4 progresivos, canales LIVE e IPTV. | **Reypelis** (`spark.9bg.net`), **PelisGO** (`*.tnmr.org`) |
| **`TvYouTubeScreen`** | WebView IFrame API | Identificador de YouTube extraído (`extractYouTubeId`). | Plataforma oficial YouTube |
| **`WebRtcReceiverManager`** | WebRTC Nativo (libwebrtc) | Screen Mirroring P2P (<60ms) con opciones Pantalla Completa y Por Aplicación. | Proyección en tiempo real (HU-013) |

### 4. Same-Origin Nativo en `TvWebStreamScreen`
* Se fija `baseUrl = streamOrigin` (`https://ds4.dropcdn.io/`), convirtiendo las peticiones XHR de `Hls.js` en peticiones internas Same-Origin.
* Chromium utiliza su stack de red nativo C++ (BoringSSL con huella TLS de Google Chrome), erradicando el rechazo HTTP 403 y los errores de CORS sin interceptores Java.

## Consecuencias
* **Positivas:** Estabilidad absoluta en PelisPlus, PelisGO y Reypelis; cero consumo de batería o ancho de banda por proxies en el móvil; respuesta instantánea al mando a distancia D-Pad.
* **Inviolabilidad:** Queda prohibido reinstaurar interceptores Java en `TvWebStreamScreen` o forzar ExoPlayer en flujos DropCDN con tokens HMAC.
