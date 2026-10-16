# ADR-004: Arquitectura Colectiva de Auto-Recuperación y Auto-Curación por IA (OTA)

## Estado
Aceptado (2026-10-15)

## Contexto
Los sitios web de streaming y servicios de alojamiento de video (ej. Lulustream, Fastream, Streamwish) alteran de forma periódica sus cabeceras anti-hotlink (`Referer`, `Origin`, tokens de embed) provocando bloqueos HTTP 403 Forbidden y la interrupción de la reproducción tanto en el móvil como en Android TV. Recompilar e instalar manualmente nuevas versiones de la aplicación cada vez que un sitio web cambia sus reglas es insostenible y no escala a múltiples usuarios.

## Decisión
Implementar un circuito cerrado de **Inteligencia Colectiva Centralizada con Costo Cero ($0)**:
1. **On-Device Heuristics (`StreamIntelligenceEngine.kt`):** El emisor evalúa dinámicamente cada flujo multimedia asociando cabeceras de origen y descartando anuncios <60s sin depender de código estático rígido.
2. **Telemetría Silenciosa de Fallos (`StreamRuleSyncManager.kt`):** Cuando una CDN devuelve un código no resuelto (ej. 403), la app despacha un log anónimo a Supabase (`event = UNRESOLVED_STREAM`).
3. **Inferencia Asíncrona en la Nube (`scripts/ai_stream_healer.py`):** Un workflow de GitHub Actions se ejecuta cada 4 horas. Consulta los fallos en Supabase e invoca a **Google Gemini 1.5 Flash** (capa gratuita) para deducir las cabeceras exactas.
4. **Distribución OTA en Caliente (`rules/live_stream_rules.json`):** Gemini actualiza automáticamente el archivo de reglas en el repositorio. Todas las apps descargan el JSON en segundo plano, quedando reparadas sin requerir actualización por APK.

## Consecuencias
- **Positivas:**
  - Cero costo operativo ($0 en infraestructura).
  - Un único fallo reportado repara la experiencia para todos los usuarios en el mundo.
  - No se satura el hilo de la app ni se expone la API key de Gemini en los dispositivos clientes.
- **Negativas / Mitigaciones:**
  - La sincronización depende de conectividad a GitHub/Supabase; mitigado mediante un catálogo de reglas locales base embebidas en los assets de la app (`default_stream_rules.json`).
