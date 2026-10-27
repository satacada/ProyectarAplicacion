# HU-005: Detección Hexagonal y Principio "Poner y Sacar" de Streams Multimedia

## Descripción del Requisito
Como usuario de OmniCast,
Quiero que la detección de videos en el navegador web capture únicamente flujos de video reales y funcionales (.m3u8, .mp4, .mpd, CDN de video legítimas),
Para que la lista de opciones no se contamine con llamadas de telemetría o estadísticas que no sirven, y que dar soporte a un sitio nuevo jamás desestabilice o modifique lo que ya funciona.

---

## Criterios de Aceptación Inamovibles (Contrato)

1. **Arquitectura Hexagonal Pura:**
   - La lógica de decisión reside en `:core:domain` mediante `DetectMediaStreamUseCase`.
   - Los extractores universales (`UniversalHlsExtractorAdapter`, `UniversalMp4ExtractorAdapter`, `UniversalDashExtractorAdapter`) son intocables y estables.
   - Cada sitio web no estándar debe ser un adaptador independiente (`IMediaStreamExtractorPort`) que se conecta o desconecta en `StreamExtractorRegistry`.

2. **Filtrado Temprano de Telemetría e Iframes (ADR-012):**
   - Toda llamada con patrones como `/cdn-cgi/`, `tracker`, `top-fwz`, `videostat`, `statnew`, `telemetry`, `analytics`, `beacon`, `heartbeat` o `ping` debe ser descartada por la función central canónica `isTelemetryOrGarbage(url)` de `TelemetryFilterAdapter` antes de que se consulte a cualquier extractor.
   - Las páginas web de iframe (`/embed/`, `/embed-`, `/e/`, `/widget/`) y portadas (`anonymvideo`, `showcase`, `st.cmd`, `/dk?`) quedan automáticamente excluidas como flujos de video por `isTelemetryOrGarbage(url)` para evitar el envío de HTML a la TV Box.

3. **Aislamiento de OK.ru y CDNs con Firma:**
   - La extracción de `ok.ru` se realiza a través de su adaptador dedicado `OkRuSiteExtractorAdapter` capturando exclusivamente los flujos firmados de su CDN (`mycdn.me`, `okcdn.ru` con `sig=` y `type=`), mapeando calidades (1080p, 720p) e inyectando `Referer: https://ok.ru/`. Las páginas web HTML y los trackers rusos de Mail.ru quedan estrictamente excluidos.

4. **Principio "Poner y Sacar":**
   - Si se añade un nuevo sitio o si se retira uno existente, el resto del sistema de casteo, fotos y adblocker debe permanecer 100% inalterado y funcional.
   - Los extractores especializados (`FastreamExtractorAdapter`, `UniversalHlsExtractorAdapter`) exigen firmas de media reales (`.m3u8`, `/hls`, `.mp4`) y prohíben la aceptación de URLs únicamente por coincidencia de nombre de host.
