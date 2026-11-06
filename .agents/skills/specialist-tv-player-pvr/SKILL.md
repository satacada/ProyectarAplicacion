---
name: specialist-tv-player-pvr
description: Especialista en Receptor Android TV, Reproductor Híbrido (ExoPlayer + TvWebStreamScreen) y Motor PVR Dual (Modelo flash). Garantiza resiliencia Anti-CORS, preservación de cabeceras, grabación TS/fMP4/Chromium BoringSSL y reserva sagrada de almacenamiento.
---

# 📺 Especialista en Receptor TV, Reproductor Híbrido y Motor PVR (`specialist-tv-player-pvr`)

## 1. Configuración de Modelo y Ámbito
- **Modelo Obligatorio:** `flash`.
- **Ámbito Exacto:**
  - `app-receiver/src/main/kotlin/com/omnicast/receiver/directstream/ExoPlayerTvManager.kt`
  - `app-receiver/src/main/kotlin/com/omnicast/receiver/ui/components/TvWebStreamScreen.kt`
  - `app-receiver/src/main/kotlin/com/omnicast/receiver/ui/components/TvWebResourceInterceptor.kt`
  - `app-receiver/src/main/kotlin/com/omnicast/receiver/ui/components/TvYouTubeScreen.kt`
  - `app-receiver/src/main/kotlin/com/omnicast/receiver/directstream/PvrStreamRecorder.kt`
  - `app-receiver/src/main/kotlin/com/omnicast/receiver/directstream/PvrHlsMasterParser.kt`
  - `app-receiver/src/main/kotlin/com/omnicast/receiver/directstream/TvWebPvrBridge.kt`
  - `app-receiver/src/main/kotlin/com/omnicast/receiver/directstream/TvStorageSafetyManager.kt`

## 2. Leyes Técnicas del Pilar 3 y Motor PVR (`v3.6.08`)
1. **Arquitectura Híbrida Dual y Anti-CORS:**
   - Mantener siempre activo `TvWebStreamScreen` (Chromium + `Hls.js`) junto con `TvWebResourceInterceptor.shouldIntercept(r, headers)` (`Access-Control-Allow-Origin: *`) y preservar `headers = lastDirectHeaders` en todo fallback 403.
   - Toda URL que no sea `.mp4` o `.webm` directo debe inicializarse mediante `initHls()`.
2. **Motor PVR Dual (`HU-019` / `v3.6.08`):**
   - Variante 1 (DropCDN/Fastream en `TvWebStreamScreen`) graba a través de `TvWebPvrBridge.kt` usando el stack BoringSSL de Chromium.
   - Variante 2/3 (ExoPlayer) graba mediante `PvrStreamRecorder.kt` y `PvrHlsMasterParser.kt` soportando tanto segmentos `.ts` como HLS fMP4 (`#EXT-X-MAP:URI="init.mp4"`).
3. **Guardián Anti-Brick y Cero Archivos Fantasma:**
   - Respetar estrictamente la reserva sagrada en `TvStorageSafetyManager.kt`: **2.0 GB** libres intocables en almacenamiento interno de la TV Box y **500 MB** en USB externo, más creación perezosa y purga automática de archivos de 0 bytes.
