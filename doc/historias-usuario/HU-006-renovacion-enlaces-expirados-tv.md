# HU-006: Auto-Renovación de Flujos Expirados en la TV Box (Receiver)

## Descripción
Como usuario de Android TV con la aplicación OmniCast Receiver instalada de forma independiente, quiero que las películas y videos guardados que hayan caducado en su servidor web (error HTTP 403 / 404) se auto-renueven desde la página de origen conservando el minuto exacto donde me quedé, para poder continuar viéndolas sin tener que buscar manualmente la película de nuevo.

---

## Criterios de Aceptación

### CA-01: Detección y No-Bloqueo de Interfaz
- Si `ExoPlayer` lanza una excepción HTTP 403 / 404 / 410 al reproducir un elemento guardado:
  - La aplicación NO debe cerrarse ni quedar en pantalla negra colgada.
  - Debe mostrar un estado visible: *"Renovando enlace de video..."*.

### CA-02: Auto-Extracción Autónoma en la TV (Nivel 1)
- La APK de la TV debe intentar refrescar el flujo mediante su propio extractor HTTP / WebView temporal efímero cargando la `pageUrl` guardada.
- Una vez detectado el nuevo `.m3u8` o `.mp4`:
  - Se actualiza la `url` en la lista guardada de la TV.
  - Se mantiene inalterado `lastPositionMs`.
  - La reproducción arranca de inmediato en la posición guardada.

### CA-03: Asistencia de Red y Código QR de Respaldo (Niveles 2 y 3)
- Si el sitio web cuenta con protección anti-bot infranqueable de manera automática:
  - Si el emisor móvil está conectado a la red local, solicitar la renovación por socket.
  - En caso contrario, mostrar un diálogo con **Código QR** hacia la `pageUrl` para resolver y castear desde el móvil en 1 toque.

### CA-04: Opción Manual en Menú de la TV
- En el menú contextual de cada elemento de la lista en la TV (donde están Compartir y Eliminar), debe figurar la opción: `🔄 Renovar enlace`.
