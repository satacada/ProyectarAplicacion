# 📅 PLAN MAESTRO DE FASES Y ACTIVIDADES: OMNICAST PRO

> **Metodología:** Desarrollo Modular Incremental (*Clean-by-Design*)  
> **Duración Total Estimada:** 10 - 12 Semanas  
> **Estructura:** 7 Fases Técnicas Secuenciales  
> **Ecosistema:** Emisor Android (Mobile) + Receptor Android TV / AOSP (TV Box)  

---

## 📊 RESUMEN GENERAL DE FASES

| Fase | Nombre | Duración | Objetivo Principal | Estado |
|---|---|---|---|---|
| **Fase 1** | **Cimientos y Arquitectura Base** | 2 semanas | Estructura multi-módulo, interfaces Clean Architecture y mocks de simulación. | 🟡 Lista para inicio |
| **Fase 2** | **Captura Android OS y Hardware MediaCodec** | 2 semanas | Captura de pantalla y audio interno a nivel de SO con codificación H.264 por hardware. | ⏳ Pendiente |
| **Fase 3** | **Motor de Transporte WebRTC P2P Local** | 2.5 semanas | Señalización local mDNS + WebSocket y canal P2P RTP de ultra-baja latencia (<80ms). | ⏳ Pendiente |
| **Fase 4** | **Receptor TV Box (Leanback & Hardware Decode)** | 2 semanas | App de TV Box controlable por control remoto D-Pad con decodificación directa en SurfaceView. | ⏳ Pendiente |
| **Fase 5** | **UI Móvil, Widget Flotante y Control al Vuelo** | 1.5 semanas | Interfaz emisor, burbuja flotante sobre cualquier app y selector dinámico de calidad. | ⏳ Pendiente |
| **Fase 6** | **Compatibilidad Multi-Protocolo (AirPlay & Web)** | 1.5 semanas | Recepción AirPlay para iOS y Web Receiver para laptops sin apps adicionales. | ⏳ Pendiente |
| **Fase 7** | **Profiling Térmico, Benchmarking y Release APK** | 1 semana | Pruebas de latencia en TV Boxes genéricos (Amlogic/Rockchip) y empaquetado de producción. | ⏳ Pendiente |

---

## 🛠️ DESGLOSE DETALLADO DE CADA FASE

---

### 🟢 FASE 1: CIMIENTOS Y ARQUITECTURA BASE (2 semanas)

#### Objetivo
Establecer el repositorio multi-módulo, el sistema de tipos compartidos, las interfaces de dominio de Clean Architecture y un simulador de bucle cerrado para pruebas en computadora y emuladores.

#### Actividades
1. **Configuración del Entorno Multi-Módulo:**
   - Creación del proyecto con módulos Gradle:
     * `:core:domain` (Interfaces, entidades de sesión, perfiles de calidad y lógica pura sin dependencias de UI).
     * `:core:protocol` (Esquemas de mensajes de señalización local mDNS/WebSocket en JSON/Protobuf).
     * `:app-sender` (Aplicación para el teléfono móvil).
     * `:app-receiver` (Aplicación para la TV Box con soporte Leanback).
2. **Definición de Interfaces Core (Ports):**
   - `IScreenCaptureEngine`: Contrato para iniciar/pausar/detener captura de frames.
   - `IAudioCaptureEngine`: Contrato para captura de audio digital estéreo a 48kHz.
   - `IHardwareMediaCodec`: Contrato para codificación y decodificación de video H.264/HEVC.
   - `ILocalSignalingChannel`: Contrato para descubrimiento y handshake de conexión.
   - `IP2PTransport`: Contrato para el canal de transporte de paquetes de video y audio.
3. **Desarrollo de Adaptadores Mock:**
   - Implementación de `MockScreenCapturer` (generador de patrones de prueba SMPTE con timestamp en milisegundos para medir latencia).
   - Implementación de `MockSignalingChannel` sobre loopback local (127.0.0.1).
4. **Configuración de CI/CD y Linters:**
   - Configuración de `ktlint` y `detekt` con reglas estrictas de Clean Architecture.
   - Suite de pruebas unitarias iniciales para el módulo `:core:domain`.

#### Entregables de Fase 1
- Repositorio estructurado y compilable sin advertencias ni errores.
- Paquete de interfaces en `:core:domain:interfaces`.
- Simulador funcional de captura y transporte local verificado con pruebas unitarias.

