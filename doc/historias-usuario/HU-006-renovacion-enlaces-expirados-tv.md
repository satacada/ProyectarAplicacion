# HU-006: Auto-Renovación de Flujos Expirados en la TV Box (Receiver)

## Descripción
Como usuario de Android TV con la aplicación OmniCast Receiver instalada de forma independiente, quiero que las películas y videos guardados que hayan caducado en su servidor web (error HTTP 403 / 404) se auto-renueven desde la página de origen conservando el minuto exacto donde me quedé, para poder continuar viéndolas sin tener que buscar manualmente la película de nuevo.

---

## Criterios de Aceptación

### CA-01: Detección y No-Bloqueo de Interfaz
- Si `ExoPlayer` lanza una excepción HTTP 403 / 404 / 410 al reproducir un elemento guardado:
  - La aplicación NO debe cerrarse ni quedar en pantalla negra colgada.
  - Debe mostrar un estado visible: *"Renovando enlace de video..."*.

### CA-02: Asistencia por Código QR y Criterio Humano
- Ante un error 403 / 404, la TV despliega un diálogo con **Código QR** dinámico apuntando a la `pageUrl` original de la película.
- El usuario escanea el QR con su móvil, abriendo la página en OmniCast para seleccionar con criterio humano la opción de audio/servidor correcta, evitando al 100% anuncios o videos basura.
- Al pulsar *"Transmitir a TV"*, el flujo se reenvía a la TV con el token activo.

### CA-03: Sustitución Automática del Enlace en TV (Aprovechamiento de TTL)
- Al recibir el nuevo flujo, la TV actualiza automáticamente el registro en la lista guardada (`TvPlaylistsManager`), sustituyendo la URL vieja caducada por la nueva.
- Se mantiene inalterado el progreso exacto (`lastPositionMs`) y el título.
- El usuario puede seguir viendo la película de inmediato.
- Si apaga la TV o sale y vuelve durante la vigencia del nuevo token (6-12 horas), la película se reproduce directamente en la TV sin pedir QR.

### CA-04: Opción Manual en Menú de la TV
- En el menú contextual de cada elemento de la lista en la TV (donde están Compartir y Eliminar), debe figurar la opción: `🔄 Renovar enlace`.
