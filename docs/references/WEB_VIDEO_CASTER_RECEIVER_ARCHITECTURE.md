# 📺 ARQUITECTURA TÉCNICA: WEB VIDEO CASTER RECEIVER

Documento de referencia técnica extraído del análisis del código descompilado de **Web Video Caster Receiver (v2.3.0)** (`com.instantbits.cast.receiver`).

---

## 1. COMPONENTES CLAVE Y CICLO DE VIDA

### A. Activities Principales
1. **`com.instantbits.android.receiver.MainActivity`:**
   - Pantalla de bienvenida Leanback / Android TV.
   - Generación y presentación del **Código de Emparejamiento** (`codeLayout`, `code`, `or_enter_code_label`).
   - Muestra la lista de dispositivos emisores descubiertos en la red local (`foundDevicesList`).
2. **`com.instantbits.cast.receiver.connected.ConnectedActivity`:**
   - Actividad de reproducción en pantalla completa (`PlayerView` de `androidx.media3`).
   - Intercepta intents `com.instantbits.cast.receiver.DIAL` y `com.google.android.gms.cast.tv.action.LAUNCH`.
   - Control D-Pad optimizado para mandos de TV Box.

---

## 2. MANEJO DE CONTROL REMOTO D-PAD (`ConnectedActivity.dispatchKeyEvent`)

Web Video Caster Receiver implementa un manejo ergonómico de los eventos del control remoto de la TV Box:
- **FLECHA ARRIBA / ABAJO (`KEYCODE_DPAD_UP` / `DOWN`):**
  - Si los controles están visibles (`playerControlView.isVisible()`): enfoca el botón de control o la barra de tiempo (`TimeBar`). Un segundo toque hacia arriba/abajo oculta los controles inmediatamente (`playerView.hideController()`).
  - Si los controles están ocultos: los hace visibles (`playerView.showController()`) y sitúa el foco sobre la barra de progreso (`TimeBar.requestFocus()`).
- **FLECHA IZQUIERDA (`KEYCODE_DPAD_LEFT`):**
  - Retrocede exactamente **10 segundos** (`player.seekTo(current - 10000)`).
- **FLECHA DERECHA (`KEYCODE_DPAD_RIGHT`):**
  - Avanza exactamente **10 segundos** (`player.seekTo(current + 10000)`).
- **BOTÓN CENTRAL / OK (`KEYCODE_DPAD_CENTER` / `KEYCODE_ENTER`):**
  - Alterna entre Reproducir y Pausar (`player.setPlayWhenReady(!player.isPlaying())`).
  - Si se pausa, muestra los controles y enfoca el botón de Play.
- **BOTÓN RETROCESO (`onBackPressed`):**
  - Primer toque: Si los controles en pantalla están desplegados, los oculta sin detener el video.
  - Segundo toque: Detiene la reproducción, notifica al emisor y regresa a la pantalla principal de espera.

---

## 3. PROTOCOLO JSON-RPC Y COMANDOS DE CONTROL (`nc.e`)

Toda la comunicación de comandos y telemetría entre el Emisor (Móvil) y el Receptor (TV) utiliza un esquema JSON estructurado:

### Comandos Recibidos (`cmd`):
| Comando | Parámetros | Acción en TV |
|---|---|---|
| `loadMedia` | `url`, `mimeType`, `headers`, `position`, `poster`, `title`, `corsOn`, `corsServer` | Carga el flujo multimedia en ExoPlayer con los headers HTTP exactos del sitio web. |
| `play` | - | Reanuda la reproducción (`playWhenReady = true`). |
| `pause` | - | Pausa la reproducción (`playWhenReady = false`). |
| `stop` | - | Detiene el reproductor y regresa a standby. |
| `positionSet` | `position` (long en ms) | Realiza `seekTo(position)`. |
| `positionGet` | - | Solicita posición actual. |
| `volumeSet` | `volume` (float 0.0 - 1.0) | Ajusta el volumen del reproductor o del sistema. |
| `speedSet` | `speed` (double, e.g. 1.0, 1.25, 1.5) | Ajusta la velocidad de reproducción de ExoPlayer (`PlaybackParameters`). |
| `audioTrackSet`| `track` (String ID) | Selecciona la pista de audio solicitada. |
| `textTrackSet` | `track` (String ID) | Activa/desactiva la pista de subtítulos solicitada. |
| `subtitlesSet` | `subtitlesUrl`, `subtitlesFormat` | Inyecta subtítulos externos (SRT/VTT). |
| `zoom` | `scale` (double) | Cambia el escalado de video (Ajustar / Rellenar / Zoom). |
| `disconnect` | `reason` | Cierra la sesión de streaming. |

### Telemetría Enviada al Emisor:
- **`mediaInfo`:** Ancho (`width`), alto (`height`), `mimeType`, `url`, lista de `audioTracks` (con nombre, idioma y track ID), y lista de `textTracks` (subtítulos disponibles).
- **`position`:** Envío periódico de posición actual y duración total para sincronizar la barra de progreso en el móvil.
- **`state`:** Estados de reproducción (`IDLE`, `BUFFERING`, `READY`, `ENDED`).

---

## 4. INGENIERÍA DE EXOPLAYER Y HEADERS HTTP (`mc.p` & `mc.x0`)

Uno de los secretos fundamentales de Web Video Caster para reproducir enlaces que fallan en otros reproductores:
1. **Inyección de Headers HTTP:**
   - Recibe un objeto JSON `headers` (con `Referer`, `Origin`, `User-Agent`, `Cookie`).
   - Pasa estos encabezados a `DefaultHttpDataSource.Factory` para que el servidor de streaming (Akamai, Cloudflare, Fastly) no rechace la solicitud con error `403 Forbidden` o `401 Unauthorized`.
2. **Proxy Local CORS (`corsServer`):**
   - Cuando el servidor de video bloquea peticiones de dispositivos inteligentes o restringe por IP/CORS, el móvil activa un servidor proxy local y la TV se conecta a `http://IP_MOVIL:PUERTO/proxy?...`.
