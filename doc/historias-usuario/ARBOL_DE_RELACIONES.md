# 🌳 ÁRBOL DE RELACIONES Y FUNCIONAMIENTO DE LA APK (Feature Dependency Graph)

Este documento describe el **árbol de dependencias, relaciones y desacoplamiento** entre las diferentes Historias de Usuario (HU) y los componentes de la APK.

---

## 1. Principio Fundamental: Cero Acoplamiento Directo (Zero Tight Coupling)
- Las Historias de Usuario en OmniCast **no deben tener acoplamiento rígido entre sí**.
- Una modificación en la interfaz de la TV (`HU-002`) no puede afectar la navegación de la galería en el móvil (`HU-001`).
- La única vía de comunicación entre el móvil (`:app-sender`) y la TV (`:app-receiver`) es el **Protocolo de Señalización Inmutable (`:core:protocol`)** mediante mensajes WebSocket tipados (`SignalingMessage`).

---

## 2. Mapa Visual de Relaciones y Flujos

```mermaid
graph TD
    classDef base fill:#1e293b,stroke:#475569,stroke-width:2px,color:#f8fafc;
    classDef sender fill:#1e1b4b,stroke:#6366f1,stroke-width:2px,color:#f8fafc;
    classDef receiver fill:#064e3b,stroke:#10b981,stroke-width:2px,color:#f8fafc;
    classDef protocol fill:#451a03,stroke:#f59e0b,stroke-width:2px,color:#f8fafc;

    subgraph PROTOCOL [Protocolo Común Desacoplado :core:protocol]
        MSG_IMG[ShowImage / ImageTransform]:::protocol
        MSG_PLAY[DirectStream / PlayControl]:::protocol
        MSG_STAT[PlaybackStatus Telemetry]:::protocol
    end

    subgraph SENDER [Móvil - Emisor :app-sender]
        MAIN_S[OmniCast Home Screen]:::sender
        HU_001[HU-001: Galería Continua & Retorno]:::sender
        HU_003[HU-003: Menú Lateral & Historial con Tiempo]:::sender
        HU_004_S[HU-004: Extracción Títulos Reales Web]:::sender
        BROWSER[InAppBrowser / Sniffer C++]:::sender
    end

    subgraph RECEIVER [TV Box - Receptor :app-receiver]
        HU_002[HU-002: Standby Limpio TV Box Seekee]:::receiver
        EXO_TV[ExoPlayer / Web Engine TV]:::receiver
        HU_004_R[HU-004: Deduplicación CDN en Historial TV]:::receiver
    end

    %% Relaciones en Sender
    MAIN_S -->|Abre Selector| HU_001
    MAIN_S -->|Botón ☰| HU_003
    MAIN_S -->|Navegación Web| BROWSER
    BROWSER -->|Detecta Video Limpio| HU_004_S

    %% Comunicación Desacoplada Móvil -> TV
    HU_001 -.->|1. ShowImage Base64| MSG_IMG
    MSG_IMG -.->|Renderiza Foto en TV| HU_002

    HU_003 -.->|2. PlayControl ▶ TV| MSG_PLAY
    MSG_PLAY -.->|Reproduce Video| EXO_TV

    HU_004_S -.->|3. DirectStream con Título Real| MSG_PLAY

    %% Telemetría Desacoplada TV -> Móvil
    EXO_TV -.->|4. Reporta Tiempo Actual| MSG_STAT
    MSG_STAT -.->|Actualiza 'Se quedó en: mm:ss'| HU_003

    %% TV Interno
    EXO_TV -->|Guarda Registro| HU_004_R
    HU_004_R -->|Muestra Carpetas y Continuar Viendo| HU_002
```

---

## 3. Matriz de Impacto y Aislamiento por Historia

| Historia de Usuario | Ámbito / Módulo | Depende de | Afecta a | Nivel de Acoplamiento |
|---|---|---|---|---|
| **HU-001 (Galería Fotos)** | `:app-sender` | `GalleryPickerActivity`, `PhotoCastViewerActivity` | `SignalingMessage.ShowImage` en TV | 🟢 **Bajo (Totalmente Aislado):** Cambiar el visor o el botón Atrás no impacta a la web ni al menú lateral. |
| **HU-002 (Standby Limpio TV)** | `:app-receiver` | `TvStandbyCleanScreen`, `TvCleanComponents` | Vista de espera en TV Box | 🟢 **Bajo:** Modificar el diseño o dock no altera el reproductor ExoPlayer ni al móvil. |
| **HU-003 (Menú Lateral Móvil)** | `:app-sender` | `SideMediaDrawer`, `PlayHistoryManager` | Emisión de `SignalingMessage.DirectStream` | 🟡 **Medio (Consumidor):** Consume el evento de telemetría de la TV para mostrar `Se quedó en: mm:ss`. |
| **HU-004 (Anti-Duplicados Web)** | `:app-sender` y `:app-receiver` | `DynamicSnifferManager`, `TvPlaybackHistoryManager` | Normalización de títulos en ambos extremos | 🟢 **Bajo:** Normaliza URLs antes de guardarlas; no afecta el motor WebRTC ni la galería. |

---

## 4. Flujo de Navegación Inversa de HU-001 (Pila de Actividades / Back Stack)

El principio de navegación inversa solicitado exige que la pila de Android se comporte como una pila LIFO estricta:

```
[Paso 0: OmniCast SenderMainActivity]
       │
       ▼ (Pulsar "Proyectar Foto")
[Paso 1: Explorador del Sistema (DocumentsUI / Picker)]
       │
       ▼ (Seleccionar app: Galería nativa, Google Fotos, etc.)
[Paso 2: Aplicación Elegida (ej: Galería)]
       │
       ▼ (Seleccionar foto)
[Paso 3: Visor a Pantalla Completa (PhotoCastViewerActivity) sincronizado con TV]
```

### Comportamiento del Botón ATRÁS en Orden Inverso:
1. **Pulsación 1 (desde Paso 3):** Retrocede al **Paso 2 (Galería elegida)**.
2. **Pulsación 2 (desde Paso 2):** Retrocede al **Paso 1 (Explorador del sistema)**.
3. **Pulsación 3 (desde Paso 1):** Retrocede al **Paso 0 (OmniCast)**.