---

### 🟢 FASE 2: CAPTURA DE SISTEMA ANDROID Y CODIFICACIÓN HARDWARE (2 semanas)

#### Objetivo
Capturar la pantalla completa del celular o ventanas individuales de cualquier aplicación a 60 FPS mediante `MediaProjection`, capturar el audio digital interno sin ruidos y codificar el stream utilizando el chip gráfico móvil vía `MediaCodec`.

#### Actividades
1. **Implementación del Foreground Service de Captura:**
   - Creación de `ProjectionForegroundService` con notificación persistente requerida por Android 10/14+.
   - Manejo seguro de permisos en tiempo de ejecución: `RECORD_AUDIO`, `FOREGROUND_SERVICE_MEDIA_PROJECTION`, `POST_NOTIFICATIONS`.
   - Gestión del token de permiso de `MediaProjectionManager`.
2. **Captura de Pantalla vía VirtualDisplay:**
   - Creación de `VirtualDisplay` asociado al tamaño y densidad de pantalla real del dispositivo (`DisplayMetrics`).
   - Conexión directa del búfer de salida del `VirtualDisplay` al `InputSurface` de `MediaCodec` (cero copia de memoria RAM).
3. **Captura de Audio Interno Digital:**
   - Implementación de `AudioPlaybackCaptureConfiguration` con filtro `USAGE_MEDIA`, `USAGE_GAME` y `USAGE_UNKNOWN`.
   - Configuración de `AudioRecord` en modo baja latencia (PCM 16-bit, 48kHz, estéreo).
   - Conversión y compresión en tiempo real mediante codificador **Opus** a 128 kbps.
4. **Pipeline de Codificación por Hardware (MediaCodec):**
   - Configuración de `MediaCodec` en modo H.264/AVC (High Profile, Nivel 4.2) y HEVC/H.265 opcional.
   - Tasa de bits variable adaptativa (CBR/VBR de 4 Mbps a 20 Mbps).
   - Parámetro `KEY_LATENCY = 0` y `KEY_PRIORITY = 0` (tiempo real prioritario sobre compresión).
   - Configuración de intervalo de fotogramas clave (KeyFrame Interval) cada 1 segundo para recuperación instantánea ante pérdida de paquetes.

#### Entregables de Fase 2
- Servicio nativo funcional capaz de capturar video y audio interno de cualquier app (TikTok, YouTube, juegos, navegadores).
- Pipeline `VirtualDisplay` → `InputSurface` → `MediaCodec` generando paquetes NALU H.264 listos para transmisión.
- Módulo de captura de audio entregando paquetes Opus sincronizados.

---

### 🟢 FASE 3: MOTOR DE TRANSPORTE WEBRTC P2P LOCAL (2.5 semanas)

#### Objetivo
Construir el motor de transporte en tiempo real sobre la red Wi-Fi local, con descubrimiento automático cero-configuración mediante mDNS y señalización WebSocket local sin depender de servidores en internet.

#### Actividades
1. **Servidor de Señalización Embebido en la TV Box:**
   - Implementación de un servidor WebSocket ultraligero embebido en la aplicación de la TV Box utilizando Ktor / Netty (puerto local 8088).
   - Protocolo de handshake en JSON para intercambio de ofertas y respuestas SDP (Session Description Protocol).
2. **Descubrimiento Local Cero-Configuración (mDNS / NSD):**
   - Implementación de `NsdManager` en el receptor de la TV Box para publicar el servicio `_omnicast._tcp`.
   - Implementación del escáner en la app móvil que lista instantáneamente todas las TV Box encendidas en la red local.
3. **Canal WebRTC P2P con PeerConnection:**
   - Configuración de la librería `libwebrtc` (Google WebRTC Android SDK) en modo solo local.
   - Filtrado estricto de candidatos ICE para seleccionar únicamente direcciones IP privadas LAN (192.168.x.x, 10.x.x.x).
   - Desactivación de consultas a servidores STUN/TURN externos para permitir funcionamiento 100% offline.
4. **Inyección de Pistas de Video y Audio Personalizadas:**
   - Implementación de `CustomVideoSource` y `CustomAudioSource` para alimentar WebRTC con los búferes de `MediaCodec` generados en la Fase 2.
   - Configuración de RTP Payload Types para H.264 y Opus.
