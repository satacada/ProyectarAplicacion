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

### CA-05: Pila de Navegación Inversa LIFO por Carpetas con el Botón Atrás del Celular
El retroceso debe respetar y recrear sin saltar la secuencia exacta de carpetas visitadas:
1. **Contexto Estricto por Carpeta:** Cada foto y cada video pertenece al contexto exclusivo de su carpeta (`folderPath`). Al abrir una foto o video en la `Carpeta 5`, el visor navega únicamente dentro de las fotos o videos de la `Carpeta 5`.
2. **Recreación Íntegra de la Ruta Inversa (LIFO):** Si para llegar a la `Carpeta 5` el usuario pasó por la secuencia `Carpeta 1 -> Carpeta 3 -> Carpeta 2 -> Carpeta 4 -> Carpeta 5` (ya sea por subcarpetas anidadas o cambiando entre carpetas), al pulsar el botón Atrás desde la foto o video:
   - Primer Atrás: Regresa de la foto/video a la **`Carpeta 5`** en su posición exacta de scroll.
   - Siguientes pulsaciones de Atrás: Retrocede estrictamente paso a paso **`Carpeta 4 -> Carpeta 2 -> Carpeta 3 -> Carpeta 1 -> Carpetas (Raíz) -> OmniCast`** sin saltarse ningún nivel intermedio.
3. **Reconstrucción Automática de Rutas Anidadas:** Si el usuario selecciona directamente un álbum anidado o un archivo desde el selector del sistema (`📁 Sistema`), `GallerySessionHolder` reconstruye automáticamente todos los niveles ancestros (`1 -> 3 -> 2 -> 4 -> 5`) en la pila de rutas para garantizar el retroceso paso a paso.

### CA-06: Memoria de Posición de Scroll en Cada Carpeta de la Ruta al Dar Atrás
- Cada nodo (`GalleryNavNode`) de la pila `routeStack` conserva su `firstVisibleIndex` y `firstVisibleOffset`, restaurando la posición exacta de scroll al retroceder por cada carpeta previa.

### CA-07: Liberación Automática de Memoria en la TV Box
- Ninguna foto ni video debe ocupar espacio en el disco/almacenamiento interno de la TV Box (0 Bytes en flash).
- Al finalizar la proyección, el emisor envía `SignalingMessage.PlayControl("STOP")` a la TV Box.
- La TV Box transiciona a `ReceiverUiState.Standby`, desmontando el visor de imágenes y liberando inmediatamente de la memoria RAM cualquier `Bitmap` cargado.

---

## 4. Historial de Ajustes
- **v3.25:** Proyección rápida Base64 JPEG y zoom táctil sincronizado a TV.
- **v3.28:** Selector universal, pase continuo y memoria de scroll.
- **v3.5.33:** Explorador nativo de carpetas (`GalleryExplorerScreen`, `GallerySessionHolder`) con pila de rutas LIFO estricta (`routeStack`), reconstrucción automática de ancestros para rutas anidadas y aislamiento de contexto de fotos y videos por carpeta.
