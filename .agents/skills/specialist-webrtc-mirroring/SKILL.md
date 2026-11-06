---
name: specialist-webrtc-mirroring
description: Especialista en WebRTC, Señalización WebSocket de Baja Latencia y Screen Mirroring (Modelo flash). Protege la separación Pantalla Completa vs Por Aplicación, escalado matemático y obediencia <5ms del comando STOP.
---

# 📡 Especialista en WebRTC, Sockets y Proyección (`specialist-webrtc-mirroring`)

## 1. Configuración de Modelo y Ámbito
- **Modelo Obligatorio:** `flash`.
- **Ámbito Exacto:**
  - `core/protocol/src/main/kotlin/com/omnicast/core/protocol/SignalingMessage.kt`
  - `app-sender/src/main/kotlin/com/omnicast/sender/webrtc/WebRtcSenderManager.kt`
  - `app-receiver/src/main/kotlin/com/omnicast/receiver/webrtc/WebRtcReceiverManager.kt` *(SELLADO)*
  - `app-receiver/src/main/kotlin/com/omnicast/receiver/ui/components/TvScreens.kt` *(SELLADO)*
  - `app-receiver/src/main/kotlin/com/omnicast/receiver/server/LocalSignalingServer.kt`

## 2. Leyes Inviolables de Hardware WebRTC (`HU-013`)
1. **Prohibido `changeCaptureFormat()` en Caliente:**
   - Nunca invocar `changeCaptureFormat()` ni registrar un `DisplayListener` que redimensione el encoder H.264 al rotar el teléfono en Android 14.
2. **Tolerancia a `IceConnectionState.DISCONNECTED`:**
   - `DISCONNECTED` es transitorio; queda prohibido cerrar la sesión WebRTC salvo ante `FAILED` o `CLOSED`.
3. **Separación Inmutable: Pantalla Completa vs Por Aplicación:**
   - **Pantalla Completa (`isAppProjectionMode == false`):** En vertical FIT aplica factor `1.075f` con `Modifier.fillMaxHeight().aspectRatio(streamAspect)` y `WRAP_CONTENT, MATCH_PARENT`.
   - **Por Aplicación (`isAppProjectionMode == true`):** Usa `Modifier.fillMaxSize()` y `MATCH_PARENT, MATCH_PARENT`, ignorando `isPhoneLandscape` y obedeciendo solo `effectiveFrameW > effectiveFrameH`.
4. **Obediencia Inmediata del Comando `STOP` (`< 5 ms`):**
   - `SignalingMessage.PlayControl("STOP")` o `Disconnect` debe detener reproductores, resetear WebRTC y volver a `ReceiverUiState.Standby` en menos de 5 ms.
