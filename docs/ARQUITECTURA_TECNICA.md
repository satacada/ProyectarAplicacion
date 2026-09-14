# ⚙️ ARQUITECTURA TÉCNICA DETALLADA: OMNICAST PRO

Este documento define la especificación técnica profunda de hardware, red, codecs y contratos de código para el sistema **OmniCast Pro**.

---

## 1. PIPELINE DE CAPTURA Y CODIFICACIÓN EN ANDROID (EMISOR MÓVIL)

### 1.1 Captura a Nivel de Sistema Operativo con MediaProjection
A diferencia de las aplicaciones tradicionales que dependen de APIs web o de reproductores incrustados, OmniCast Pro captura el búfer de la GPU de Android mediante `MediaProjection`. Esto garantiza que **cualquier aplicación** que se esté ejecutando en pantalla (juegos, redes sociales, navegadores, documentos o reproductores de video locales) sea capturada con total fidelidad.

```kotlin
// Contrato del motor de captura de pantalla
interface IScreenCaptureEngine {
    fun initialize(resultCode: Int, resultData: Intent)
    fun createVirtualDisplay(width: Int, height: Int, dpi: Int, surface: Surface): VirtualDisplay
    fun stopCapture()
}
```

#### Flujo de Inyección Directa (Zero-Copy Surface Pipeline):
1. Se inicializa el codificador de hardware `MediaCodec` configurado en modo `createInputSurface()`.
2. Se genera un `VirtualDisplay` pasando como objetivo directo la `InputSurface` del codec.
3. El subsistema gráfico de Android (`SurfaceFlinger`) vuelca directamente los frames renderizados por la GPU en la superficie de entrada del chip codificador H.264/HEVC.
4. **Resultado:** Cero copias en memoria RAM de la CPU, latencia de renderizado inferior a 8 milisegundos y consumo mínimo de batería en el teléfono.

---

### 1.2 Captura de Audio Digital Interno con AudioPlaybackCapture
Para evitar capturar el sonido ambiente por el micrófono del teléfono, se utiliza la API `AudioPlaybackCaptureConfiguration` (disponible desde Android 10+):

```kotlin
// Configuración de captura de audio interno digital (sin micrófono)
val audioCaptureConfig = AudioPlaybackCaptureConfiguration.Builder(mediaProjection)
    .addMatchingUsage(AudioAttributes.USAGE_MEDIA)
    .addMatchingUsage(AudioAttributes.USAGE_GAME)
    .addMatchingUsage(AudioAttributes.USAGE_UNKNOWN)
    .build()

val audioRecord = AudioRecord.Builder()
    .setAudioFormat(
        AudioFormat.Builder()
            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
            .setSampleRate(48000)
            .setChannelMask(AudioFormat.CHANNEL_IN_STEREO)
            .build()
    )
    .setBufferSizeInBytes(minBufferSize)
    .setAudioPlaybackCaptureConfig(audioCaptureConfig)
    .build()
```

- **Calidad de audio:** PCM 16-bit estéreo a 48 kHz.
- **Compresión:** Codificación ultrarrápida a **Opus 128 kbps** (latencia algorítmica de 10-20 ms).

---

## 2. PIPELINE DE CODIFICACIÓN Y TRANSPORTE WEBRTC P2P

### 2.1 Configuración de Baja Latencia en MediaCodec
Para conseguir una transmisión en tiempo real idéntica a una pantalla local, el codificador H.264 se inicializa con parámetros de baja latencia:

```kotlin
val mediaFormat = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, width, height).apply {
    setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
    setInteger(MediaFormat.KEY_BIT_RATE, targetBitrate) // 8 Mbps a 15 Mbps
    setInteger(MediaFormat.KEY_FRAME_RATE, 60)
    setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1) // Keyframe cada 1 segundo
    // Parámetros críticos para Ultra-Baja Latencia:
    setInteger(MediaFormat.KEY_LATENCY, 0)
    setInteger(MediaFormat.KEY_PRIORITY, 0) // Máxima prioridad de procesamiento
    setInteger(MediaFormat.KEY_PROFILE, MediaCodecInfo.CodecProfileLevel.AVCProfileHigh)
    setInteger(MediaFormat.KEY_LEVEL, MediaCodecInfo.CodecProfileLevel.AVCLevel42)
    setInteger("vendor.qcom-ext-enc-low-latency.enable", 1) // Aceleración Qualcomm Snapdragon
}
```

