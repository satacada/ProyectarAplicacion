# ADR-005: Autoaprendizaje Proactivo de Dominios Publicitarios y Resiliencia de Flujos Multimedia

## Estado
Aprobado

## Fecha
2026-10-16

## Contexto
1. **Limitación de Listas Remotas Estáticas:** Las redes de anuncios y acortadores maliciosos en sitios de streaming generan nuevos dominios desechables (ej. `073m.com`, `ayhal.com`) en cuestión de horas. Las listas remotas públicas (EasyList) tardan días o semanas en catalogarlos. Si el bloqueador solo consulta la base de datos descargada, estos dominios escapan al filtro repetidamente.
2. **Trampas de Click con Gesto de Usuario (`r.hasGesture()`):** Los sitios piratas insertan trampas transparentes sobre los controles de reproducción. Cuando el usuario toca la pantalla para presionar "Play", Chromium marca el evento con `hasGesture() == true`. Una regla que confíe en `hasGesture()` para autorizar redirecciones foráneas permite el secuestro de la ventana.
3. **Falsos Negativos en Sondas Pre-Cast (HTTP 403 de CDNs):** Servidores CDN como `dropcdn.io` emplean mecanismos anti-bot que invalidan peticiones out-of-band hechas mediante `HttpURLConnection` de Java (requiriendo fingerprints TLS de Chromium o tokens de un solo uso). Si la sonda de validación previa descarta el stream ante un código 403, el usuario experimenta "0 videos detectados" a pesar de que el video se reproduce sin problemas en el navegador móvil.

## Decisión Técnica

### 1. Autoaprendizaje Proactivo Local (Zero-Latency SQLite Persistence)
- En el método `shouldOverrideUrlLoading`, cuando el usuario se encuentra en un contexto cinematográfico (`inMovie` o con streams detectados):
  Cualquier navegación hacia un `targetHost` que no coincida con el dominio del sitio (`!isSameSite`), que no sea un motor de búsqueda oficial (Google, Bing, Yandex) y que no sea un host multimedia conocido o sitio en lista blanca, se clasificará como secuestro publicitario sin importar el valor de `r.hasGesture()`.
- El dominio foráneo se bloqueará de inmediato y se registrará de forma asíncrona pero prioritaria en la tabla SQLite local `ad_domains` mediante `AdBlocker.learnAndBlockAdHost(targetHost, curHost)`.
- Desde ese instante, el dominio queda en la caché en memoria (`cachedDomains`) y en SQLite, bloqueando cualquier solicitud subsecuente antes de tocar la red.

### 2. Resiliencia de Streams sin Bloqueo de ExoPlayer (Fail-Safe Streaming)
- Si una sonda previa `StreamManifestProbe.probeAndHeal()` falla con HTTP 403 o códigos no-200 para un stream capturado que cumple el patrón de medio real (`isRealMedia(url)`), el motor no purgará el stream de `detectedStreams`.
- En lugar de devolver una lista vacía, `StreamQualityDetector` proveerá una opción de calidad de respaldo ("▶ Flujo Directo Nativo") con las cabeceras originales del navegador (Referer, Cookie, User-Agent, Origin, Sec-Fetch-*).
- En `InAppBrowserScreen.kt`, la función `detectedStreams.retainAll` jamás eliminará flujos si la validación remota falla por razones de CDN, garantizando que el usuario siempre pueda transmitir el video activo a la TV Box.

### 3. Inmunidad Colectiva Descentralizada mediante Supabase (Crowd-sourced AdBlock)
- Todo nuevo dominio publicitario detectado en frío por un usuario se reporta asíncronamente en segundo plano a la tabla `omnicast_system_logs` en Supabase con el evento `ADBLOCK_LEARNED_DOMAIN`.
- El gestor `AdBlockRuleSyncManager` consulta cada 2 horas los dominios reportados por otros dispositivos y los inyecta en la base SQLite local, propagando la protección a toda la base instalada sin esperar ciclos de actualización de EasyList o despliegues de APK.


## Consecuencias
- **Positivas:** Cero anuncios repetidos tras el primer intento; bloqueo en frío de dominios no catalogados remotamente; disponibilidad ininterrumpida de reproducción en TV para CDNs protegidos como `dropcdn.io`.
- **Negativas:** Pequeña probabilidad de bloquear dominios foráneos no multimedia si el usuario hace clic intencionalmente en un enlace externo dentro de una página de películas (resuelto mediante la Lista Blanca de Sitios Permitidos).
