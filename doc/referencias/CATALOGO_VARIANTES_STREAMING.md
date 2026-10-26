# CATÁLOGO TÉCNICO DE VARIANTES DE STREAMING Y PROTECCIÓN CDN

Este documento recopila la base de conocimiento técnico e ingeniería inversa sobre los tipos de flujos de video, sistemas de protección de CDNs y comportamiento de reproductores nativos vs web en OmniCast y Web Video Caster.

---

## 1. LAS 6 VARIANTES DE STREAMING Y COMPORTAMIENTO EN RED

### Variante 1: Tokens Criptográficos HMAC y Parámetros Efímeros
* **Ejemplos:** DropCDN (`ds4.dropcdn.io`), Fastream, LuluStream (`*.tnmr.org`), Akamai EdgeAuth, Cloudflare Stream.
* **Firma en URL:** Parámetros `t=`, `sig=`, `token=`, `s=`, `e=`, `sp=`, `hdnts=`, `__token__=`.
* **Firma en Cabeceras:** `Referer` obligatorio de iframe embed (`/e/`, `dr0pstream.com`, `fastream.to`) y cookies de sesión (`cf_clearance`).
* **Comportamiento Crítico ("Token Quemado"):**
  * Nginx / Cloudflare valida la huella TLS y la sesión.
  * Si un cliente Java nativo (ExoPlayer `DefaultHttpDataSource`) realiza una petición previa o de sondeo con huella TLS de Dalvik/Java, el servidor **consume o invalida el token y responde HTTP 403 Forbidden**.
  * Una vez quemado el token, cualquier reintento posterior (incluso en HTML5) vuelve a fallar con 403.
* **Reproductor Requerido:** **HTML5 nativo (`TvWebStreamScreen` con `Hls.js`)** en primera instancia para preservar la huella de navegador Chromium.

---

### Variante 2: Flujos con Cifrado HLS AES-128
* **Ejemplos:** Portales con repackers HLS, Anime, series protegidas.
* **Firma en Manifiesto:** Directiva `#EXT-X-KEY:METHOD=AES-128,URI="https://..."`.
* **Firma en Cabeceras:** La descarga del archivo binario de la clave (`.key`) suele exigir las mismas cabeceras `Referer` y `Cookie` que el manifiesto maestro.
* **Comportamiento Crítico:** Si el reproductor no transmite las cabeceras en la petición de la llave de 16 bytes, el servidor devuelve 403 y la reproducción se congela por fallo de desencriptación de bloques.
* **Reproductor Requerido:** Si proviene de embed protegido -> **`TvWebStreamScreen`**; si es clave pública sin cookies -> **`ExoPlayer`** con `filterSafeHeaders` preservando `Referer`.

---

### Variante 3: Manifiestos HLS Atípicos o Disfrazados
* **Ejemplos:** PelisGO / Spider-Man (`master.txt`), scripts PHP de redirección.
* **Firma en URL:** Terminación en `.txt`, `.php?url=...` o URLs sin extensión que devuelven MIME `application/vnd.apple.mpegurl`.
* **Comportamiento Crítico:** ExoPlayer falla al intentar inferir el contenedor por la extensión de archivo, a menos que se fuerce `MimeTypes.APPLICATION_M3U8`.
* **Reproductor Requerido:** **`TvWebStreamScreen`** (Hls.js analiza la estructura interna del texto sin importar la extensión) o ExoPlayer con `fallbackMime` explícito.

---

### Variante 4: Transmisiones en Vivo y Canales Continuos
* **Ejemplos:** 808ball (fútbol en vivo), Canela.tv Live, canales IPTV abiertos.
* **Firma en URL:** Manifiestos `.m3u8` sin tokens HMAC efímeros (`hasSignedSecurityToken == false`).
* **Firma en Cabeceras:** `Referer` abierto o nulo, sin cookies de sesión.
* **Comportamiento:** Requiere baja latencia, buffer corto de 2 segundos, soporte de live-edge y decodificación por hardware continuo.
* **Reproductor Requerido:** **`ExoPlayer` nativo (Hardware a 60 fps)**.

---

### Variante 5: Archivos Directos Progresivos con Byte-Range
* **Ejemplos:** Servidores de almacenamiento, videos MP4 directos de ok.ru.
* **Firma en URL:** Terminación en `.mp4`, `.webm`, `.mkv`.
* **Firma en Cabeceras:** Soporte de `Range: bytes=0-` y respuesta `HTTP 206 Partial Content`.
* **Comportamiento:** Permite saltos temporales instantáneos (seek) descargando bloques de bytes bajo demanda sin recargar el archivo completo.
* **Reproductor Requerido:** **`ExoPlayer` nativo**.

