# 📺 ARQUITECTURA TÉCNICA: AIRSCREEN (RECEIVER DE ESPEJO Y CASTEO)

Documento de referencia técnica extraído del análisis de ingeniería inversa de **AirScreen (v2.8.1)** (`com.ionitech.airscreen`).

---

## 1. COMPONENTES CLAVE Y SERVICIOS NATIVOS

### A. Núcleo de Espejo de Pantalla (Screen Mirroring)
1. **`com.ionitech.airscreen.ui.activity.ScreenMirrorActivity`:**
   - Actividad dedicada de renderizado de baja latencia para el flujo de pantalla en vivo.
   - Renderiza directamente sobre `SurfaceView` / `TextureView` nativo acelerado por hardware sin capas intermedias de Compose pesadas.
2. **`com.ionitech.airscreen.service.NativeService`:**
   - Servicio nativo en segundo plano que inicializa y coordina los servidores de escucha locales.
   - Utiliza `libmdns.so` para emitir registros mDNS locales de descubrimiento para:
     - AirPlay (`_airplay._tcp`, `_raop._tcp`)
     - Google Cast / CastV2 (`_googlecast._tcp`)
     - DLNA / UPnP (`_upnp._tcp`)
     - Miracast / Wi-Fi Display
3. **`com.ionitech.airscreen.function.share.screen.media.VideoEncoder` & `CaptureScreenService`:**
   - Pipeline de codificación y decodificación por hardware vía `MediaCodec`.

---

## 2. NUCLEO DE DECODIFICACIÓN NATIVA Y MULTIMEDIA (`i9.b` & `i9.e`)

AirScreen implementa un pipeline de decodificación por hardware de latencia ultracorta:

### A. Loop de Entrada (`VideoDataInputThread` en `i9.b`):
- `mediaCodec.dequeueInputBuffer(10000L)`: Timeout ajustado a 10 ms para alimentar el buffer del codec sin trabar el hilo.
- `inputBuffer.clear()`: Reutilización de buffers de entrada directos.
- `mediaCodec.queueInputBuffer(iDequeueInputBuffer, 0, length, presentationTimeUs, 0)`: Envío directo del NAL unit H.264.

### B. Loop de Salida y Renderizado Directo a Surface (`VideoDataOutputThread` en `i9.b`):
- `int outIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, 16000L)`: Timeout de 16 ms (exactamente 1 fotograma a 60 fps).
- **Renderizado Cero-Copia (Zero-Copy):**
  - Invoca `mediaCodec.releaseOutputBuffer(outIndex, render = true)`.
  - El buffer de video decodificado se vuelca directamente sobre la `Surface` nativa sin pasar por memoria de CPU, garantizando 60 fps fluidos sin consumo térmico.
- **Detección Dinámica de SPS/PPS:**
  - Al recibir `MediaCodec.INFO_OUTPUT_FORMAT_CHANGED`, extrae las dimensiones exactas (`crop-right - crop-left + 1`, `crop-bottom - crop-top + 1`) y calcula la matriz de transformación gráfica en `ScreenMirrorActivity`.

### C. Descarte de Frames y Mitigación de Congelamiento:
- Si la cola de entrada se atrasa respecto al tiempo real, `render` se establece en `false` en `releaseOutputBuffer` para soltar el frame viejo y avanzar de inmediato al frame más reciente (Drop Late Frames).

---

## 3. GESTIÓN DE ESCALADO Y ORIENTACIÓN (`ScreenMirrorActivity`)

- Permite alternancia mediante configuración entre:
  - `setting_screen_resolution`: Ajuste de resolución entre 720p, 1080p y 4K según la capacidad del decodificador por hardware del chip de la TV (Amlogic, Allwinner, Rockchip).
  - `setting_full_screen`: Alternancia entre `ASPECT_FIT` (bandas negras laterales para fotos verticales de móvil, evitando recorte) y `ASPECT_FILL` (pantalla completa para juegos y videos horizontales).

