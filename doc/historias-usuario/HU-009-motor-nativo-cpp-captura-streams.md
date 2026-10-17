# HU-009: Motor Nativo C++20 de Captura de Streams, Clasificación en 6 Ramas, Aprendizaje en la Nube y UI Pixel-Perfect

## Descripción
Como usuario de OmniCast, quiero que la detección de videos en páginas web sea instantánea, que no se congele el navegador, que las películas y transmisiones en vivo se reproduzcan con sus configuraciones óptimas de buffer, que los botones y textos nunca se descuadren estéticamente, y que cuando un servidor de video cambie de cabeceras, la aplicación aprenda y se actualice automáticamente desde la nube sin tener que esperar una nueva versión del APK.

## Criterios de Aceptación (Contrato Inamovible)

### Criterio 1: Despacho Nativo en C++20 (<0.1 ms)
- El análisis de peticiones de red (`shouldInterceptRequest`) y respuestas de sniffer DOM se ejecuta a nivel nativo en C++20 mediante JNI.
- Se filtran de inmediato fragmentos aislados (`.ts`), balizas analíticas (beacons) y anuncios pre-roll (<45 segundos en páginas de películas).

### Criterio 2: Especialización de Reproducción en 6 Ramas
- **Rama VOD (Películas/Series):** Configura buffer de 30-60s, habilita seekbar, reanudación de posición en SQLite y selección de pistas de subtítulos/audio.
- **Rama LIVE (En Vivo):** Configura buffer de 2-4s, fija reproducción en *Live Edge* sin seekbar y activa auto-reconexión silenciosa.
- **Rama DASH:** Manejo nativo de `.mpd` segmentado.
- **Rama Audio Puro:** Apaga códecs de video por hardware (`MediaCodec`) para ahorro de 80% de batería.
- **Rama IPTV:** Soporte de listas M3U y tokens de autenticación.
- **Rama Medios Locales:** Despacho vía servidor HTTP interno en `:8089`.

### Criterio 3: Memoria Local de Refuerzo e Inteligencia Colectiva
- Todo stream reproducido exitosamente (>1 min) guarda su perfil de servidor (`host`, `headers`, `bypass_proxy`) en SQLite local.
- Se sincroniza periódicamente con la nube (`stream_rules.json`) para descargar perfiles verificados por otros usuarios.
- Nuevos perfiles de éxito se reportan anónimamente a la nube para beneficio de toda la comunidad.

### Criterio 4: Estándar UI Pixel-Perfect & Zero-Layout-Shift
- Todos los iconos en barras de navegación, botones y diálogos deben ser componentes vectoriales (`ImageVector` Material 3 o XML Vector Drawable). Queda terminantemente prohibido usar caracteres Unicode (`☰`, `<`, `⟳`, etc.).
- Todo texto con contenido dinámico (URL, título de video, calidad, tiempo) debe incluir `maxLines = 1`, `overflow = TextOverflow.Ellipsis` y `softWrap = false`.
- Los botones de acción deben poseer dimensiones fijas (ej. 36dp x 36dp o 40dp x 40dp con área táctil de 48dp), garantizando que las variaciones en el contenido de texto nunca empujen ni deformen los elementos vecinos.
