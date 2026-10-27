# ADR-012: Filtro Centralizado de Higiene de Red y Purificación de Extractores Hexagonales

## Estado
Aprobado (v3.5.3)

## Contexto y Causa Raíz
En la versión v3.5.1, la delegación hexagonal en `isRealMedia()` permitió que el registro `StreamExtractorRegistry` evaluara directamente los candidatos multimedia. Sin embargo, los adaptadores individuales (`UniversalHlsExtractorAdapter`, `FastreamExtractorAdapter`, `OkRuSiteExtractorAdapter`) contenían reglas permisivas basadas únicamente en nombres de host (por ejemplo `contains("morencius.com")` o `contains("dr0pstream.com")`), que previamente eran contenidas por un filtro rígido central.

Esto provocó tres fallos críticos en producción:
1. **Pelismart:** Detección de 9 flujos espurios al capturar la página web del reproductor iframe (`morencius.com/embed/...`) y telemetría de Cloudflare (`morencius.com/cdn-cgi/rum?`), enviando HTML no reproducible a la TV.
2. **PelisPlus:** Captura de telemetría y páginas iframe de DropCDN (`/cdn-cgi/`, `/e/`).
3. **OK.ru:** Captura de balizas de telemetría y píxeles de analítica de Mail.ru (`top-fwz1.mail.ru/tracker?...video...`) y páginas web de portada (`ok.ru/dk?st.cmd=anonymVideo`), provocando flujos duplicados y fallos de reproducción en la TV.
4. **Pelisgo:** Persistencia de anuncios y banners gráficos in-page sobre el contenedor de video.

## Decisión de Arquitectura: El Principio del Doble Cerrojo

Para erradicar la duplicación de código y garantizar que ningún sitio nuevo degrade a los portales existentes, se implementa una arquitectura en dos capas:

### 1. Barrera Universal de Higiene Previa (`TelemetryFilterAdapter.kt`)
Se centraliza en el núcleo hexagonal (`IStreamTelemetryFilterPort`) el descarte inmediato (0ms) de toda URL que no sea un recurso reproducible:
- **Telemetría y Analítica:** `/cdn-cgi/`, `tracker`, `top-fwz`, `videostat`, `statnew`, `beacon`, `heartbeat`.
- **Páginas de Iframe / Embed Web:** `/embed/`, `/embed-`, `/e/`, `/widget/`, `/iframe/` (excepto YouTube oficial).
- **Catálogos y Portadas:** `anonymvideo`, `showcase`, `st.cmd`, `/dk?`.
- **Extensiones Estáticas y HTML:** `.html`, `.htm`, `.css`, `.js`, etc.

### 2. Rigor Estricto en Adaptadores Específicos
Queda terminantemente prohibido que cualquier adaptador (`IMediaStreamExtractorPort`) acepte una URL únicamente por su dominio. Todo adaptador debe verificar que el candidato contenga firmas o extensiones de video legítimas:
- **`UniversalHlsExtractorAdapter`:** Para hosts como `morencius.com`, `swdyu.com`, `cloudemb.com`, exige `.m3u8`, `/hls`, `master` o `chunklist`, rechazando `/embed/` y telemetría.
- **`OkRuSiteExtractorAdapter`:** Rechaza páginas web de navegación y trackers; se enfoca exclusivamente en los flujos firmados de la CDN rusa (`mycdn.me`, `okcdn.ru` con `vd`, `type=`, `sig=`, `expires=`), mapeando calidades y asignando `Referer: https://ok.ru/`.
- **`FastreamExtractorAdapter`:** Exige `.m3u8` o `/hls` y sintetiza cabeceras anti-403 (`Referer`, `Origin`, `Sec-Fetch-Dest`).

## Consecuencias
- **Punto Único de Verdad (DRY):** Cualquier nueva trampa publicitaria o telemetría se bloquea en `TelemetryFilterAdapter` sin tocar los extractores.
- **Transmisión Autónoma:** La TV Box recibe únicamente manifiestos HLS (`.m3u8`) o MP4 directos, garantizando decodificación por hardware nativa en ExoPlayer sin cierres forzados.
