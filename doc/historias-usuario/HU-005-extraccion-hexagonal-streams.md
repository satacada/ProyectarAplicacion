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

2. **Filtrado Temprano de Telemetría:**
   - Toda llamada con patrones como `videoStat`, `statNew`, `videoplayermeta`, `telemetry`, `analytics`, `beacon`, `heartbeat` o `ping` debe ser descartada por `TelemetryFilterAdapter` antes de llegar a los extractores.

3. **Aislamiento de OK.ru:**
   - La extracción de `ok.ru` se realiza a través de su adaptador dedicado `OkRuSiteExtractorAdapter` y la lectura DOM de `data-options` (`sniffOkRuDom`), extrayendo el manifiesto HLS maestro y las calidades MP4 reales sin alterar otros sitios.

4. **Principio "Poner y Sacar":**
   - Si se añade un nuevo sitio o si se retira uno existente, el resto del sistema de casteo, fotos y adblocker debe permanecer 100% inalterado y funcional.
