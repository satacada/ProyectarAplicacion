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

---

## 🛠️ Reglas de Modificación
1. Ninguna refactorización o cambio en el código puede romper los **Criterios de Aceptación** definidos en estos archivos.
2. Cada ajuste solicitado por el usuario sobre un flujo existente debe quedar registrado en su respectivo archivo `.md` con fecha y versión.
3. Si una nueva función no pertenece a las historias existentes, se crea una nueva: `HU-005-...md`.
