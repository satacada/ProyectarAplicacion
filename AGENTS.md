# 🚨 MANDATORY AI AGENT GUIDELINES & STANDARDS (OmniCast Pro)

Este proyecto rige el desarrollo de **OmniCast Pro** (Sistema de Proyección y Transmisión Universal de Pantalla y Audio desde dispositivos móviles hacia Android TV / TV Box sin depender de compatibilidad nativa de Google Cast).

Todos los agentes de Inteligencia Artificial (Antigravity, Cursor, Claude Code, GitHub Copilot) que colaboren en este repositorio DEBEN cumplir estrictamente las directrices aquí especificadas, alineadas con los estándares de ingeniería de software de alta calidad (Clean Architecture, Clean-by-Design y Modularidad Quirúrgica inspirados en `boot-ventas-saas`, `marketplace-saas` y `app-ofertas`).

---

## 👥 ROLES DE AGENTES Y MATRIZ DE HABILIDADES

| Rol de Agente | Especialidad Técnica | Responsabilidades Principales | Skills Requeridas |
|---|---|---|---|
| **🏛️ System Architect Agent** | Clean Architecture, P2P Protocols, Local Discovery | Diseñar contratos de interfaz, topología de red P2P (mDNS/WebRTC), y gobernanza de dependencias. | `system-design`, `clean-architecture` |
| **📱 Android Core Specialist** | MediaProjection API, AudioPlaybackCapture, Services | Captura de pantalla a nivel de sistema operativo, captura de audio interno, Foreground Services y permisos. | `android-cli`, `media-projection-api` |
| **⚡ Media & Streaming Engineer** | MediaCodec (H.264/H.265/HEVC), WebRTC, Bitrate Control | Codificación/decodificación por hardware, sincronización A/V de ultra-baja latencia (<80ms), control adaptativo de tasa de bits. | `multimedia-encoding`, `webrtc-specialist` |
| **📺 TV Box & Leanback UX Agent** | Android TV, D-Pad Navigation, Leanback / Compose TV | Desarrollo de la aplicación receptora en TV Box, manejo de control remoto D-Pad, renderizado sobre SurfaceView nativo. | `generative_ui`, `tv-ux-guidelines` |
| **🧪 QA & Latency Benchmarking** | Profiling, Network Jitter, Battery & Thermal Analysis | Medición de framerate (60fps sostenidos), latencia de extremo a extremo, pérdida de paquetes en Wi-Fi 2.4/5GHz. | `testing-benchmarking` |

---

## 🎯 REGLAS MANDATORIAS DE DESARROLLO

1. **Clean-by-Design & Modularidad Quirúrgica:**
   - Prohibido código monolítico o archivos spaghetti (> 200 líneas sin modularizar).
   - Toda lógica de captura, codificación, señalización y renderizado debe estar aislada detrás de interfaces puras (Ports & Adapters).
2. **Latencia Crítica (Ultra-Low Latency Standard):**
   - El objetivo de latencia es **< 80 milisegundos**.
   - Queda prohibido el uso de protocolos pesados con buffering de streaming masivo (como HLS o DASH con chunks de varios segundos).
   - Se debe utilizar **WebRTC P2P con aceleración por hardware (MediaCodec)** o **Socket TCP/UDP Raw H.264** con buffers de fotogramas mínimos (1 frame buffer).
3. **Cero Dependencia de la Nube (100% Local / Offline-First):**
   - La proyección debe funcionar al 100% en la red Wi-Fi local sin requerir internet, autenticación externa ni servidores de señalización en la nube.
   - El descubrimiento de la TV Box debe realizarse mediante **mDNS / NSD (Network Service Discovery)** local.
4. **Independencia del Cast Nativo:**
   - La solución debe proyectar **cualquier aplicación** (juegos, TikTok, navegadores, apps educativas o de streaming sin cast) capturando el buffer de visualización de Android vía `MediaProjection` y el audio interno con `AudioPlaybackCapture`.
5. **Manejo de Errores y Cero Caídas (Fault Tolerance):**
   - Si la red Wi-Fi sufre microcortes o congestión temporal, el sistema debe degradar dinámicamente la resolución/bitrate antes que congelar la pantalla.
   - Reconexión automática transparente en menos de 1 segundo.

---

## 🛠️ METODOLOGÍA: INCREMENTAL MODULAR PROTOTYPING

Todos los agentes deben seguir este ciclo en cada iteración:
1. **Definición de Interfaz y Contrato:** Definir el puerto (e.g. `IScreenCapturer`, `IVideoEncoder`, `IPeerConnectionTransport`, `ITvRenderer`).
2. **Implementación de Adaptador:** Desarrollar el adaptador específico de plataforma garantizando compilación sin errores.
3. **Prueba Unitaria y Profiling:** Medir uso de CPU/GPU y consumo de memoria antes de dar por completado un entregable.
