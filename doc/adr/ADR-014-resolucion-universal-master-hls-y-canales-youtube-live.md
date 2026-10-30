# ADR-014: Resolución Universal de Master HLS (RFC 8216) y Desbloqueo de Canales YouTube Live

## Estado
Aprobado (v3.5.43)

## Contexto y Causa Raíz
1. **Pérdida de Audio en Transmisiones en Vivo (Sky News, Das Erste):**
   Al sintonizar canales de televisión en vivo que implementan HLS con flujos desmultiplexados (audio y video separados según RFC 8216, mediante etiquetas `#EXT-X-MEDIA:TYPE=AUDIO`), las peticiones HTTP subsiguientes capturaban sub-variantes de solo video (ej. `stream04/streamPlaylist.m3u8` o `master270p700.m3u8`). Al reemplazar la URL en el reproductor de la TV Box (ExoPlayer), este reproducía el sub-stream de video que carece físicamente de paquetes de audio en sus segmentos `.ts`, resultando en reproducción muda (0 pistas de audio). ExoPlayer requiere indispensablemente el `master.m3u8` para conocer las pistas de audio y orquestar la sincronización de ambos flujos.
2. **Omisión de Canales de Noticias con YouTube Live Embebido (Espreso TV):**
   En sitios de canales de noticias y eventos que incrustan transmisiones en vivo de YouTube mediante reproductores embebidos, el análisis de streaming descartaba la URL si el host de la página actual no era `youtube.com`, impidiendo la detección y casteo nativo del canal.
3. **Contaminación por Hardcoding de Dominios:**
   Existían comprobaciones en duro (`dropcdn.io`, `fastream.to`) en clases utilitarias globales (`BrowserUrlHelper`), violando la Regla 6 del proyecto (Arquitectura Hexagonal, Principio "Poner y Sacar" y Cero Código Spaghetti).

## Decisión de Arquitectura

### 1. Núcleo de Dominio Desacoplado: `HlsMasterResolver` (RFC 8216)
Se crea el caso de uso `HlsMasterResolver.kt` en el módulo `:core:domain:usecases` como la fuente única de verdad para la resolución e inspección de listas HLS:
- **Inspección de Contenido (RFC 8216):**
  - Es **Master Playlist** si contiene `#EXT-X-STREAM-INF:` o `#EXT-X-MEDIA:TYPE=`.
  - Es **Media Playlist (Sub-variante)** si contiene `#EXTINF:` o `#EXT-X-TARGETDURATION:`.
- **Detección Estructural de Sub-variantes:**
  Identifica URLs que representan sub-variantes sin requerir descarga de red, analizando patrones universales en la ruta: carpetas de bitrate/stream (`/stream\d+/`, `/chunklist`, `/tracks-v\d+`), sufijos de resolución/tasa (`270p700`, `index-v1-a1`), o archivos `chunks.m3u8`.
- **Derivación y Re-ascenso Universal a Master:**
  Transforma sub-variantes a su lista maestra ascendiendo al directorio padre (`../master.m3u8` o `../playlist.m3u8`) o limpiando sufijos numéricos, de forma genérica e independiente del dominio proveedor.

### 2. Doble Escudo Defensivo (Sender y Receiver)
- **Primer Escudo (Sender - `InAppBrowserScreen` / `BrowserUrlHelper`):**
  - Si un flujo marcado como Master ya fue detectado para una página/video, la llegada de una sub-variante posterior no lo sobreescribe ni lo degrada.
  - `BrowserUrlHelper.deriveMasterPlaylistUrl` y `UniversalHlsExtractorAdapter` delegan completamente en `HlsMasterResolver`.
- **Segundo Escudo (Receiver - `ExoPlayerTvManager`):**
  - Al recibir una URL para reproducción en `playStream()` y `playClean()`, la TV Box ejecuta `HlsMasterResolver.deriveMasterPlaylistUrl(cleanUrl)` antes de instanciar el `MediaItem`. Si una sub-variante traspasa la red, la TV se auto-recupera apuntando al Master para recibir las pistas de audio demuxed.

### 3. Detección Universal de YouTube Live Embebido
- Se elimina la restricción que exigía que la página principal fuera de YouTube. Si se detecta un stream de YouTube (live o VOD) incrustado en cualquier sitio web externo, se extrae el ID y se habilita la proyección nativa.

### 4. Erradicación de Nombres de Dominio en Métodos Globales
- Se remueven los `if (lower.contains("fastream.to"))` de los métodos globales. Los patrones de reescritura de streaming adaptativo se basan en firmas semánticas de URL (`.urlset`, segmentos indexados) respetando las excepciones de seguridad documentadas en ADR-013.

## Consecuencias
- **Audio 100% Restaurado:** Canales en vivo con pistas desmultiplexadas (Sky News, Das Erste, etc.) cargan siempre su lista maestra en ExoPlayer, decodificando audio y video sin mutismos.
- **Canales Embebidos Operativos:** Portales de noticias y transmisiones internacionales con YouTube Live son proyectables directamente.
- **Mantenibilidad y Robustez:** El dominio `:core` concentra las reglas RFC 8216, reutilizables tanto por el Sender móvil como por el Receiver de TV sin duplicación de código.
