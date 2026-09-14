# 📺 OmniCast Pro — Proyección Universal de Móvil a TV Box

> **Solución Definitiva de Proyección de Pantalla y Audio Digital en Tiempo Real**  
> Proyecta **cualquier aplicación** de tu teléfono hacia tu TV Box (Android TV / AOSP / Fire TV), tengan o no soporte de Google Cast, con latencia ultra-baja (<80ms) y audio digital nativo.

---

## 🌟 ¿Por qué OmniCast Pro?

En muchas TV Boxes y aplicaciones móviles, el botón de "Cast" (Chromecast) no existe o no funciona debido a:
1. **Falta de certificación Google Cast en la TV Box** (típico en TV Boxes genéricas AOSP, Rockchip, Amlogic, Allwinner o Fire TV).
2. **Aplicaciones móviles sin soporte de casteo** (redes sociales como TikTok e Instagram, emuladores de juegos, navegadores, apps corporativas, reproductores IPTV o galerías privadas).
3. **Mala calidad en apps de duplicación gratuitas** (llenas de anuncios, retrasos de más de 1 segundo, sobrecalentamiento y audio capturado por el micrófono en lugar del sistema).

**OmniCast Pro** resuelve este problema de raíz capturando la pantalla a nivel de sistema operativo (`MediaProjection`) y transmitiendo el audio digital interno (`AudioPlaybackCapture`) directamente a la TV Box mediante **WebRTC P2P acelerado por hardware (MediaCodec)** sobre tu red Wi-Fi local.

---

## 📑 Documentación del Proyecto

El proyecto está diseñado bajo rigurosos estándares de ingeniería de software inspirados en `boot-ventas-saas`, `marketplace-saas` y `app-ofertas`:

1. 🤖 **[Directrices de Agentes de IA (`AGENTS.md`)](file:///d:/APLICACIONES/proyectarAplicacion/AGENTS.md):**  
   Roles de agentes de IA, matriz de habilidades técnicas, reglas mandatorias de desarrollo y metodología *Clean-by-Design*.

2. 📡 **[Documento Maestro de Ingeniería (`docs/PROYECTO_INGENIERIA_PROYECCION.md`)](file:///d:/APLICACIONES/proyectarAplicacion/docs/PROYECTO_INGENIERIA_PROYECCION.md):**  
   Diagnóstico profundo del problema, comparativa técnica exhaustiva, Clean Architecture y secuencia de comunicación WebRTC P2P.

3. ⚙️ **[Arquitectura Técnica Detallada (`docs/ARQUITECTURA_TECNICA.md`)](file:///d:/APLICACIONES/proyectarAplicacion/docs/ARQUITECTURA_TECNICA.md):**  
   Pipeline de hardware `MediaCodec`, captura de audio digital estéreo a 48kHz, señalización mDNS/WebSocket local y perfiles de calidad.

4. 📅 **[Plan Maestro de Fases y Actividades (`docs/PLAN_DE_FASES_Y_ACTIVIDADES.md`)](file:///d:/APLICACIONES/proyectarAplicacion/docs/PLAN_DE_FASES_Y_ACTIVIDADES.md):**  
   Cronograma de 7 fases secuenciales con actividades paso a paso, entregables tangibles y criterios de aceptación.

---

## 🏗️ Resumen de la Arquitectura Tecnológica

| Componente | Tecnología Seleccionada | Justificación |
|---|---|---|
| **Captura de Pantalla** | Android `MediaProjection` API + `VirtualDisplay` | Permite proyectar cualquier aplicación sin depender de soporte Cast. |
| **Captura de Audio** | `AudioPlaybackCaptureConfiguration` (Android 10+) | Sonido digital puro estéreo a 48kHz sin ruidos ambientales de micrófono. |
| **Codificación de Video** | Hardware `MediaCodec` (H.264 / HEVC) | Procesamiento directo en la GPU del teléfono; latencia < 10ms y bajo consumo de batería. |
| **Protocolo de Transporte** | WebRTC P2P Local (RTP / UDP) | Latencia ultra-baja (<80ms), sincronización A/V labial y adaptación dinámica a la red Wi-Fi. |
| **Descubrimiento de Red** | mDNS / Bonjour / NSD (`_omnicast._tcp`) | Cero configuración: detección automática de la TV Box sin escribir direcciones IP. |
| **Receptor TV Box** | Android TV Leanback / Compose for TV | Interfaz nativa optimizada para control remoto D-Pad con decodificación directa en `SurfaceView`. |
| **Servidor de Señalización** | WebSocket Local Embebido (Ktor / Netty) | 100% Offline: funciona sin internet, todo el tráfico permanece en la red local de la casa. |

---

## 🚀 Inicio Rápido y Roadmap

Consulta el plan detallado en [docs/PLAN_DE_FASES_Y_ACTIVIDADES.md](file:///d:/APLICACIONES/proyectarAplicacion/docs/PLAN_DE_FASES_Y_ACTIVIDADES.md) para conocer las etapas de ejecución desde la Fase 1 (Cimientos y Arquitectura) hasta la Fase 7 (Benchmarking de Latencia y Generación de APKs).
