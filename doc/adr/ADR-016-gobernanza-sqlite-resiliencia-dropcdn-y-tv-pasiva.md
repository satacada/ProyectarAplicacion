# ADR-016: Gobernanza Dinámica por SQLite, Preservación Fiel de Dominios CDN y TV Box Pasiva

## Estado
Aprobado (v3.5.58)

## Contexto y Causa Raíz
1. **Fallo de Conexión en Películas DropCDN / Dr0pstream (Sacrifice a las 14:29):**
   Al transmitir contenidos protegidos alojados en DropCDN (`https://r1.dropcdn.io/hls2/02/00039/jxau9sw23lqp_n/master.m3u8?t=...&srv=ds1i`), la reproducción colapsaba de inmediato en la Android TV Box con el error:
   `[TvWebResourceInterceptor] Excepción en recurso web: Failed to connect to ds1i.dropcdn.io/192.168.10.23:443`
   `[TvWebStreamJS] [ERROR] TvWebStreamJS [HLS_ERROR] networkError manifestLoadError code=0`
2. **Causa Raíz - La Mutación Destructiva de Host (Herencia de v3.32.70):**
   En la versión histórica `v3.32.70`, cuando el nodo frontal `r*.dropcdn.io` arrojó un error 502 Bad Gateway, se introdujo en `BrowserUrlHelper.kt` y `StreamManifestProbe.kt` una rutina de sustitución en caliente:
   `u = u.replace(Regex("https?://r\\d*\\.dropcdn\\.io"), "https://$srv.dropcdn.io")`
   Esta lógica asumía que el parámetro `srv=` siempre contenía un balanceador público (ej. `ds4`). Sin embargo, en *Sacrifice*, la CDN entregó `srv=ds1i`. El subdominio `ds1i.dropcdn.io` resuelve a la IP privada interna del datacenter **`192.168.10.23`** (RFC 1918), completamente inalcanzable desde internet público, provocando el rechazo inmediato de conexión.
3. **Violación de la Pasividad de la TV Box:**
   En `ExoPlayerTvManager.kt`, persistían llamadas residuales a `StreamManifestProbe.sanitizeMediaUrl(url)`. La TV Box intervenía y mutaba URLs recibidas en lugar de actuar como un reproductor puramente pasivo y fiel a las directivas del Emisor.
4. **Desconexión entre Nube y Cabeceras SQLite:**
   `CloudStreamRuleSyncManager.kt` consultaba a Supabase (`omnicast_stream_rules`) descargando únicamente `host`, `branch` y `bypass_proxy`, ignorando el campo `headers` (JSON). Esto obligaba a mantener seeds en código duro para cabeceras críticas como `User-Agent: UA_FASTREAM` y `X-Target-Player: CHROMIUM_ANTI_CORS`.

---

## Decisiones de Arquitectura

### 1. Ley de Intocabilidad y Preservación Fiel del Host de CDN
- **Prohibición de Reescritura de Host por Parámetros Query:**
  Se erradica por completo la inspección del parámetro `srv=` en `BrowserUrlHelper.kt` y `StreamManifestProbe.kt`. La función `sanitizeMediaUrl(url)` se limita estrictamente a `url.trim()`.
- **Conexión al Dominio Público Asignado:**
  El reproductor de la TV Box se conecta directamente al host público legítimo provisto por el servidor (`r1.dropcdn.io` / IP pública `91.218.50.202`), el cual resuelve y despacha el tráfico sin interferencia de subdominios privados internos.

### 2. TV Box 100% Pasiva y Agnóstica
- **Supresión de Alteraciones en el Receptor (`ExoPlayerTvManager.kt`):**
  Se eliminan las llamadas a `sanitizeMediaUrl` tanto en `playStream()` como en `playClean()`. La TV Box confía plenamente en la URL enviada por el Emisor y la pasa directamente al resolver de lista maestra (`HlsMasterResolver.deriveMasterPlaylistUrl(url) ?: url`).
- **Aislamiento por Interceptor Nativo Anti-CORS:**
  La TV Box ejecuta las directivas despachadas en la cabecera `X-Target-Player`. Al recibir `CHROMIUM_ANTI_CORS`, enruta incondicionalmente a `TvWebStreamScreen`, donde `TvWebResourceInterceptor` responde las peticiones preflight OPTIONS en 0 ms e inyecta `Access-Control-Allow-Origin: *`.

### 3. Sincronización Completa de Cabeceras Cloud a SQLite (`CloudStreamRuleSyncManager.kt`)
- **Ampliación de Consulta REST en Supabase:**
  La sincronización periódica (cada 3 horas) consulta:
  `$SUPABASE_RULES_URL?select=host,branch,bypass_proxy,headers&order=last_verified_at.desc&limit=100`
- **Persistencia en Columna `headers_json`:**
  Las cabeceras asociadas al host (`User-Agent`, `X-Target-Player`, `Referer`) se parsean del JSON remoto y se registran en la base de datos local SQLite (`StreamProfileDatabase.get().recordSuccess(host, hdrsMap, bypass, branch)`).
- **Arquitectura Offline-First y Resiliencia Total:**
  Si Supabase está inaccesible o no hay conexión a internet, la aplicación opera sin interrupciones consultando la base de datos local SQLite persistida en el dispositivo.

---

## Consecuencias
- **Reproducción Inmediata de DropCDN / Dr0pstream:** La película *Sacrifice* y portales con servidores DropCDN reproducen sin fallos de red ni colapso por IPs privadas.
- **Cero Código Spaghetti de URLs:** Se elimina la manipulación impredecible de nombres de servidor en tiempo de ejecución.
- **Desacoplamiento Operativo:** Nuevas protecciones o cabeceras de servidores se configuran en la nube o en SQLite sin necesidad de recompilar y publicar nuevas versiones del APK.
- **Cumplimiento Estricto de Clean Architecture:** El Emisor resuelve la inteligencia de red y la TV Box reproduce pasivamente con aceleración por hardware.