5. **Control de Congestión y Adaptación Dinámica de Red:**
   - Activación de mecanismos RTCP: NACK (Negative Acknowledgment), PLI (Picture Loss Indication) y FIR (Full Intra Request).
   - Ajuste automático de bitrate si la señal Wi-Fi decae (de 15 Mbps a 6 Mbps sin cortar la transmisión).

#### Entregables de Fase 3
- Conexión P2P local establecida entre teléfono móvil y TV Box en menos de 800 milisegundos tras seleccionar el dispositivo.
- Transmisión continua y fluida de paquetes de video y audio con latencia medida < 70 ms.

---

### 🟢 FASE 4: APLICACIÓN RECEPTORA PARA TV BOX (LEANBACK & HARDWARE DECODE) (2 semanas)

#### Objetivo
Desarrollar la aplicación receptora para TV Box optimizada para pantallas grandes, control remoto D-Pad y decodificación eficiente por hardware sobre procesadores Amlogic, Rockchip y Allwinner.

#### Actividades
1. **Diseño de Interfaz Leanback TV (UI/UX 10 Pies):**
   - Creación de la pantalla principal optimizada para visualización a 3 metros de distancia.
   - Soporte total de navegación por control remoto tradicional (D-Pad: arriba, abajo, izquierda, derecha, OK, atrás).
   - Visualización de estado: "Listo para conectar", nombre del dispositivo ("TV Box Sala"), código PIN temporal y código QR de conexión rápida.
2. **Pipeline de Decodificación por Hardware:**
   - Implementación de decodificador `MediaCodec` en el receptor configurado para volcar los fotogramas directamente en una `SurfaceView`.
   - Renderizado con `SurfaceHolder` en modo hardware (`FLAG_HARDWARE_ACCELERATED`).
   - Cero sobrecarga de CPU: decodificación directa en la VPU (Video Processing Unit) del procesador de la TV Box.
3. **Pipeline de Reproducción de Audio de Baja Latencia:**
   - Renderizado de audio vía `AudioTrack` en modo `MODE_STREAM` con tamaño de búfer mínimo (Low Latency Audio Track).
   - Sincronización A/V (Audio/Video Lip-Sync) manteniendo desfase inferior a 15 milisegundos.
4. **Mecanismo de Seguridad y Emparejamiento Local:**
   - Verificación opcional por código PIN de 4 dígitos generado en la pantalla de la TV para autorizar nuevas conexiones y evitar proyecciones no deseadas de vecinos en la misma red Wi-Fi.
5. **Overlay de Estadísticas en Vivo (Modo Ingeniero):**
   - Menú accesible desde el control remoto para visualizar métricas en pantalla: FPS actuales, bitrate recibido, latencia en ms, paquetes perdidos y temperatura del procesador.

#### Entregables de Fase 4
- APK instalable en cualquier TV Box con Android 7.0 o superior (Google TV, Android TV, Fire TV, TV Boxes AOSP).
- Interfaz fluida y controlable 100% con control remoto tradicional.
- Renderizado de video a 60 FPS con temperatura de TV Box controlada y CPU < 15%.

---

### 🟢 FASE 5: UI MÓVIL, WIDGET FLOTANTE Y CONTROL AL VUELO (1.5 semanas)

#### Objetivo
Crear una aplicación móvil moderna, intuitiva y rápida, complementada con un widget flotante accesible sobre cualquier aplicación proyectada.

#### Actividades
1. **Pantalla Principal del Emisor Móvil (Jetpack Compose / Material 3):**
   - Lista automática de TV Boxes detectadas en la red con indicador de potencia de señal y latencia estimada.
   - Botón de un toque: "Iniciar Proyección en TV".
   - Selector de perfiles de calidad predefinidos:
     * **Modo Fluidez (Juegos):** 720p @ 60 FPS (Bitrate: 6 Mbps, Buffer mínimo).
     * **Modo Estándar (Redes Sociales y Navegación):** 1080p @ 60 FPS (Bitrate: 10 Mbps).
     * **Modo Cine (Películas y Videos):** 1080p / 4K @ 30/60 FPS (Bitrate: 18 Mbps, Corrección avanzada de color).
