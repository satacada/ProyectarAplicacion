# 📚 REFERENCIA ARQUITECTURAL PERMANENTE: OMNICAST PRO (v3.4, AirScreen & WVC)
> **Propósito:** Registro estático de arquitectura e ingeniería inversa para evitar decompilaciones repetitivas y ahorrar tokens en sesiones futuras.

---

## 1. ESPECIFICACIÓN DE SCREEN MIRRORING (WEBRTC P2P)

### Móvil Emisor (app-sender)
- **Codificador:** DefaultVideoEncoderFactory(eglBaseContext, true, true) con perfil H.264 High Profile.
- **Resolución:** Escalado automático a 1920x1080 o ratio de pantalla nativo (múltiplos de 16 requeridos por hardware encoders Qualcomm/MediaTek).
- **Tasa de Refresco & Bitrate:** 30 FPS fijos, 3.5 Mbps (3500 kbps) target, keyframe interval cada 1 segundo.
- **Audio Captura:**
  - Android 10+ (API 29+): AudioPlaybackCaptureConfiguration con AudioRecord en PCM 16-bit estéreo 48 kHz.
  - Android 8/9 (API 26-28): Fallback silencioso por micrófono o mute sin abortar streaming.
- **Canal de Señalización:** WebSocket en puerto 8088 del Receiver. Intercambio directo de Offer y Answer SDP más IceCandidate.

### TV Box Receptor (app-receiver)
- **Decodificador:** DefaultVideoDecoderFactory(eglBaseContext) con aceleración HW.
- **Renderizado de Video:** SurfaceViewRenderer.
  - **CRÍTICO:** surfaceViewRenderer.setZOrderOnTop(true) es OBLIGATORIO en Android TV Box para evitar que el layer de SurfaceView quede oculto detrás de la capa de fondo de Jetpack Compose (MaterialTheme background).
- **Relación de Aspecto (Scaling):**
  - Alternancia entre SCALE_ASPECT_FIT (bandas negras laterales) y SCALE_ASPECT_FILL (recorte 16:9 completo) mediante DPAD Center / Enter del control remoto.

---

## 2. REPRODUCTOR DE YOUTUBE EN TV BOX (SIN ERROR 152-4)

### Causa Raíz del Error 152-4
- El error 152-4 de YouTube ocurre cuando un WebView intenta interceptar o alterar scripts de YouTube (MediaSource, HTMLMediaElement.prototype.play), o cuando se utiliza un User-Agent móvil obsoleto o URLs con restricciones de dominio embebido.

### Solución Verificada y Definitiva
- **Base URL:** https://www.youtube-nocookie.com
- **Embed URL:** https://www.youtube-nocookie.com/embed/{VIDEO_ID}?autoplay=1&controls=1&fs=1&rel=0&iv_load_policy=3&modestbranding=1&cc_load_policy=0&enablejsapi=1
- **User-Agent:** Desktop Chrome moderno: Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36
- **Control Remoto PostMessage:**
  - Play: iframe.contentWindow.postMessage('{"event":"command","func":"playVideo","args":""}', '*')
  - Pausa: iframe.contentWindow.postMessage('{"event":"command","func":"pauseVideo","args":""}', '*')
  - Seek: iframe.contentWindow.postMessage('{"event":"command","func":"seekTo","args":[seconds, true]}', '*')
  - Volumen: iframe.contentWindow.postMessage('{"event":"command","func":"setVolume","args":[percent]}', '*')
- **Reporte de Tiempo Bidireccional:**
  - Envío periódico de {"event":"listening"} al iframe.
  - Recepción de infoDelivery en window.addEventListener('message', ...) -> invocación a AndroidBridge.onPlaybackStatus(cur, dur, state) -> emisión de SignalingMessage.PlaybackStatus al emisor móvil.

---

## 3. INTEGRACIÓN DEL VOLUMEN DEL SISTEMA EN TV BOX

- ExoPlayerTvManager.setVolume(vol) sólo controla la ganancia interna de ExoPlayer, no tiene efecto en YouTube (WebView) ni en el volumen del televisor.
- **Solución Nativa:** Mapear SignalingMessage.PlayControl("VOLUME", volume) directamente al AudioManager del televisor con AudioManager.FLAG_SHOW_UI:
  - Al pasar FLAG_SHOW_UI, el televisor/TV Box dibuja su barra nativa de volumen en pantalla y el audio de YouTube y ExoPlayer sube o baja de forma real.

---

## 4. TRANSMISIÓN DE FOTOS DE GALERÍA

### Arquitectura Probada
- En lugar de levantar un servidor HTTP móvil local en puerto 8090 (vulnerable a Client Isolation en routers Wi-Fi o firewalls locales de Android TV), las fotos se envían directamente por el WebSocket existente (SignalingMessage.ShowImage):
  - Imagen redimensionada a 1080p JPEG (data:image/jpeg;base64,...).
  - La TV Box decodifica el stream Base64 con BitmapFactory.decodeByteArray de forma 100% instantánea y local.
  - Transformaciones de zoom y paneo se transmiten fluidamente vía SignalingMessage.ImageTransform(scale, panX, panY).

---

## 5. NAVEGADOR WEB Y EXTRACCIÓN DE STREAMS (PELISMART, OK.RU, ETC.)

### Aislamiento de Popups
- Sitios como Pelismart o reproductores con publicidad abren ventanas secundarias mediante window.open o redirecciones.
- El navegador intercepta popups en un popupWebView secundario y no permite que la pestaña principal sea secuestrada.

### Filtrado de Anuncios de Video (Pelismart)
- Pelismart carga videos cortos de publicidad (.mp4) de 5 a 10 segundos antes de comenzar la película.
- Filtro estricto en BrowserUrlHelper.isCommercialAdUrl: ignora patrones como preroll, midroll, commercial, /vast, /ad/, trailer.mp4.
- Los streams .m3u8 (HLS Adaptativo) siempre tienen prioridad primaria y sustituyen inmediatamente cualquier .mp4 publicitario interceptado.

### Limpieza de Memoria al Navegar
- ActiveStreamHolder.clear() se debe ejecutar siempre que el usuario navega a una nueva URL o cambia de dominio, evitando que URLs residuales (como videos previos de YouTube o comerciales) queden retenidas en la barra de control inferior.

---

## 6. MENÚ FLOTANTE Y ASISTENTE

- ACTION_DOWN = true en el TouchListener del botón flotante para evitar eventos fantasmas.
- El asistente ofrece:
  - "📺 Cambiar Modo TV"
  - "⏹ Detener Transmisión"
  - "❌ Desactivar Asistente" (cierre total del FloatingIndicatorService sin congelar la app).
