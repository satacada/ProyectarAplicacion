# AGENTS.md — GUÍA DE INSTRUCCIONES PARA IA (Screen Mirroring - proyectarAplicacion)

Este repositorio sigue el estándar abierto AGENTS.md para el desarrollo de la app nativa Android de Screen Mirroring.

---

## 1. NATURALEZA DEL PRODUCTO: SCREEN MIRRORING
- **Dominio Principal:** Transmisión de video/audio en tiempo real, baja latencia y gestión de sockets de red.
- **Tolerancia a Red:** Toda reconexión de socket o pérdida de paquetes debe manejarse de forma silenciosa sin cerrar la aplicación (Principio Fail Fast).
- **Telemetría en TV:** Ante fallos en la Android TV, analizar el log del streaming enviado por red local con timestamps ('HH:mm:ss.SSS').

---

## 2. ESTRATEGIA DE PRUEBAS
- **Unidades Nativas:** Probar codificación/decodificación y formateo de datos en ':core' con JUnit/Mockito.
- **Pruebas de Red:** Verificar manejo de excepciones de Timeout y desconexión en ':app-sender' y ':app-receiver'.
- **Ejecución del Oráculo:** Todo cambio debe validar la compilación y tests ejecutando:
  .\gradlew.bat clean build

---

## 3. BUENAS PRÁCTICAS Y ESTÁNDAR DE CÓDIGO
- **Límite de Tamaño:** Máximo 300 líneas por archivo. Separar estados, modelos y composables en archivos independientes.
- **Diseño de Código:** Aplicar 'Fail Fast' (evitar 'if-else' anidados). Sin 'Magic Numbers' o puertos de red 'hardcoded'.
- **Comentarios:** Solo explicar el 'POR QUÉ' de un parche o limitación técnica del hardware. Prohibido narrar lo que hace el código.
- **Catálogo de Versiones:** Declarar dependencias únicamente en 'gradle/libs.versions.toml'.

---

## 4. CONTROL DE VERSIONES (GIT)
- **Mensajes de Commit:** Usar Convención Estándar: 'feat(modulo): descripción', 'fix(modulo): descripción'.
- **Ámbito Acotado:** Cada tarea debe modificar únicamente los archivos necesarios sin refactorizaciones ajenas no solicitadas.

---

## 5. DOCUMENTACIÓN VIVA Y CIERRE
- Mantener 'doc/PROJECT_DASHBOARD.md' actualizado al finalizar cada sesión.
- Si se modifica la estructura del protocolo de red o streaming, registrar el cambio en 'doc/adr/ADR-xxx.md'.
- Toda funcionalidad aprobada por el usuario debe registrarse como contrato inamovible en 'doc/historias-usuario/HU-xxx.md'.
- **Versionado y Despliegue Obligatorio:** Toda nueva versión debe actualizar 'versionCode' y 'versionName' en los archivos 'build.gradle.kts', compilarse, copiarse a la carpeta 'releases/' (conservando siempre las versiones generadas de sender y receiver) e instalarse de inmediato en el dispositivo móvil vía ADB. En GitHub Releases únicamente debe publicarse y residir la versión final actual, eliminando automáticamente de GitHub las versiones previas para no saturar espacio remoto.

---

## 6. ARQUITECTURA HEXAGONAL Y PRINCIPIO "PONER Y SACAR" (LEY INVIOLABLE)
- **Cero Código Spaghetti:** Queda terminantemente PROHIBIDO encadenar 'if-else' o modificar métodos de análisis globales para soportar un sitio web específico.
- **Extractores Aislados (Plugins):** Todo soporte para una web o servicio específico debe implementarse en un archivo independiente como adaptador del puerto 'IMediaStreamExtractorPort'.
- **Principio "Poner y Sacar":** Agregar o retirar un extractor nunca debe modificar ni desestabilizar los extractores universales (HLS, MP4, DASH).
- **Aislamiento de los 4 Pilares:** 
  1. Bloqueador de Anuncios (C++ / Red).
  2. Proyección de Fotos (WebSocket liviano / reposo de video).
  3. Reproductor / Casteo (ExoPlayer y WebRTC con exclusión mutua estricta).
  4. Detección de Streams (Núcleo Hexagonal puro).
  Ningún pilar puede invadir, depender o alterar el ciclo de vida de los otros tres.
- **Prohibición en WebViews:** PROHIBIDO usar bucles 'MutationObserver' o 'querySelectorAll' masivos que saturen el hilo de JavaScript. Toda neutralización de trampas debe ser reactiva al evento del usuario.

---

## 7. LEYES TÉCNICAS INVIOLABLES DE HARDWARE (PROHIBICIÓN ESTRICTA DE REGRESIONES)
1. **Prohibido `changeCaptureFormat()` o `DisplayListener` en caliente durante Screen Mirroring:**
   El encoder por hardware H.264 (MediaCodec) y el `VirtualDisplay` de Android 14 no admiten redimensionamiento en caliente sin reinicializar el pipeline WebRTC. Llamar a `changeCaptureFormat` al rotar colapsa el capturador y desconecta la TV Box. La rotación de pantalla fluye nativamente dentro del `VirtualDisplay` sin alterar dimensiones negociadas en el SDP.
2. **Tolerancia a Desconexión Temporal ICE (`IceConnectionState.DISCONNECTED`):**
   `DISCONNECTED` es un estado transitorio de red; queda TERMINANTEMENTE PROHIBIDO cerrar la sesión WebRTC o expulsar la pantalla de TV ante este estado. Únicamente `FAILED` o `CLOSED` son causales de cierre.
3. **Intocabilidad de Foco en `BrowserWebView`:**
   PROHIBIDO asignar `setOnTouchListener` con `requestFocus()` al `WebView`. Chromium y el `InputMethodManager` de Android gestionan el foco y la posición del cursor de forma nativa; manipularlo en eventos táctiles reinicia la conexión IME en el índice 0 y genera escritura inversa o bloqueo del teclado virtual.
4. **Protección de Elementos de Entrada y Dominios Oficiales en `AdBlocker`:**
   El script de limpieza y neutralización de capas NUNCA debe ejecutarse en plataformas oficiales (`dailymotion.com`, `youtube.com`, buscadores) y NUNCA debe remover elementos que contengan formularios (`form`, `input`, `textarea`, `select`).
5. **Arquitectura Híbrida Dual y Resiliencia CDN con `TvWebStreamScreen` (HTML5/Chromium):**
   Queda terminantemente PROHIBIDO eliminar o forzar exclusivamente `ExoPlayer` para flujos protegidos por tokens efímeros, firmas criptográficas o CDNs anti-scrape (DropCDN, Fastream, etc.). Los sockets de red de Java en ExoPlayer son rechazados con `HTTP 403 Forbidden` por inspección TLS/Fingerprint de dichas CDNs. La TV Box DEBE mantener siempre activo el receptor web HTML5 nativo (`TvWebStreamScreen` con `Hls.js`) para flujos con tokens firmados (`hasSignedSecurityToken`) y como fallback reactivo automático ante cualquier error 403 en ExoPlayer (ADR-007, HU-012).


