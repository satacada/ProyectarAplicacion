# HU-017: Grabación Multimedia Dual (PVR en Vivo y Descarga VOD), Heartbeat Pasivo y Deep Link QR Universal

**Épica:** Reproducción, Grabación Local y Enlace Móvil-TV  
**Versión de Introducción:** v3.5.55  
**Estado:** Inamovible / Aprobado por el Usuario  

---

## 1. Descripción de la Historia

**Como** usuario de OmniCast en Android TV y móvil,  
**Quiero** grabar transmisiones de televisión en vivo y descargar películas con acceso directo en los controles de pantalla, escanear el código QR del televisor para abrir la app móvil en un solo toque sin pasar por Chrome, y contar con una detección inteligente que apague los controles móviles si la TV se apaga,  
**Para** disfrutar de contenidos grabados offline en la memoria de la TV Box, enlazar mi móvil al televisor sin fricciones y evitar drenaje de batería en mi celular.

---

## 2. Criterios de Aceptación (Contrato Inamovible)

1. **Clasificación Universal de TV Lineal y Live-Edge:**
   - Todo canal continuo o de televisión lineal identificado por descriptores de ruta (`linear`, `lin-`, `live`, `envivo`, `directo`) debe clasificarse como `MediaBranch.LIVE_HLS`.
   - El Sender fuerza incondicionalmente `startPositionMs = 0L`.
   - ExoPlayer ejecuta `seekToDefaultPosition()` para anclarse a la cabeza en vivo (*live edge*), erradicando cualquier ciclo de buffering infinito originado por solicitar fragmentos fuera de la ventana deslizante.
   - En la interfaz de la TV Box, las señales en vivo deben identificarse con la etiqueta brillante `🔴 EN VIVO`, suprimiendo textos obsoletos de tiempo como *"⏱ Quedó en 14:07"*.

2. **Modelo Dual de Grabación Multimedia en TV Box:**
   - **Grabación en Tiempo Real (PVR / 1x):** Para señales en vivo o visualización continua, captura de fragmentos `.ts` en tiempo real volcados directamente a almacenamiento interno sin sobrecarga de CPU.
   - **Descarga Completa en Segundo Plano (VOD):** Para películas y series, descarga acelerada de archivos íntegros mediante `TvDownloadManager` en `Descargas/Pelis_descargas`.
   - **Acceso Directo:** El botón de grabación/descarga debe ser accesible directamente en la barra principal del reproductor sin anidarse dentro del menú de ajustes (⚙️).

3. **Puente HTTP Local para Deep Linking por QR (`:8087/open`):**
   - El código QR desplegado en el televisor codifica una URL HTTP local de la propia TV Box: `http://<IP_TV_BOX>:8087/open?url=...`.
   - Cualquier cámara física de celular (Google Lens, Xiaomi, Samsung, Motorola) reconoce el enlace web y muestra el botón de apertura instantánea.
   - La TV Box entrega una micro-página local con redirección inmediata a `omnicast://cast?url=...`, abriendo la app móvil en menos de medio segundo.
   - Si el dispositivo visitante no posee la app, la página proporciona el botón de descarga del instalador APK.

4. **Heartbeat Pasivo y Autodestrucción de Notificación en Pantalla Apagada (AOD):**
   - El Sender supervisa el flujo constante de telemetría de la TV Box.
   - Si no se recibe telemetría ni respuesta de red durante más de 25 segundos (o 3 minutos en pausa prolongada), el servicio `CastMediaNotificationService` ejecuta `stopForegroundService()`, retirando el icono de la pantalla apagada (AOD) y liberando el estado de casteo del móvil.
