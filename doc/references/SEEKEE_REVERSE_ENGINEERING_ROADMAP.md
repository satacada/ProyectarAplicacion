# SEEKEE REVERSE ENGINEERING & REFINEMENT ROADMAP (OmniCast v3.26+)

Este documento recopila la auditoría de ingeniería inversa realizada sobre la APK `Seekee-2-14-0.xapk` (`com.enzo.paulo`), el estado actual de OmniCast v3.26, y la guía para continuar el afinamiento en nuevas sesiones de chat.

---

## 1. ESTADO ACTUAL DEL PROYECTO (v3.26)
- **Versión:** `versionCode = 31`, `versionName = "3.26.0"`.
- **Móvil del usuario:** APK `app-sender-debug.apk` instalado vía ADB en `192.168.0.13:37115`.
- **TV Box:** Receptor disponible en `http://192.168.0.3:8080/tv.apk`.
- **Novedad implementada en v3.26:**
  - Sistema de Listas de Reproducción y Marcadores estilo YouTube (`PlaylistManager.kt`, `PlaylistModels.kt`, `SaveToPlaylistSheet.kt`, `SavedPlaylistsSheet.kt`).
  - Botón `💾 Guardar` integrado en el reproductor remoto y barra de detección.
  - Centro de navegación: `📂 Listas Guardadas` y `📜 Historial de Películas`.
  - Fix de fotos instantáneas Base64 con exclusión mutua estricta.
  - Fix de zoom en TV Box sin colapso de contexto EGL (escalado hardware GPU `scaleX`/`scaleY`).

---

## 2. HALLAZGOS TÉCNICOS EXTRAÍDOS DE SEEKEE (`com.enzo.paulo`)

### A. Motor AdBlock Nativo C++ (`libadblock-client.so`)
- **Origen:** Proyecto de código abierto `io.github.edsuns.adblockclient` (núcleo C++ de AdBlockPlus/Brave).
- **Problema actual en OmniCast:** `AdBlocker.kt` tiene una lista estática de ~95 dominios y palabras clave. Los sitios piratas crean dominios efímeros (`*.xyz`, `*.live`) y overlays transparentes (`<div class="overlay">`) que se cuelan.
- **Solución evaluada:** Integrar `AdBlockClient` con filtros estándar EasyList / uBlock para neutralizar popups y overlays invisibles por CSS y regex en `shouldInterceptRequest`.

### B. Interceptación y Preservación de Blobs (`blob:` y `URL.revokeObjectURL`)
- **Origen:** Script inyectado `assets/local_script.js`.
- **Problema actual en OmniCast:** Plataformas como Twitter/X, TikTok o Reddit usan Media Source Extensions (MSE). Generan URLs `blob:` en RAM y ejecutan `URL.revokeObjectURL()` a los pocos milisegundos. Un `blob:` no es reproducible directamente por la TV Box porque reside en la RAM del móvil.
- **Solución evaluada:** Sobrescribir `URL.revokeObjectURL` para mantener el blob activo y extraer los segmentos subyacentes o capturar el flujo antes de la revocación.

### C. Remuxing y Códecs de Audio Incompatibles (FFmpegKit)
- **Origen:** Binarios nativos en `lib/arm64-v8a/`: `libffmpegkit.so`, `libavformat.so`, `libavcodec.so`.
- **Problema actual en OmniCast:** Las TV Box económicas carecen de licencias Dolby (AC3 / EAC3 5.1) y DTS, o fallan al reproducir streams DASH con video y audio separados (como Dailymotion tasa original).
- **Solución evaluada:** Embeber FFmpeg para remuxing o transcodificación de audio incompatible a AAC estándar directamente en el móvil antes de emitir a la TV.

### D. Actualización Dinámica de Detectores (`ResourceAnalysisScript.js`)
- **Origen:** Carga dinámica desde `resourceAnalysis.html` hacia `Documents/ResourceAnalysisScript.js`.
- **Problema actual en OmniCast:** Los detectores de `WvcDomSniffer.kt` son estáticos en el APK. Cuando YouTube o Facebook cambian su reproductor web, la app requiere recompilación.
- **Solución evaluada:** Mecanismo de descarga o caché de script sniffer para actualizar patrones sin recompilar el APK.

---

## 3. PÁGINAS DE PRUEBA PARA COMPARAR COMPORTAMIENTO
1. **Publicidad y popups invisibles:** `https://animeflv.net` o `https://pelismart.mov` (servidores Streamwish / Lulustream).
2. **Streams directos no adaptativos:** `https://www.dailymotion.com` (seleccionar *Video Directo / Tasa Máxima*).
3. **Videos basados en memoria RAM (`blob:`):** `https://x.com` o `https://www.tiktok.com`.
4. **Videos secuenciales con DOM dinámico:** `https://ok.ru` y `https://m.facebook.com/watch`.