### 2.2 Señalización Local Cero-Internet (mDNS + WebSocket Local)

```
        ┌─────────────────────────────────────────────────────────┐
        │                 RED WI-FI LOCAL (ROUTER)                │
        └─────────────────────────────────────────────────────────┘
                     ▲                               ▲
      Descubrimiento │ mDNS           Señalización   │ WebSocket
      _omnicast._tcp │ local          P2P Handshake  │ ws://192.168.1.45:8088
                     │                               │
        ┌────────────┴────────────┐     ┌────────────┴────────────┐
        │   📱 CELULAR EMISOR    │     │   📺 TV BOX RECEPTOR    │
        │   • NsdManager Scanner  │     │   • NsdManager Service  │
        │   • WebRTC Client       │     │   • Ktor Local WS Server│
        │   • MediaProjection HW  │     │   • Hardware Decoder    │
        └─────────────────────────┘     └─────────────────────────┘
                     │                               ▲
                     └────── CANAL P2P DIRECTO ──────┘
                        RTP / SRTP (UDP Local)
                        Latencia: 35ms - 65ms
```

1. **Sin Servidores en Internet:** No se requiere ningún servidor público STUN/TURN en la nube. La señalización ocurre íntegramente a través de la IP local de la TV Box.
2. **Descubrimiento Transparente:** La TV Box anuncia su presencia mediante mDNS. El teléfono escanea la red local y detecta automáticamente dispositivos como "TV Box Sala" o "Mi Box Recámara".
3. **Resiliencia ante Interrupciones:** Si la red Wi-Fi experimenta fluctuaciones, el protocolo WebRTC responde con paquetes RTCP NACK/PLI para solicitar inmediatamente un nuevo cuadro clave I-Frame sin congelar la transmisión.

---

## 3. ARQUITECTURA DEL RECEPTOR EN LA TV BOX

### 3.1 Interfaz Leanback y Navegación por Control Remoto
La aplicación de la TV Box está diseñada con componentes nativos de Android TV (Jetpack Compose for TV / Leanback):
- Foco automático en los botones mediante navegación D-Pad (flechas arriba/abajo/izquierda/derecha y botón central OK).
- Estado de espera con pantalla ambiental oscura, indicando:
  * Estado: `● Listo para proyectar`
  * Nombre del receptor: `OmniCast - Sala Principal`
  * Dirección IP local: `192.168.1.45`
  * Código PIN de emparejamiento (opcional, para evitar intrusiones no deseadas).

### 3.2 Decodificación Directa en SurfaceView por Hardware
Cuando comienza la transmisión, la UI pasa a pantalla completa y delega el renderizado a una `SurfaceView` vinculada al decodificador `MediaCodec`:
- El stream de paquetes NALU H.264 entra directamente en los búferes de entrada del chip gráfico del TV Box (GPU/VPU).
- El hardware dibuja cada cuadro en pantalla en menos de 5 milisegundos.
- **Ventaja en TV Boxes Chinos (Amlogic/Rockchip):** La CPU no interviene en el dibujo de píxeles, eliminando el sobrecalentamiento y evitando que la TV Box se reinicie o ralentice tras horas de uso.

---

## 4. MATRIZ DE CONFIGURACIÓN Y PERFILES DE CALIDAD

| Perfil | Resolución | FPS | Bitrate | Latencia Estimada | Caso de Uso Óptimo |
|---|---|---|---|---|---|
| **Modo Gamer (Ultra-Fluido)** | 1280x720 (720p) | 60 fps | 5 - 8 Mbps | **35 - 50 ms** | Videojuegos móviles, emuladores, interacción táctil rápida. |
| **Modo Balanceado (Estándar)** | 1920x1080 (1080p) | 60 fps | 10 - 14 Mbps | **50 - 70 ms** | Navegación web, redes sociales (TikTok/IG), documentos, Zoom. |
| **Modo Cine (Alta Fidelidad)** | 1920x1080 / 4K | 30/60 fps | 16 - 22 Mbps | **70 - 95 ms** | Películas, series, videos de YouTube y reproductores locales. |
