# 📚 REPOSITORIO DE APLICACIONES DE REFERENCIA (BENCHMARK MAPPING)

Este directorio documenta de forma permanente y exhaustiva la arquitectura interna, protocolos, trucos de implementación y estrategias extraídas de las aplicaciones líderes de transmisión y proyección en Android:

1. **Web Video Caster (Receiver & Sender):**
   - Dominio de captura de flujos multimedia en páginas web protegidas contra piratería/adware.
   - Aislamiento de popups y pestañas publicitarias sin perder la página de origen ni romper el reproductor del sitio.
   - Gestión de cookies, headers HTTP (`Referer`, `Origin`, `User-Agent`) y peticiones de rango.
   - Servidor proxy HTTP local integrado para eludir restricciones de CORS y DRM suave.
   - Receptor Leanback / Android TV basado en ExoPlayer / Media3 con control D-Pad y selección de pistas.

2. **AirScreen (AirPlay / Cast / Miracast / DLNA Receiver):**
   - Recepción y renderizado de Screen Mirroring (espejo de pantalla) de ultra-baja latencia.
   - Inicialización y configuración óptima de `MediaCodec` por hardware.
   - Gestión de buffers de fotogramas mínimos (1-frame jitter buffer) para evitar caídas y congelamiento de pantalla.
   - Pacing de fotogramas y adaptación dinámica ante fluctuaciones de la red Wi-Fi.

---

## 🗂️ ESTRUCTURA DEL MAPEO

| Componente de Referencia | Ubicación Decompilada Permanente | Documento de Análisis Técnico |
|---|---|---|
| **Web Video Caster Receiver** | `references/decompiled/web_video_caster_receiver/` | [WEB_VIDEO_CASTER_MAPPING.md](file:///d:/APLICACIONES/proyectarAplicacion/docs/references/WEB_VIDEO_CASTER_MAPPING.md) |
| **Web Video Caster Sender** | `references/decompiled/web_video_caster_sender/` | [WEB_VIDEO_CASTER_MAPPING.md](file:///d:/APLICACIONES/proyectarAplicacion/docs/references/WEB_VIDEO_CASTER_MAPPING.md) |
| **AirScreen** | `references/decompiled/airscreen/` | [AIRSCREEN_MAPPING.md](file:///d:/APLICACIONES/proyectarAplicacion/docs/references/AIRSCREEN_MAPPING.md) |
| **Síntesis OmniCast Pro** | `core/`, `app-sender/`, `app-receiver/` | [OMNICAST_SYNTHESIS.md](file:///d:/APLICACIONES/proyectarAplicacion/docs/references/OMNICAST_SYNTHESIS.md) |

---

## 🎯 OBJETIVO DE LA INTEGRACIÓN
Aplicar la **Nueva Teoría** (Clean Architecture, Clean-by-Design, archivos <= 200 líneas, WebRTC local P2P sin servidores en la nube, latencia < 80ms) asimilando todas las virtudes y soluciones probadas en batalla de estas tres aplicaciones de referencia.
