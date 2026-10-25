# 📚 ÍNDICE DE HISTORIAS DE USUARIO (HU) — OmniCast Pro

Este directorio almacena de forma modular, independiente y versionada cada **Historia de Usuario (HU)** del proyecto.
Cada vez que se solicite un cambio, ajuste o nueva funcionalidad, se actualiza o crea su archivo correspondiente.

---

## 📌 Historias de Usuario Activas e Inamovibles

| ID | Archivo | Título | Estado | Versión de Implementación |
|---|---|---|---|---|
| **HU-001** | [`HU-001-galeria-fotos-continua.md`](./HU-001-galeria-fotos-continua.md) | Galería de Fotos Continua, Selector Universal y Retorno con Botón Atrás | 🟢 Aprobado / En Producción | v3.28 |
| **HU-002** | [`HU-002-standby-limpio-tv.md`](./HU-002-standby-limpio-tv.md) | Interfaz Limpia y Standby en TV Receiver (Estilo Seekee TV) | 🟢 Aprobado / En Producción | v3.28 |
| **HU-003** | [`HU-003-menu-lateral-historial.md`](./HU-003-menu-lateral-historial.md) | Menú Lateral Móvil con Historial y Videos Guardados con Tiempo | 🟢 Aprobado / En Producción | v3.28 |
| **HU-004** | [`HU-004-deduplicacion-streams-web.md`](./HU-004-deduplicacion-streams-web.md) | Detección Limpia y Anti-Duplicados en Streams Web (Dailymotion, ok.ru) | 🟢 Aprobado / En Producción | v3.28 |
| **HU-005** | [`HU-005-extraccion-hexagonal-streams.md`](./HU-005-extraccion-hexagonal-streams.md) | Detección Hexagonal y Principio "Poner y Sacar" de Streams Multimedia | 🟢 Aprobado / En Producción | v3.30 |
| **HU-006** | [`HU-006-renovacion-enlaces-expirados-tv.md`](./HU-006-renovacion-enlaces-expirados-tv.md) | Auto-Renovación de Flujos Expirados en la TV Box | 🟢 Aprobado / En Producción | v3.32.55 |
| **HU-007** | [`HU-007-bloqueador-anuncios-autoaprendizaje-proactivo.md`](./HU-007-bloqueador-anuncios-autoaprendizaje-proactivo.md) | Bloqueador de Anuncios con Autoaprendizaje Proactivo y Resiliencia | 🟢 Aprobado / En Producción | v3.4.59 |
| **HU-008** | [`HU-008-enrutamiento-proxy-local.md`](./HU-008-enrutamiento-proxy-local.md) | Enrutamiento por Proxy HTTP Local para Flujos Protegidos | 🟢 Aprobado / En Producción | v3.32.65 |
| **HU-009** | [`HU-009-motor-nativo-cpp-captura-streams.md`](./HU-009-motor-nativo-cpp-captura-streams.md) | Motor Nativo C++20 de Captura de Streams y UI Pixel-Perfect | 🟢 Aprobado / En Producción | v3.32.67 |
| **HU-010** | [`HU-010-interfaz-minimalista-wvc-control-multimedia-salida-limpia.md`](./HU-010-interfaz-minimalista-wvc-control-multimedia-salida-limpia.md) | Interfaz Minimalista WVC, Control Dismissible y Zoom TV | 🟢 Aprobado / En Producción | v3.32.85 |
| **HU-011** | [`HU-011-autocompletado-inteligente-selecciones-web.md`](./HU-011-autocompletado-inteligente-selecciones-web.md) | Autocompletado y Replicación Inteligente de Selecciones Web | 🟢 Aprobado / En Producción | v3.32.100 |
| **HU-012** | [`HU-012-receptor-web-html5-resiliencia-cdn-tv.md`](./HU-012-receptor-web-html5-resiliencia-cdn-tv.md) | Receptor Web HTML5 (Chromium / Hls.js) y Resiliencia CDN | 🟢 Aprobado / En Producción | v3.4.57 |
| **HU-013** | [`HU-013-screen-mirroring-proyeccion-pantalla-completa.md`](./HU-013-screen-mirroring-proyeccion-pantalla-completa.md) | Screen Mirroring — Proyección de Pantalla Completa Vertical y Horizontal | 🟢 Aprobado / En Producción | v3.4.8 |
| **HU-014** | [`HU-014-inteligencia-colectiva-reputacion-streams.md`](./HU-014-inteligencia-colectiva-reputacion-streams.md) | Inteligencia Colectiva y Reputación Ponderada de Streams | 🟢 Aprobado / En Producción | v3.4.33 |
| **HU-015** | [`HU-015-analisis-previo-clasificacion-sitios-oficiales.md`](./HU-015-analisis-previo-clasificacion-sitios-oficiales.md) | Clasificación Dinámica Dual de Páginas y Pre-Análisis de Películas | 🟢 Aprobado / En Producción | v3.4.34 |
| **HU-016** | [`HU-016-estabilizador-visibilidad-busqueda-caret-react.md`](./HU-016-estabilizador-visibilidad-busqueda-caret-react.md) | Estabilizador Universal de Visibilidad de Búsqueda y Caret React | 🟢 Aprobado / En Producción | v3.4.39 |

---

## 🛠️ Reglas de Modificación
1. Ninguna refactorización o cambio en el código puede romper los **Criterios de Aceptación** definidos en estos archivos.
2. Cada ajuste solicitado por el usuario sobre un flujo existente debe quedar registrado en su respectivo archivo `.md` con fecha y versión.
3. Si una nueva función no pertenece a las historias existentes, se crea una nueva: `HU-005-...md`.
