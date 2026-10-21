# HU-011: Autocompletado y Replicación Inteligente de Selecciones Web

**Épica:** Experiencia de Usuario en Navegación y Captura de Medios  
**Versión de Introducción:** v3.32.97  
**Estado:** Implementado / Validado  

---

## 1. Descripción de la Historia

**Como** usuario del navegador web integrado en OmniCast,  
**Quiero** que la aplicación recuerde automáticamente las selecciones que realizo en cada sitio web (como casillas de verificación, opciones de radio, selectores de idioma o botones de servidores de reproducción favoritos),  
**Para** no tener que volver a marcar manualmente las mismas casillas ni seleccionar repetidamente el mismo idioma o servidor de video cada vez que visito o recargo páginas del mismo sitio.

---

## 2. Criterios de Aceptación (Contrato Inamovible)

1. **Captura Reactiva Pasiva (Cero Lag):**
   - La memorización de decisiones se dispara únicamente ante interacciones genuinas del usuario (`isTrusted === true`) en eventos `change` y `click`.
   - Queda estrictamente prohibido el uso de `MutationObserver` masivos o bucles continuos que saturen el hilo de JavaScript (Regla 6 de AGENTS.md).

2. **Ámbito por Dominio y Persistencia Nativa:**
   - Las selecciones se indexan por dominio (`host`) y clave determinista del elemento (`#id`, selector por atributos de servidor/idioma o texto distintivo).
   - El almacenamiento reside de forma persistente y segura en Android (`SharedPreferences` a través de `SmartSelectionBridge`), inmune a limpiezas de almacenamiento web temporal del sitio.

3. **Replicación Automática Silenciosa:**
   - Al cargar cualquier página perteneciente al mismo dominio, se consultan las reglas almacenadas y se aplican de forma inmediata y en diferido suave (600ms y 1600ms) para elementos renderizados dinámicamente.
   - En casillas (`checkbox`), radios y selects, se sincroniza el estado y se despacha el evento `change`.
   - En botones o pestañas de servidor/opción (e.g. "Latino", "DropCDN"), si el elemento no está activo, se dispara un click único controlado con marca `data-omni-applied` para prevenir bucles de clicks.

4. **Aislamiento Estricto de Pilares:**
   - La funcionalidad reside exclusivamente en el navegador web del emisor (`:app-sender`).
   - No invade ni modifica en modo alguno la galería de fotos (Pilar 2), el reproductor/casteo (Pilar 3) ni la transmisión WebRTC de pantalla completa.
