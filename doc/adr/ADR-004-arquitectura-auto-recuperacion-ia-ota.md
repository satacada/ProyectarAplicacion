# ADR-004: Arquitectura Colectiva de Auto-Recuperación y Auto-Curación por IA (OTA)

## Estado
Aceptado (2026-10-15)

## Contexto
Los sitios web de streaming y servicios de alojamiento de video (ej. Lulustream, Fastream, Streamwish) alteran de forma periódica sus cabeceras anti-hotlink (`Referer`, `Origin`, tokens de embed) provocando bloqueos HTTP 403 Forbidden y la interrupción de la reproducción tanto en el móvil como en Android TV. Recompilar e instalar manualmente nuevas versiones de la aplicación cada vez que un sitio web cambia sus reglas es insostenible y no escala a múltiples usuarios.

## Decisión
Implementar una **Arquitectura Escalonada de Costo Cero Absoluto ($0)** donde el **90% del trabajo lo realizan Clases y Objetos Kotlin deterministas en el dispositivo** y solo el **10% de casos inéditos** escala a la bolsa gratuita de IA:
1. **Nivel 1 — Captura Fiel sin "Cosas en Duro" (Clases Nativas, $0):** Se respeta siempre el `Referer` y `Origin` real capturado por Chromium (`shouldInterceptRequest`) y el `window.location.href` del `<iframe>` reproductor en JS, eliminando listas estáticas en duro que pisen la fuente real.
2. **Nivel 2 — Objetos Deterministas de Validación y Auto-Curación Local ($0):**
   - `StreamManifestProbe`: Sonda ligera Pre-Cast que lee los primeros 4 KB del `.m3u8` para descartar enlaces que crashean ExoPlayer, detectar **Streams en Vivo** (sin `#EXT-X-ENDLIST`) y separar **Tráilers (<3m)** de **Películas (>20m)**.
   - `HeaderSelfHealer`: Ante un `HTTP 403`, permuta localmente en <1s las 4 fuentes reales (Iframe $\rightarrow$ `pageUrl` $\rightarrow$ CDN $\rightarrow$ Limpio) y memoriza localmente la combinación exitosa (`learned_rules.json`).
   - `TokenRenewalFallback`: Si un enlace guardado en *"Ver más tarde / Favoritos"* caducó, intenta re-extraerlo de `pageUrl`; si falla el play automático, abre directamente `pageUrl` sin obligar al usuario a volver a buscar la película.
3. **Nivel 3 — Bolsa Gratuita de IA en Lote (`scripts/ai_stream_healer.py`, $0):** Únicamente cuando un dominio nuevo falla todos los intentos del Nivel 2, se registra en Supabase (`UNRESOLVED_STREAM`) y un cron de GitHub Actions consulta en lote a **Google Gemini Flash (Free Tier: 1,500 req/día gratis)** para publicar la regla en `rules/live_stream_rules.json` (OTA).

## Consecuencias
- **Positivas:**
  - **Costo $0 garantizado de por vida:** No consume tokens de IA en el uso diario porque los Niveles 1 y 2 son 100% objetos y clases en Kotlin.
  - **Velocidad instantánea:** Las clases Kotlin evalúan el manifiesto y reparan cabeceras en milisegundos sin depender de servidores externos.
- **Negativas / Mitigaciones:**
  - La capa gratuita de IA solo se invoca en lote (máximo 4-6 veces al día en GitHub Actions), manteniéndose siempre por debajo del 1% del límite gratuito.