2. **Widget Flotante Sobre la Pantalla (Floating Overlay Bubble):**
   - Servicio con permiso `SYSTEM_ALERT_WINDOW`.
   - Burbuja minimizable con controles rápidos:
     * Pausar imagen (útil para ingresar contraseñas o datos privados en el celular sin que se vean en la TV).
     * Alternar salida de audio (TV Box / Altavoz del Teléfono / Ambos).
     * Botón de desconexión inmediata.
3. **Gestión de Rotación y Relación de Aspecto:**
   - Detección automática de giro del teléfono (Modo Vertical / Modo Horizontal).
   - Escalado inteligente en la TV Box:
     * En vertical: Presenta barras laterales elegantes difuminadas (estilo Reels/TikTok) para no deformar la imagen.
     * En horizontal: Ocupa el 100% del panel de la televisión en formato 16:9 sin bordes.

#### Entregables de Fase 5
- Interfaz móvil pulida en Jetpack Compose.
- Burbuja flotante funcional que permite controlar la proyección sin cerrar la app en uso.
- Rotación automática vertical/horizontal fluida y sin cortes de transmisión.

---

### 🟢 FASE 6: COMPATIBILIDAD MULTI-PROTOCOLO (AIRPLAY & WEB RECEIVER) (1.5 semanas)

#### Objetivo
Convertir la TV Box en una estación de proyección universal capaz de recibir no solo la app de Android, sino también dispositivos iPhone/iPad (AirPlay) y computadoras PC/Mac mediante navegador web sin instalar software.

#### Actividades
1. **Motor Receptor AirPlay en TV Box:**
   - Integración de servidor receptor AirPlay (RTSP + FairPlay/OpenAirPlay emulation).
   - Emisión del servicio mDNS `_airplay._tcp` y `_raop._tcp`.
   - Permite que cualquier dispositivo iOS en la casa proyecte con la opción "Duplicar Pantalla" nativa de Apple.
2. **Receptor Web para Laptops (WebRTC Browser Cast):**
   - Servidor web ligero en la TV Box que expone una página web local (e.g. `http://192.168.1.45:8088/webcast`).
   - Cualquier persona con una laptop (Windows, Mac, Linux o Chromebook) puede abrir esa URL en Chrome/Edge, hacer clic en "Compartir Pestaña/Pantalla" y transmitir a la TV Box instantáneamente sin apps.
3. **Motor DLNA / UPnP Renderer:**
   - Capacidad de recibir archivos directos de video, música o fotos enviados desde la galería del teléfono sin duplicar la pantalla entera.

#### Entregables de Fase 6
- TV Box convertida en un concentrador multimedia universal: recibe Android (OmniCast Pro), iOS (AirPlay nativo) y computadoras (WebRTC Web).

---

### 🟢 FASE 7: PROFILING TÉRMICO, BENCHMARKING Y RELEASE APK (1 semana)

#### Objetivo
Ejecutar pruebas exhaustivas de latencia, optimización para TV Boxes genéricos económicos y generación de instaladores APK finales de producción.

#### Actividades
1. **Pruebas de Estrés y Medición de Latencia Real:**
   - Medición con cronómetro de alta precisión en pantalla: captura fotográfica simultánea de móvil y TV para certificar latencia inferior a 60 milisegundos.
   - Prueba continua de 3 horas proyectando videojuegos y video 1080p 60 FPS para verificar estabilidad de memoria y temperatura.
2. **Pruebas en Hardware Real de TV Boxes Económicos:**
   - Verificación en procesadores Amlogic S905X / S905W, Rockchip RK3318 / RK3566 y Allwinner H6.
   - Ajuste de buffers de hardware para evitar parpadeos en dispositivos con 1GB y 2GB de RAM.
3. **Generación de Entregables APK:**
   - Compilación de `OmniCast-Sender-v1.0.apk` (Móvil).
   - Compilación de `OmniCast-Receiver-TVBox-v1.0.apk` (TV Box con Android TV / AOSP).
4. **Documentación de Usuario y Puesta en Marcha:**
   - Manual de usuario ilustrado paso a paso para instalación mediante memoria USB o red local.

#### Entregables de Fase 7
- Reporte formal de Benchmarking de Latencia y Desempeño.
- APKs de producción firmados y listos para distribución e instalación directa.
