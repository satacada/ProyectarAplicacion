# HU-014: Inteligencia Colectiva y Reputación Ponderada de Streams

**Épica:** Análisis Inteligente, Detección de Streams y Experiencia de Usuario  
**Versión de Introducción:** v3.4.33  
**Estado:** Implementado / Validado  

---

## 1. Descripción de la Historia

**Como** usuario del explorador web integrado en OmniCast Sender,  
**Quiero** que la aplicación aprenda automáticamente qué enlaces y servidores de video son películas reales que funcionan y cuáles son falsos positivos, publicidad o enlaces caídos, compartiendo dicho aprendizaje de forma colectiva con toda la comunidad a través de la nube sin demorar el flujo de la aplicación,  
**Para** ver siempre resaltados con colores y distintivos claros los enlaces con mayor probabilidad de reproducir la película y no perder tiempo probando enlaces inútiles.

---

## 2. Criterios de Aceptación (Contrato Inamovible)

1. **Arquitectura Zero-Lag (Cero Latencia en la Interfaz de Navegación):**
   - Las consultas de reputación y ordenamiento de flujos se resuelven en memoria RAM (`ConcurrentHashMap`) en menos de 0.1 ms al momento de abrir la lista de videos detectados.
   - Queda estrictamente prohibido hacer llamadas de red bloqueantes que congelen el hilo principal o retrasen la navegación web.

2. **Diferenciación Visual Inmediata por Colores y Estados en la Lista (`WvcVideoListSheet`):**
   - 🟢 **Flujo Exitoso / Verificado:** Cuando un flujo es probado y comienza a reproducirse con éxito, su tarjeta adquiere fondo verde translúcido, thumbnail verde y distintivo `• 🟢 [EN EMISIÓN]`.
   - 🔴 **Flujo Fallido / Falso Positivo:** Si un flujo probado da error HTTP 404, 410 o falla la sonda, se atenúa al 55% de opacidad y se etiqueta con `• 🔴 [INACCESIBLE]`.
   - ⭐ **Flujo Recomendado por la Comunidad:** Servidores con alta reputación (score >= 75%) o seleccionados como sugeridos se destacan con thumbnail dorado y distintivo `• ⭐ [RECOMENDADO]`.

3. **Base de Datos Local y Sincronización en la Nube (`StreamReputationDatabase` y `StreamReputationSyncManager`):**
   - **Almacenamiento Local:** Tabla SQLite `stream_reputation` (`site_domain`, `stream_host`, `success_count`, `failure_count`, `score`, `last_updated`).
   - **Sincronización Silenciosa:** Se ejecuta en segundo plano (`Dispatchers.IO`) con intervalo de 4 horas, descargando los últimos eventos de telemetría de la comunidad desde Supabase sin consumir recursos excesivos.
   - **Reporte Asíncrono:** Toda reproducción exitosa o fallo comprobado se reporta de forma desatendida a Supabase, beneficiando a todos los usuarios de la aplicación.

4. **Preservación Total de Configuración y Tokens:**
   - Para todo flujo que se confirme positivo, se preservan sus cabeceras HTTP (`Referer`, `User-Agent`, cookies) y query tokens en el Historial (`PlayHistoryManager`) y Listas Guardadas (`PlaylistManager`), permitiendo su reproducción posterior y auto-renovación si el token caduca.

5. **Aislamiento y Regla de 300 Líneas (AGENTS.md):**
   - El subsistema de reputación reside de forma modular en `com.omnicast.sender.ui.browser.reputation`.
   - Todos los archivos modificados y creados se mantienen estrictamente por debajo de 300 líneas.
   - Queda intacto el canvas de WebRTC y la TV Box (`TvScreens.kt` y `WebRtcReceiverManager.kt`).
