# 📋 HISTORIAS DE USUARIO INAMOVIBLES — OmniCast Pro

Este documento es el registro maestro y permanente de todas las Historias de Usuario (HU) y contratos de experiencia de usuario solicitados. Ningún cambio de código o refactorización futura puede violar o revertir estos comportamientos aprobados.

---

## 📌 HU-01: Galería de Fotos Continua, Selector Universal y Retorno a Galería
- **Objetivo:** Proyectar fotos y álbumes locales en la TV de forma instantánea, fluida y sin fricción.
- **Acceso:** Desde el botón *"🖼️ Enviar Foto con Zoom Táctil"* en el menú flotante o pestaña *"🖼️ Galería"* en la pantalla principal.

### Criterios de Aceptación:
1. **Selector Universal del Sistema (Sin Restricciones):**
   - Al pulsar proyectar foto, se abre el selector nativo del sistema operativo (`ActivityResultContracts.GetContent("*/*")`).
   - El usuario debe poder elegir libremente entre **Galería nativa de la marca (Samsung, Xiaomi, etc.)**, **Google Fotos**, **Exploradores de archivos** o **Descargas**.
2. **Visualización en Pantalla Completa y Proyección Instantánea:**
   - Al tocar la foto en la galería elegida, se abre en pantalla completa en el móvil y se transmite a la TV por WebSocket en Base64 JPEG HD en menos de ~20 ms.
3. **Pase Continuo (Swipe Táctil Adelante / Atrás):**
   - Estando en el visor en el móvil, deslizar con el dedo a izquierda o derecha avanza o retrocede entre las fotos del dispositivo.
   - Cada foto visualizada en la pantalla del móvil **se refleja automáticamente y de forma sincronizada en la TV**.
   - **Prohibición Arquitectónica:** No se permite el flujo de "seleccionar una foto, salir, enviar y tener que volver a abrir todo". El visor permanece activo permitiendo ver toda la galería.
4. **Comportamiento del Botón Atrás (Hardware / Gestual):**
   - Si el usuario está viendo una foto en pantalla completa y presiona el **botón Atrás del celular**, **NO DEBE cerrarse todo ni regresar directo a la interfaz de OmniCast**.
   - El botón Atrás debe **devolver al usuario a la aplicación de Galería / Selector** desde donde tomó la foto, para que pueda seguir explorando álbumes o eligiendo más fotos.
   - Solo si el usuario presiona Atrás estando dentro del selector/galería sin elegir foto, regresa a la interfaz de OmniCast.

---

## 📌 HU-02: Interfaz Limpia y Standby en TV Receiver (Estilo Seekee TV)
- **Objetivo:** Proporcionar una pantalla de espera moderna, limpia y organizada en Android TV Box cuando no hay video activo.

### Criterios de Aceptación:
1. **Barra Superior (Top Bar):**
   - Logotipo `OmniCast TV`, navegación por pestañas (`Recientes`, `Todo`, `Favoritos`), nombre de usuario / badge de versión actual (ej: `v3.28`).
2. **Cuerpo Central:**
   - Contador de elementos (`X elementos`).
   - Filas limpias de carpetas con icono amber `📁` (ej: `FromWebSite`, `⭐ Favoritos`), fecha/hora y navegación con mando a distancia (D-Pad).
   - Botón píldora central de ayuda.
3. **Dock Inferior de Conexión:**
   - Tarjeta anclada en la parte inferior mostrando claramente:
     - Estado de conexión: `🟢 Listo para conectar`.
     - Dirección IP y puerto local: `$ipAddress:8088`.
     - Instrucciones de conexión para el usuario.

---

## 📌 HU-03: Menú Lateral Móvil con Historial y Videos Guardados con Tiempo
- **Objetivo:** Acceder al historial y videos guardados conociendo exactamente el minuto y segundo donde se quedó la reproducción.
- **Acceso:** Botón hamburguesa `☰` en la esquina superior izquierda del móvil.

### Criterios de Aceptación:
1. **Dos Pestañas Principales:** `🕒 Historial` y `⭐ Guardadas`.
2. **Indicador de Posición Exacta:** Cada elemento muestra `▶ Se quedó en: mm:ss / mm:ss` actualizado en tiempo real con la telemetría enviada por la TV Box.
3. **Acciones Rápidas por Tarjeta:**
   - `▶ TV`: Proyecta directamente el video a la TV Box en el segundo donde se quedó.
   - `🌐 Web`: Abre la página web original en el navegador integrado.
   - `✕`: Elimina el elemento del historial.
4. **Filtro Alfabético y Búsqueda:** Lista ordenada con filtro por texto.

---

## 📌 HU-04: Detección Limpia y Anti-Duplicados en Streams Web (Dailymotion, ok.ru, etc.)
- **Objetivo:** Evitar tarjetas duplicadas con nombres genéricos cuando los sitios web cargan múltiples variantes CDN o resoluciones.

### Criterios de Aceptación:
1. **Captura del Título Real:** El título del video debe capturarse del DOM de la página (`document.title` o elemento de video) antes de registrar el stream, evitando nombres vacíos o repetidos como "Dailymotion".
2. **Deduplicación Automática:** Si una misma URL o video genera múltiples variantes CDN (1080p, 720p, chunklist), se agrupa en un único elemento en el historial y en las listas tanto de la TV como del móvil.
