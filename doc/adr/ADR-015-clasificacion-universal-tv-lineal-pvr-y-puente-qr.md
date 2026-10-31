# ADR-015: Clasificación Universal de TV Lineal, Grabación Dual (PVR/Descarga) y Puente HTTP para Deep Link QR

## Estado
Aprobado (v3.5.55)

## Contexto y Causa Raíz
1. **Buffering Infinito en Canales de Televisión Lineal (Sky News):**
   Al sintonizar emisiones de televisión continua 24/7 (como Sky News), la URL del manifiesto maestro contenía descriptores de ruta como `linear` y `lin-` (`linear901-oo-hls0-prd-gtm.delivery.skycdp.com/.../prod-gb-lin-skynews-hls-25-web/master.m3u8`). Al no contener la subcadena en inglés `live`, el clasificador en C++ (`stream_classifier.cpp`) la categorizaba erróneamente como película/serie (`VOD_HLS_MP4`). Como consecuencia, el Sender inyectaba una posición de reanudación previa guardada en el historial local (ej. 14 minutos = 847,223 ms) y le ordenaba a ExoPlayer ejecutar `seekTo(847223L)`. Debido a que una emisión lineal en vivo solo retiene en la CDN una ventana deslizante (*sliding window*) de 20 a 30 segundos, ExoPlayer solicitaba segmentos inexistentes y entraba en un ciclo perpetuo de `BUFFERING`.
2. **Persistencia Indeseada de Notificación en Pantalla Apagada (AOD):**
   `CastMediaNotificationService` solo se detenía ante el evento explícito `isCasting == false`. Cuando el usuario apagaba la TV Box de golpe o dejaba la reproducción pausada por tiempo prolongado, el servicio en primer plano permanecía activo indefinidamente en el sistema operativo Android, drenando batería y manteniendo visible el icono multimedia en la pantalla de bloqueo y Always-On Display (AOD).
3. **Fallo de Apertura Directa en Escaneo de Códigos QR Físicos con la Cámara:**
   Al codificar el esquema personalizado `omnicast://` directamente en la imagen del código QR, los lectores de cámara nativos (Google Lens, Xiaomi HyperOS, Samsung OneUI) lo trataban como texto plano por directivas de seguridad del sistema operativo, requiriendo copiar y pegar en lugar de abrir la aplicación con un solo toque.
4. **Ocultamiento de Descargas en el Reproductor de la TV Box:**
   La descarga de películas se encontraba anidada dentro del diálogo de ajustes de la tuerca (⚙️), limitando la accesibilidad rápida para guardar contenidos directamente en el almacenamiento de la TV Box.

---

## Decisiones de Arquitectura

### 1. Clasificación Universal de TV Lineal y Política Estricta de Live-Edge
- **Clasificador C++ (`stream_classifier.cpp`) y Kotlin (`NativeStreamClassifier.kt`):**
  Se incorporan formalmente los términos del estándar de la industria: `"linear"`, `"lin-"`, `"envivo"` y `"directo"` para categorizar unívocamente el flujo como `MediaBranch.LIVE_HLS`.
- **Despacho Seguro en el Móvil (`SenderMainActivity.kt`):**
  Si el flujo pertenece a `LIVE_HLS` o `IPTV_STREAM`, el Sender anula incondicionalmente cualquier reloj previo del historial (`savedPos = 0L`), garantizando que la señalización entregue `startPositionMs = 0L`.
- **Intocabilidad del Receptor (`ExoPlayerTvManager.kt`):**
  La TV Box no requiere modificaciones en su lógica de reproducción: al recibir `startPositionMs = 0L`, ExoPlayer ejecuta de forma natural `seekToDefaultPosition()` enganchándose al borde en vivo en 0 ms.

### 2. Detección de Inactividad y Heartbeat Pasivo (Anti-Sockets Zombies)
- **Temporizador de Inactividad en Móvil (`CastMediaNotificationService.kt`):**
  Se incorpora un temporizador de inactividad de **3 minutos** (`handleIdleTimer`). Si el video entra en pausa o no hay reproducción activa durante 3 minutos, el servicio destruye la sesión y retira la notificación del AOD.
- **Heartbeat Pasivo Basado en Telemetría:**
  El Sender monitoriza el flujo regular de telemetría de la TV Box (que llega cada 1 a 2 segundos). Si transcurren más de 25 segundos sin recibir ninguna señal de vida de la TV Box, el socket se considera muerto, forzando la liberación de recursos y desmontando los controles de casteo.

### 3. Puente HTTP Local para Deep Linking por QR (`:8087/open`)
- **Compatibilidad 100% con Cualquier Cámara de Fotos:**
  El código QR generado por la TV Box codifica una dirección web HTTP local:
  `http://<IP_TV_BOX>:8087/open?url=<ENCODED_STREAM_URL>&title=<ENCODED_TITLE>`
- **Redirección Invisible Inmediata:**
  Al apuntar la cámara física del celular, el sistema reconoce un enlace web `http://` estándar y muestra la burbuja de toque. Al pulsarlo, el servidor HTTP local de la TV Box responde una micro-página HTML que ejecuta en 50ms:
  `window.location.href = "omnicast://cast?url=..."`
  Android intercepta el esquema `omnicast://` mediante el `intent-filter` de `SenderMainActivity` y abre Castera directamente con el video listo para reproducir.

### 4. Modelo Dual de Grabación Multimedia en la TV Box
Se unifica el almacenamiento en disco en la TV Box en dos modalidades claras:
1. **Grabación en Tiempo Real (PVR / 1x):**
   Captura continua de fragmentos `.ts` en tiempo real para emisiones en vivo o lo que el usuario está viendo en pantalla, guardándose progresivamente sin recodificación de CPU.
2. **Descarga Completa en Segundo Plano (VOD):**
   Descarga acelerada de archivos completos para películas y series mediante `TvDownloadManager` hacia la carpeta `Descargas/Pelis_descargas`.
3. **Acceso Directo en el Reproductor:**
   Se traslada el botón de grabación/descarga fuera del menú de la tuerca ⚙️ y se sitúa como botón prominente de primer nivel en la barra de controles (`TvDirectStreamScreen.kt` y `RemoteExpandedControls.kt`).

---

## Consecuencias
- **Estabilidad Total en TV Lineal:** Sky News y transmisiones internacionales 24/7 reproducen inmediatamente sin buffering infinito.
- **Ahorro de Batería:** El icono en pantalla apagada (AOD) se autodestruye al terminar o pausar la transmisión.
- **Experiencia de Usuario Fluida con QR:** Cualquier celular puede escanear la pantalla del televisor y abrir Castera en un solo toque sin pasos manuales.
- **Ergonomía Multimedia:** El usuario puede grabar canales en vivo o descargar películas con un botón directo en el reproductor.
