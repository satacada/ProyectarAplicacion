# HU-018: Gobernanza Dinámica por SQLite, Preservación Fiel de Dominios CDN y TV Box Pasiva

**Épica:** Gobernanza de Red, Resiliencia CDN y Arquitectura Pasiva del Receptor  
**Versión de Introducción:** v3.5.58  
**Estado:** Inamovible / Aprobado por el Usuario  

---

## 1. Descripción de la Historia

**Como** usuario de OmniCast en Android TV y teléfono móvil,  
**Quiero** reproducir películas y transmisiones alojadas en servidores de video protegidos (DropCDN, Dr0pstream, Fastream) sin fallos de conexión `code=0` ni bloqueos de red interna,  
**Para** disfrutar de cualquier título (*Sacrifice*, películas y series) de forma inmediata y autónoma en mi televisor sin configuraciones manuales ni caídas por dominios obsoletos.

---

## 2. Criterios de Aceptación (Contrato Inamovible)

1. **Preservación Fiel del Host Público de CDN (Anti-Private RFC 1918 Leak):**
   - El sistema emisor (`BrowserUrlHelper.kt`) y el núcleo de dominio (`StreamManifestProbe.kt`) tienen terminantemente prohibido alterar, mutar o sustituir el host público asignado por la CDN (`r*.dropcdn.io`, `ds*.dropcdn.io`) mediante parámetros query de enrutamiento interno (`srv=`).
   - El enlace de streaming debe enviarse a la TV Box conservando fielmente el balanceador público asignado por la infraestructura del servidor de video (ej. `r1.dropcdn.io` con IP pública `91.218.50.202`).
   - Queda erradicada cualquier conversión hacia subdominios privados internos del datacenter (`ds1i.dropcdn.io` / `192.168.10.23`) que cause rechazo o timeout de conexión en la TV.

2. **TV Box 100% Pasiva y Agnóstica:**
   - La Android TV Box (`ExoPlayerTvManager.kt`, `TvReceiverActivity.kt`) opera estrictamente como un reproductor pasivo de alta eficiencia.
   - Queda prohibido que la TV Box altere o reescriba URLs recibidas del Emisor (`StreamManifestProbe.sanitizeMediaUrl` queda eliminado de los métodos de reproducción de la TV).
   - La TV Box reproduce incondicionalmente la URL entregada por el Emisor con el motor contratado (`ExoPlayer` o `TvWebStreamScreen` según `X-Target-Player`).

3. **Gobernanza Dinámica de Cabeceras en SQLite Local (Offline-First):**
   - Las reglas de identidad (`User-Agent`) y tipo de reproductor (`X-Target-Player`) se almacenan de forma estructurada en la tabla `stream_profiles` de la base de datos local SQLite (`omnicast_stream_profiles.db`).
   - `CloudStreamRuleSyncManager` sincroniza periódicamente con la tabla `omnicast_stream_rules` en Supabase descargando y persistiendo el JSON íntegro de cabeceras (`headers`).
   - Si la red externa o Supabase no están disponibles, la aplicación continúa funcionando con máxima resiliencia usando la base de datos local SQLite sin fallos.

4. **Blindaje Anti-CORS Universal:**
   - Para flujos asignados a `CHROMIUM_ANTI_CORS`, el interceptor nativo de la TV Box (`TvWebResourceInterceptor.kt`) responde preflights HTTP OPTIONS en 0 ms e inyecta la cabecera `Access-Control-Allow-Origin: *`, erradicando bloqueos de seguridad de Chromium.
