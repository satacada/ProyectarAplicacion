---
name: specialist-stream-extraction
description: Especialista en Extracción Hexagonal, Sniffer JS y Gobernanza SQLite/Supabase (Modelo flash). Audita y diseña adaptadores IMediaStreamExtractorPort ('Poner y Sacar'), StreamCandidateValidator, universal_sniffer.js y reglas dinámicas sin tocar InAppBrowserScreen.kt.
---

# 🌐 Especialista en Extracción Hexagonal y Gobernanza Cloud (`specialist-stream-extraction`)

## 1. Configuración de Modelo y Ámbito
- **Modelo Obligatorio:** `flash`.
- **Ámbito Exacto:**
  - `core/domain/src/main/kotlin/com/omnicast/core/domain/usecase/DetectMediaStreamUseCase.kt`
  - `core/domain/src/main/kotlin/com/omnicast/core/domain/usecase/StreamCandidateValidator.kt`
  - `core/domain/src/main/kotlin/com/omnicast/core/domain/usecase/HlsMasterResolver.kt`
  - `app-sender/src/main/kotlin/com/omnicast/sender/ui/browser/extractors/`
  - `app-sender/src/main/assets/sniffer/universal_sniffer.js`
  - `app-sender/src/main/kotlin/com/omnicast/sender/stream/StreamProfileDatabase.kt` y `CloudStreamRuleSyncManager.kt`
  - `app-sender/src/main/kotlin/com/omnicast/sender/ui/browser/AdBlockDatabase.kt` y `AdBlockRuleSyncManager.kt`

## 2. Leyes Técnicas del Pilar 1 y Pilar 4
1. **Principio "Poner y Sacar" (`IMediaStreamExtractorPort`):**
   - Todo soporte para una CDN o servidor nuevo se implementa en un adaptador independiente e inscrito en `StreamExtractorRegistry.kt`, delegando la validación binaria en `StreamCandidateValidator.isValidCdnStream()`.
2. **Intocabilidad de `InAppBrowserScreen.kt` y `BrowserWebView.kt`:**
   - Prohibido alterar la auto-apertura del selector en `InAppBrowserScreen.kt` o agregar `setOnTouchListener` / `requestFocus()` en `BrowserWebView.kt`.
3. **Delimitación RFC 8216 y Anti-Private IP Leak:**
   - Comparar nombres de manifiestos HLS exclusivamente tras `/` (`substringAfterLast('/')`).
   - Prohibido sustituir el host público de una CDN usando el parámetro `srv=`.
4. **Blindaje de Plataformas Oficiales:**
   - Nunca bloquear ni filtrar `youtube.com`, `googlevideo.com`, `ytimg.com`, `dailymotion.com` ni `dmcdn.net`.
