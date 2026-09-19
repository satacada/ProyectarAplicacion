# 📌 HU-001: Galería de Fotos Continua, Selector Universal y Retorno con Botón Atrás

- **Estado:** 🟢 Aprobado / En Producción (v3.28)
- **Módulo Principal:** `:app-sender` (`GalleryPickerActivity`, `PhotoCastViewerActivity`, `PhotoViewerContent`)
- **Última Actualización:** 2026-09-19 (v3.28 - Retorno a Galería con botón Atrás)

---

## 1. Descripción de la Historia
Como usuario de OmniCast,  
Quiero abrir el selector de fotos del sistema para elegir cualquier aplicación (Galería nativa, Google Fotos, Explorador de archivos), proyectar una foto a pantalla completa en la TV y poder deslizar con el dedo (swipe) para ver más fotos de forma sincronizada,  
Para poder compartir mis fotos en la TV de manera fluida y que, al presionar el botón Atrás propio del celular, regrese a la galería donde estaba en lugar de cerrarse la aplicación.

---

## 2. Puntos de Entrada
1. **Asistente Flotante:** Botón *"🖼️ Enviar Foto con Zoom Táctil"*.
2. **Pestaña de Inicio Móvil:** Pestaña *"🖼️ Galería"* -> *"Seleccionar Foto / Video"*.

---

## 3. Criterios de Aceptación Inamovibles

### CA-01: Selector Universal del Sistema
- Debe abrir el selector nativo del sistema operativo mediante `ActivityResultContracts.GetContent("*/*")` con fallback a `image/*`.
- Debe permitir al usuario seleccionar libremente entre **Galería nativa de la marca**, **Google Fotos**, **Explorador de Archivos**, **Descargas** u otras apps compatibles, sin pantallas intermedias restrictivas.

### CA-02: Visualización en Pantalla Completa y Proyección Instantánea
- Al tocar una foto en la galería elegida, se abre en pantalla completa en el celular dentro de `PhotoCastViewerActivity`.
- Se transmite inmediatamente a la TV Box por WebSocket en Base64 JPEG HD optimizado (~15 ms de latencia) mediante `SignalingMessage.ShowImage`.

### CA-03: Pase Continuo (Swipe Táctil Adelante / Atrás)
- Estando en el visor en el móvil, el usuario desliza con el dedo (swipe izquierda / derecha) para avanzar o retroceder fotos.
- La foto visualizada en la pantalla del móvil **se refleja automáticamente y de forma sincronizada en la TV**.
- **Prohibición Arquitectónica:** No se permite el patrón de "seleccionar una foto, salir, enviar y repetir". El visor permanece abierto y la colección local completa es navegable.

### CA-04: Navegación y Retorno con el Botón Atrás Propio del Celular
- Si el usuario está viendo una foto en pantalla completa y presiona el **botón Atrás del celular** (físico o gestual):
  - **NO DEBE cerrarse la aplicación ni mostrar directamente la pantalla principal de OmniCast**.
  - Debe **regresar a la aplicación de Galería / Selector** desde donde tomó la foto para seguir explorando más fotos o álbumes.
- Solo si el usuario presiona el botón Atrás estando dentro de la Galería/Selector sin elegir ninguna foto (`uri == null`), se regresa a la interfaz de OmniCast.
- Si el usuario presiona el botón rojo **"Cerrar"** en la barra superior del visor, se finaliza la sesión de fotos y regresa directamente a OmniCast.

---

## 4. Historial de Ajustes
- **v3.25:** Implementada proyección rápida de fotos en Base64 JPEG por WebSocket y zoom táctil sincronizado a TV.
- **v3.28:**
  - Restaurado el selector universal nativo (`*/*`) ante rechazo de chooser restrictivo `ACTION_PICK`.
  - Implementada carga de galería completa (`loadFullGallery`) para swipe continuo sin salir del visor.
  - Implementado `viewerLauncher` con `RESULT_BACK_TO_GALLERY` para que el botón Atrás regrese a la galería/selector y el botón Cerrar vuelva a OmniCast.