---

### Variante 6: Flujos Dinámicos DASH (.mpd) y Plataformas Oficiales
* **Ejemplos:** YouTube (`googlevideo.com`), Dailymotion, servidores DASH con audio y video separados.
* **Firma en URL:** Archivos `.mpd`, IDs de YouTube (`X-YouTube-Id`).
* **Reproductor Requerido:** **`ExoPlayer` (DashMediaSource)** o pantalla oficial dedicada **`TvYouTubeScreen`**.

---

## 2. REGLAS TÉCNICAS INVIOLABLES DE CABECERAS Y RED

1. **Aislamiento Estricto Same-Site de Cookies (Regla Anti-Sopa de Cookies):**
   * Queda prohibido fusionar cookies de múltiples dominios en una sola cabecera `Cookie:`.
   * Servidores Nginx rechazan con `HTTP 403 Forbidden` solicitudes con cabeceras sobredimensionadas (`large_client_header_buffers`) o cookies que no coincidan con su propio dominio. Solo se envían cookies del host directo del stream o de su embed (`isSameSite`).

2. **Prohibición de Cabecera `Origin` en Peticiones de Video CDN:**
   * Enviar `Origin: https://...` a CDNs de video (DropCDN, Fastream) activa validaciones de CORS restrictivas en el servidor Nginx, devolviendo 403 a orígenes no autorizados. La cabecera `Origin` debe removerse en las peticiones de descarga de video.

3. **Desactivación de `withCredentials` en Hls.js:**
   * El estándar W3C prohíbe el uso de `withCredentials = true` si el servidor responde con `Access-Control-Allow-Origin: *`.
   * En `TvWebStreamScreen`, `withCredentials` permanece en `false` para permitir compatibilidad total con `TvWebResourceInterceptor`.

4. **Preservación Inmutable del Referer Embed Canónico:**
   * DropCDN exige estrictamente `Referer: https://dr0pstream.com/e/<id>` con `UA_MOBILE`.
   * Fastream exige `Referer: https://fastream.to/embed-$id.html` con `UA_FASTREAM` (Escritorio).

---

## 3. MECANISMO DE ELECCIÓN DE REPRODUCTOR (CERO QUEMADO DE TOKENS)

La elección del reproductor se determina **analizando las cabeceras y parámetros antes de enviar peticiones a la red**:

```
SI (X-YouTube-Id presente) -> TvYouTubeScreen
SI (hasSignedSecurityToken O Referer es Embed Protegido O tiene Cookies de Sesión) -> TvWebStreamScreen (HTML5 en 0ms)
SINO -> ExoPlayer (Hardware Nativo 60fps)
```

---

## 4. MATRIZ DE REPRODUCTORES Y CASOS PROBADOS EN PRODUCCIÓN (v3.4.94)

| Portal / Servicio | Dominio / CDN | Firma Detectada | Reproductor Asignado en TV Box | Comportamiento Técnico |
|---|---|---|---|---|
| **PelisPlus** | `pelisplus.hair`<br>CDN: `ds4.dropcdn.io` | `hasSignedSecurityToken = true`<br>(Token HMAC efímero) | **`TvWebStreamScreen`**<br>(Chromium Nativo + `Hls.js`) | Carga en Same-Origin (`baseUrl = streamOrigin`). Stack C++ BoringSSL con huella TLS de Google Chrome. Cero bloqueo 403 y cero CORS. |
| **Reypelis** | `reypelis.vip`<br>CDN: `spark.9bg.net` | Flujo HLS VOD directo<br>(Fastream resolutivo) | **`ExoPlayerTvManager`**<br>(AndroidX Media3 HW) | Decodificación por hardware a 1080p nativo, respuesta ultra-rápida al mando D-Pad y salto ±10s. |
| **PelisGO** | `pelisgo.online`<br>CDN: `*.tnmr.org` | Flujo HLS VOD directo<br>(LuluStream resuelto) | **`ExoPlayerTvManager`**<br>(AndroidX Media3 HW) | Perfil resuelto y guardado en `StreamProfileDb`. Decodificación continua por hardware con jitter-buffer anti-tirones. |
| **YouTube** | `youtube.com` | ID de video oficial | **`TvYouTubeScreen`** | Reproducción en visor IFrame oficial de TV sin publicidad invasiva. |
| **Screen Mirroring** | Celular en vivo | Socket WebRTC LAN | **`WebRtcReceiverManager`** | Pipeline H.264 acelerado por hardware (<60ms) con modos Pantalla Completa y Por Aplicación (HU-013). |

