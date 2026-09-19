# 📌 HU-001: Galería de Fotos y Videos Continua, Selector Universal y Retorno Inverso con Botón Atrás

- **Estado:** 🟢 Aprobado / En Producción (v3.28)
- **Módulo Principal:** `:app-sender` (`GalleryPickerActivity`, `PhotoCastViewerActivity`, `PhotoViewerContent`)
- **Última Actualización:** 2026-09-19 (v3.28 - Flujo inverso estricto y persistencia de videos en Galería)

---

## 1. Descripción de la Historia
Como usuario de OmniCast,  
Quiero abrir el selector de fotos y videos del sistema para elegir cualquier aplicación (Galería nativa, Google Fotos, Explorador de archivos), proyectar fotos o reproducir videos en la TV Box sin que la aplicación se cierre forzosamente,  
Y que al pulsar el botón Atrás propio del celular, el sistema respete el orden inverso exacto (Visor/Video -> Galería -> Explorador -> OmniCast) sin saltarse ningún paso.

---

## 2. Puntos de Entrada
1. **Asistente Flotante:** Botón *"🖼️ Enviar Foto con Zoom Táctil"*.
2. **Pestaña de Inicio Móvil:** Pestaña *"🖼️ Galería"* -> *"Seleccionar Foto / Video"*.

---

## 3. Criterios de Aceptación Inamovibles

### CA-01: Selector Universal del Sistema
- Debe abrir el selector nativo del sistema operativo mediante `ActivityResultContracts.GetContent("*/*")` con fallback a `image/*`.
- Debe permitir al usuario seleccionar libremente entre **Galería nativa de la marca**, **Google Fotos**, **Explorador de Archivos**, **Descargas** u otras apps compatibles.

### CA-02: Visualización en Pantalla Completa y Proyección Instantánea de Fotos
- Al tocar una foto en la galería elegida, se abre en pantalla completa en el celular dentro de `PhotoCastViewerActivity`.
- Se transmite inmediatamente a la TV Box por WebSocket en Base64 JPEG HD optimizado (~15 ms de latencia) mediante `SignalingMessage.ShowImage`.

### CA-03: Pase Continuo de Fotos (Swipe Táctil Adelante / Atrás)
- Estando en el visor en el móvil, el usuario desliza con el dedo (swipe izquierda / derecha) para avanzar o retroceder fotos.
- La foto visualizada en la pantalla del móvil **se refleja automáticamente y de forma sincronizada en la TV**.
- **Prohibición Arquitectónica:** No se permite el patrón de "seleccionar una foto, salir, enviar y repetir". El visor permanece abierto y la colección local completa es navegable.

### CA-04: Reproducción Continua de Videos Locales (Cero Cierres Abruptos)
- Al seleccionar un video en la Galería, se envía el flujo directo a la TV Box mediante `LocalImageServer` y `SenderMainActivity.sendDirectStreamStatic`.
- **PROHIBICIÓN ESTRICTA:** **NO DEBE cerrarse la aplicación ni retornar automáticamente a la pantalla de inicio de OmniCast**.
- El usuario debe permanecer en la Galería para poder seguir explorando, viendo o seleccionando más videos y fotos de forma ininterrumpida.

### CA-05: Pila de Navegación Inversa con el Botón Atrás del Celular
El retroceso debe respetar la jerarquía inversa de actividades:
1. **Atrás desde Foto en Pantalla Completa:** Regresa a la **Galería / Álbum de origen** (Paso 2).
2. **Atrás desde la Galería (sin seleccionar):** Regresa al **Explorador Nativo del Sistema** (Paso 1).
3. **Atrás desde el Explorador Nativo:** Regresa a **OmniCast** (Paso 0).
4. **Botón Rojo "Cerrar" en Visor:** Permite salir directamente a OmniCast en un solo toque.

---

## 4. Historial de Ajustes
- **v3.25:** Proyección rápida Base64 JPEG y zoom táctil sincronizado a TV.
- **v3.28:**
  - Restaurado el selector universal nativo (`*/*`).
  - Carga de galería completa (`loadFullGallery`) para swipe continuo sin salir del visor.
  - Corrección de cierre forzoso al enviar videos (el usuario permanece en la galería).
  - Implementación de la pila inversa LIFO de 3 niveles en `GalleryPickerActivity`.
