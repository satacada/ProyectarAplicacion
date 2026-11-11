# Índice de Contratos y Símbolos - OmniCast Pro

Este documento enumera los contratos clave, puertos,
casos de uso y gestores principales del sistema OmniCast Pro,
junto con sus rutas relativas exactas en el repositorio del proyecto.

## 1. Puertos y Dominio Core
- [`IMediaStreamExtractorPort`](core/domain/src/main/kotlin/com/omnicast/core/domain/ports/IMediaStreamExtractorPort.kt)
  - Puerto de dominio para la extracción abstracta de flujos de medios.
- [`StreamCandidateValidator`](core/domain/src/main/kotlin/com/omnicast/core/domain/usecases/StreamCandidateValidator.kt)
  - Componente de dominio encargado de validar la integridad de candidatos.
- [`DetectMediaStreamUseCase`](core/domain/src/main/kotlin/com/omnicast/core/domain/usecases/DetectMediaStreamUseCase.kt)
  - Caso de uso central que coordina la detección y análisis de streams.

## 2. Componentes Sender (Emisor)
- [`WebRtcSenderManager`](app-sender/src/main/kotlin/com/omnicast/sender/webrtc/WebRtcSenderManager.kt)
  - Gestor encargado de inicializar y mantener las conexiones WebRTC salientes.
- [`NativeStreamClassifier`](app-sender/src/main/kotlin/com/omnicast/sender/nativeclassifier/NativeStreamClassifier.kt)
  - Clasificador nativo para identificar tipos de medios y códecs.

## 3. Componentes Receiver (Receptor / Android TV)
- [`WebRtcReceiverManager`](app-receiver/src/main/kotlin/com/omnicast/receiver/webrtc/WebRtcReceiverManager.kt)
  - Gestor de WebRTC en el receptor para aceptar canales y pistas multimedia.
- [`ExoPlayerTvManager`](app-receiver/src/main/kotlin/com/omnicast/receiver/directstream/ExoPlayerTvManager.kt)
  - Administrador de ExoPlayer adaptado para pantallas de TV y baja latencia.
- [`TvWebStreamScreen`](app-receiver/src/main/kotlin/com/omnicast/receiver/ui/components/TvWebStreamScreen.kt)
  - Pantalla de interfaz en Compose orientada a visualización de streams.
- [`PvrStreamRecorder`](app-receiver/src/main/kotlin/com/omnicast/receiver/directstream/PvrStreamRecorder.kt)
  - Módulo de grabación PVR para almacenar flujos en el receptor.

## 4. Notas de Arquitectura y Convenciones del Sistema
- Todos los componentes respetan los principios de Clean Architecture.
- Desacoplamiento estricto entre dominio, emisor y receptor.
- Mantenimiento continuo de documentación y contratos clave del sistema.
- Versionado y trazabilidad garantizados mediante bitácoras de desarrollo.
- Integración continua de pruebas unitarias y de integración.
- Optimización de rendimiento para dispositivos de tipo Android TV y móviles.
- Uso de corrutinas de Kotlin para operaciones asíncronas seguras y eficientes.
- Modularización robusta orientada a escalabilidad y mantenibilidad a largo plazo.
- Gestión avanzada de estados mediante flujos reactivos (Kotlin Flows).
- Aseguramiento de calidad en cada pull request y revisión de código.
