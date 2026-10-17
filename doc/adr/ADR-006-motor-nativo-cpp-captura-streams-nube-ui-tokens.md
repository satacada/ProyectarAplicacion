# ADR-006: Motor Nativo C++20 de Captura, Clasificación en 6 Ramas, Sincronización en la Nube y Estándar Pixel-Perfect UI

## Estado
Aprobado

## Fecha
2026-10-17

## Contexto
1. **Sobrecarga y Latencia en JVM/Kotlin:** Analizar cientos de peticiones HTTP en `shouldInterceptRequest` mediante expresiones regulares en Kotlin genera sobrecarga de recolección de basura (GC) y micro-tirones durante el scroll de páginas complejas en WebView.
2. **Diferenciación Crítica de Tipos de Medios:** Un stream en vivo (Live) y una película almacenada (VOD) requieren configuraciones antagónicas en ExoPlayer:
   - VOD requiere buffers grandes (30-60s), seekbar completa, pistas de audio/subtítulos y memoria de reanudación.
   - LIVE requiere buffers mínimos (2-4s), borde en vivo (*live edge*) y reconexión silenciosa ante pérdida de sockets.
   - Audio Puro requiere desactivar decodificadores de video por hardware (`MediaCodec`) para ahorrar 80% de batería.
   - Medios Locales requieren microservidor HTTP interno (`:8089`) en red WiFi local.
3. **Fragilidad ante Cambios en Servidores de Video:** Cuando servidores como DropCDN o Fastream alteran sus cabeceras obligatorias o trampas, los usuarios experimentan pantallas negras (HTTP 403) hasta que se compila y distribuye una nueva versión de la aplicación.
4. **Deformación Estética de la Interfaz (Layout Shift):** El uso de símbolos Unicode en lugar de iconos vectoriales Material y la falta de contención en textos dinámicos (`TextOverflow.Ellipsis`, límites de líneas) provocan que al variar títulos o calidades se deformen barras, botones y diálogos.

## Decisión Técnica

### 1. Motor Nativo C++20 (libstream_classifier)
- Implementación de un motor nativo C++20 compilado con Android NDK y expuesto mediante JNI de latencia ultra-baja (<0.1 ms).
- Evaluación en microsegundos de URLs, tipos MIME, extensiones y patrones de cabeceras (`std::string_view`, regex compiladas).
- Descarte proactivo en C++ de anuncios pre-roll (<45s), balizas de telemetría (beacons), fragmentos aislados (`.ts`, `.m4s`) e iframes publicitarios.

### 2. Despachador de 6 Ramas Polimórficas (MediaBranch Dispatcher)
Arquitectura extensible mediante `enum class MediaBranch : uint8_t`:
1. `VOD_HLS_MP4`: Películas/series con buffer extendido, seekbar y reanudación.
2. `LIVE_HLS`: Emisiones en vivo con buffer ultra-corto, live-edge y auto-reconexión.
3. `DASH_MPD`: Segmentos MPEG-DASH con soporte multicanal de audio.
4. `AUDIO_ONLY`: Podcasts y radios web con apagado total de `MediaCodec` de video.
5. `IPTV_STREAM`: Protocolos de listas con soporte de tokens y auth headers.
6. `LOCAL_MEDIA`: Fotos y videos locales servidos por `LocalImageServer` en el puerto `:8089`.

### 3. Memoria Local de Refuerzo (SQLite) e Inteligencia Colectiva en la Nube
- **Caché Local de Éxito:** Al reproducir con éxito un stream (>1 min sin error), se guarda en SQLite la combinación ganadora (`host`, `headers`, `bypass_proxy`, `branch`).
- **Sincronización en Segundo Plano:** `WorkManager` descarga periódicamente un catálogo comprimido `stream_rules.json` desde la nube con perfiles actualizados por la comunidad.
- **Aporte Colaborativo Anónimo:** Cada perfil nuevo verificado localmente se envía a la nube en segundo plano sin datos personales ni URLs privadas.
- **Cero Actualizaciones de APK:** Los cambios en sitios web se resuelven de forma inmediata en la nube para todos los usuarios.

### 4. Estándar UI "Pixel-Perfect & Zero-Layout-Shift"
- **Prohibición de Símbolos Unicode:** Reemplazo estricto de caracteres como `☰`, `<`, `⟳` por `ImageVector` oficiales de Material 3 (`Icons.Rounded.*`) o drawables vectoriales XML con tamaños estándar (18dp, 24dp, 32dp).
- **Contención Estricta de Textos:** Todo texto dinámico (títulos, URLs, calidades) debe declarar `maxLines = 1`, `overflow = TextOverflow.Ellipsis` y `softWrap = false`.
- **Botones con Dimensiones Inamovibles:** Los elementos interactivos deben poseer un contenedor fijo (ej. 36dp o 40dp con área táctil accesible de 48dp) de modo que ninguna variación de texto vecino desplace su posición en pantalla.

## Consecuencias
- **Positivas:** Cero lag en navegación web; detección instantánea; reproducción optimizada según hardware y tipo de medio; inmunidad ante cambios remotos de servidores sin recompilar APKs; interfaz estable, estética y profesional.
- **Negativas:** Requiere mantener toolchain de NDK (C++) y backend ligero de sincronización de reglas en la nube.
