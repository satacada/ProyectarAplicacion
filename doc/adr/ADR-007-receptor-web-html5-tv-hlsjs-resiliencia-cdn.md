# ADR-007: Receptor Web HTML5 Dual en TV Box (Hls.js / Chromium) y Resiliencia ante CDNs Protegidas

## Estado
Aprobado

## Fecha
2026-10-21

## Contexto
1. **Rechazo Criptográfico y TLS Fingerprinting en CDNs:**
   Servidores de distribución de video protegidos (como DropCDN, Fastream o redes detrás de Cloudflare) no solo validan tokens de expiración temporal en las URLs, sino que inspeccionan la huella TLS (orden de cipher suites, extensiones TLS), encabezados `Client Hints` y el motor HTTP subyacente.
2. **Limitación Ineludible del Stack Java en ExoPlayer:**
   Cuando ExoPlayer (`DefaultHttpDataSource` / `OkHttpDataSource`) en Android TV descarga los fragmentos `.ts` o listas `.m3u8`, la petición es originada por el motor de red Java. Las CDNs identifican que no se trata de un navegador Chromium legítimo y responden inmediatamente con `HTTP 403 Forbidden`, provocando fallos sistemáticos de reproducción en películas y series protegidas (ej. Mad Max).
3. **Inviabilidad de Forzar Proxy Permanente en el Móvil:**
   Enrutar gigabytes de video a través de un proxy local en el teléfono móvil agota la batería del dispositivo emisor, satura el ancho de banda del router WiFi doméstico y añade puntos de falla si el teléfono se suspende o desconecta.

## Decisión Técnica

### 1. Implementación del Receptor Web HTML5 Nativo (`TvWebStreamScreen`)
- Despliegue de un visor web ligero en la TV Box basado en el motor Android System WebView (Chromium) embebiendo la librería industrial `Hls.js` (`hls.light.min.js`).
- Como Chromium en la TV Box utiliza el stack de red Blink oficial (idéntico a Google Chrome), las peticiones a la CDN viajan con la huella TLS genuina de navegador y las cabeceras completas `Referer`, `Origin` y `User-Agent`.
- La CDN valida la petición como una sesión de navegación 100% auténtica, eliminando permanentemente el bloqueo `HTTP 403 Forbidden`.

### 2. Arquitectura Híbrida Dual y Despacho Dinámico
La TV Box opera con dos motores coordinados:
1. **ExoPlayer por Defecto (90% de los Medios):**
   - Utilizado para MP4 directos, YouTube, streams HLS abiertos, IPTV, medios locales y audio.
   - Garantiza consumo de CPU cercano al 0% y decodificación por hardware nativo (`MediaCodec`).
2. **TvWebStreamScreen (HTML5 / Hls.js):**
   - Se activa de forma prioritaria cuando el emisor detecta firmas criptográficas en la URL (`StreamManifestProbe.hasSignedSecurityToken(url)`).
   - **Fallback Reactivo Automático:** Si ExoPlayer encuentra un error `ERROR_FORBIDDEN_403`, la TV conmuta en caliente y en milisegundos a `TvWebStreamScreen` sin abortar la experiencia de usuario.

### 3. Puente Bidireccional de Control y Zoom (`WebStreamBridge`)
- Implementación de `@JavascriptInterface` para sincronizar posición (`currentTime`), duración (`duration`) y estado de reproducción (`isPlaying`) hacia el móvil y el mando a distancia.
- Soporte para comandos remotos de Play, Pausa, Seek ±10s y los 6 modos de Zoom TV (`Fit`, `85%`, `75%`, `115%`, `130%`, `16:9 Llenar Pantalla`) mediante transformaciones CSS y `object-fit` en tiempo real.

## Consecuencias
- **Positivas:** Reproducción fluida y directa en la TV Box de servidores con alta protección (DropCDN, Fastream) sin depender de proxy en el móvil; cero bloqueos 403; control total remoto con el mando de la TV o el celular; soporte nativo para VOD y transmisiones LIVE.
- **Negativas / Reglas de Seguridad:** PROHIBIDO retirar el componente `TvWebStreamScreen` o forzar exclusivamente ExoPlayer para URLs con tokens firmados.
