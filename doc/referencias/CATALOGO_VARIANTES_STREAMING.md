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

### Variante 4: Transmisiones en Vivo y Canales Continuos (Linear TV)
* **Ejemplos:** Sky News (`linear901-.../prod-gb-lin-...`), Das Erste, Latina TV, 808ball (fútbol en vivo), Canela.tv Live, canales IPTV abiertos.
* **Firma en URL:** Descriptores de transmisión continua: `linear`, `lin-`, `live`, `envivo`, `directo`, `stream`, `chunklist_live` y manifiestos `.m3u8` sin directiva `#EXT-X-ENDLIST`.
* **Firma en Cabeceras:** `Referer` abierto o nulo, sin cookies de sesión restrictivas.
* **Comportamiento Crítico (Ventana Deslizante / Sliding Window):**
  * La CDN solo mantiene en caché los últimos 20 a 30 segundos de emisión.
  * **Prohibición Estricta de Reanudación / Seek:** Queda terminantemente prohibido solicitar posiciones de tiempo del pasado (`savedPos > 0`). Si el reproductor ejecuta un seek fuera de la ventana deslizante, la CDN devuelve 404/403 en los fragmentos antiguos y el reproductor entra en buffering perpetuo.
  * La inicialización debe forzar obligatoriamente `startPositionMs = 0L` y ejecutar `seekToDefaultPosition()` para engancharse a la cabeza en vivo (*live edge*).
* **Reproductor Requerido:** **`ExoPlayer` nativo (Hardware a 60 fps)** con `setLiveConfiguration` activo para mantener sincronismo de baja latencia.

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

4. **Preservación Inmutable del Referer Embed Canónico e Identidad:**
   * DropCDN / Dr0pstream exige `Referer: https://dr0pstream.com/e/<id>` sin cabecera `Origin` y con User-Agent de escritorio (`UA_FASTREAM`) o móvil coherente con la sesión.
   * Fastream exige `Referer: https://fastream.to/embed-$id.html` con `Origin: https://fastream.to` y `UA_FASTREAM` (Escritorio Windows).

5. **Ley de Preservación Fiel del Host Público CDN (Anti-Private RFC 1918 Leak / v3.5.58):**
   * Queda terminantemente PROHIBIDO reescribir el nombre de host de CDNs (`r*.dropcdn.io`, `ds*.dropcdn.io`) mediante parámetros query como `srv=`.
   * En CDNs como DropCDN, los balanceadores públicos (`r1.dropcdn.io` con IP pública `91.218.50.202`) entregan parámetros de telemetría de backend como `srv=ds1i`. Los nodos terminados en `i` (`ds1i.dropcdn.io`) resuelven a **direcciones IP privadas RFC 1918 del datacenter (`192.168.10.23:443`)**, provocando fallo instantáneo de conexión `code=0` si se sustituye el host público.
   * Toda petición debe dirigirse incondicionalmente al dominio público asignado originalmente por el servidor de streaming.

---

## 3. MECANISMO DE ELECCIÓN DE REPRODUCTOR (CERO QUEMADO DE TOKENS)

La elección del reproductor se determina **analizando las cabeceras y parámetros antes de enviar peticiones a la red**:

```
SI (X-YouTube-Id presente) -> TvYouTubeScreen
SI (hasSignedSecurityToken O Referer es Embed Protegido O tiene Cookies de Sesión) -> TvWebStreamScreen (HTML5 en 0ms)
SINO -> ExoPlayer (Hardware Nativo 60fps)
```

---

## 4. MATRIZ DE REPRODUCTORES Y CASOS PROBADOS EN PRODUCCIÓN (v3.5.21)

| Portal / Servicio | Dominio / CDN | Firma Detectada | Reproductor Asignado en TV Box | Comportamiento Técnico |
|---|---|---|---|---|
| **PelisPlus** | `pelisplus.hair`<br>CDN: `ds4.dropcdn.io` | `hasSignedSecurityToken = true`<br>(Token HMAC efímero) | **`TvWebStreamScreen`**<br>(Chromium Nativo + `Hls.js`) | Carga con `baseUrl = headers["Referer"]` (`https://dr0pstream.com/e/<id>`) y User-Agent móvil. Interceptor Anti-CORS activo. Cero bloqueo 403 y resolución nativa (sin reescritura `.urlset`). |
| **OK.ru (DASH)** | `ok.ru`<br>CDN: `*.mycdn.me`, `*.okcdn.ru` | Manifiesto `.mpd`<br>(DASH dinámico) | **`ExoPlayerTvManager`**<br>(DashMediaSource HW) | Decodificación por hardware nativo para pistas VP9 y Opus. |
| **OK.ru (Progresivo)** | `ok.ru`<br>CDN: `*.mycdn.me` | Streams MP4/HLS con `Referer: https://ok.ru/` | **`ExoPlayerTvManager`**<br>(AndroidX Media3 HW) | Selección de calidades 1080p, 720p, 480p, 360p en lista plana, transmitiendo cabecera Referer oficial. |
| **Reypelis** | `reypelis.vip`<br>CDN: `spark.9bg.net` | Flujo HLS VOD directo<br>(Fastream resolutivo) | **`ExoPlayerTvManager`**<br>(AndroidX Media3 HW) | Decodificación por hardware a 1080p nativo, respuesta ultra-rápida al mando D-Pad y salto ±10s. |
| **PelisGO** | `pelisgo.online`<br>CDN: `*.tnmr.org` | Flujo HLS VOD directo<br>(LuluStream resuelto) | **`ExoPlayerTvManager`**<br>(AndroidX Media3 HW) | Perfil resuelto y guardado en `StreamProfileDb`. Decodificación continua por hardware con jitter-buffer anti-tirones. |
| **YouTube** | `youtube.com` | ID de video oficial | **`TvYouTubeScreen`** | Reproducción en visor IFrame oficial de TV con IFrame API nativa. Inmunidad ante BotGuard/PO Token (cero 403 Forbidden), telemetría continua de segundos y reporte sincronizado. |
| **Screen Mirroring** | Celular en vivo | Socket WebRTC LAN | **`WebRtcReceiverManager`** | Pipeline H.264 acelerado por hardware (<60ms) con modos Pantalla Completa y Por Aplicación (HU-013). |

---

## 5. ACELERACIÓN NATIVA C++20 (ZERO-COPY / HIGH-PERFORMANCE)

Para garantizar latencias sub-milisegundo y eliminar pausas por Garbage Collector en el móvil y la TV Box:

1. **Clasificación y Enrutamiento (`stream_classifier.cpp`):**
   * Evaluación de 6 ramas multimedia en < 0.1 ms mediante `std::string_view` y búsqueda de patrones `constexpr`.
2. **Desempaquetado de Reproductores Ofuscados (`MediaPlatformResolver`):**
   * Desofuscación de algoritmos Dean Edwards / packed scripts (PelisPlus, Fastream, LuluStream) en memoria contigua (< 1 ms vs 60 ms en Kotlin).
3. **Filtrado Masivo de Publicidad (`NativeAdBlockEngine`):**
   * Búsqueda multi-patrón Aho-Corasick para evaluar más de 100 peticiones web por segundo en < 5 microsegundos.
4. **Grabación PVR Asíncrona en TV Box (`PvrStreamRecorder`):**
   * Búfer circular en RAM (*Ring Buffer*) con volcado en bloques alineados de 128 KB hacia almacenamiento flash/USB, erradicando micro-congelamientos de fotogramas en ExoPlayer.


