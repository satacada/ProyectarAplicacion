# Referencia Técnica: Ingeniería Inversa Web Video Caster

Este documento describe la ubicación de los archivos de referencia, el análisis de ingeniería inversa realizado a **Web Video Caster (v6.0.4)** y los contratos técnicos aplicados en **OmniCast Pro**.

---

## 1. Ubicación de Archivos de Referencia (Aislados del Proyecto)

Para no saturar el repositorio Git con binarios ni código decompilado de terceros, todos los recursos se encuentran archivados en:

- **Ruta Principal de Referencias:**  
  `/home/sataca/Descargas/referencias/web_video_caster/`
- **APK Original:**  
  `/home/sataca/Descargas/referencias/web_video_caster/web_video_caster_v6.0.4.apk`
- **Assets y Scripts Web Extraídos:**  
  `/home/sataca/Descargas/referencias/web_video_caster/assets/`
  - Sniffer DOM y Hooks: `assets/ibfunctions.js`
  - Receptor Web: `assets/web-receiver/`
  - Servidor WebSocket local: `assets/wss.js`
  - Extractores específicos: `assets/specialsites/`
- **Fuentes Decompiladas (Java):**  
  `/home/sataca/Descargas/referencias/web_video_caster/decompiled/sources/`
- **Documento Detallado de Análisis:**  
  `/home/sataca/Descargas/referencias/web_video_caster/analisis/ANALISIS_INGENIERIA_INVERSA.md`

---

## 2. Lecciones Técnicas y Cambios Incorporados en OmniCast

1. **User-Agent Universal de Chrome Mobile (`v3.32.81`):**
   - **Problema previo:** Servidores CDN (DropCDN/Cloudflare) retornaban HTTP 403 Forbidden cuando la TV Box o ExoPlayer intentaban reproducir directamente el flujo `.m3u8`.
   - **Solución WVC:** Se verificó que WVC reproduce directamente sin proxy emisor porque envía la firma estándar de Google Chrome Mobile:
     `Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36`.
   - **Módulos actualizados:** `ExoPlayerTvManager.kt`, `StreamManifestProbe.kt`, `LocalStreamProxyServer.kt`.

2. **Captura Temprana de Fuentes HTML5 (`universal_sniffer.js`):**
   - Al igual que en `ibfunctions.js`, se incorporó la inspección periódica y reactiva de elementos `<source src="...">` y atributos `data-src` anidados en `<video>` antes de que se presione Play.

3. **Apertura Proactiva de la Hoja de Opciones (`InAppBrowserScreen.kt`):**
   - Comportamiento idéntico a WVC: al detectarse flujos reproducibles, la hoja de calidades se despliega de forma automática sin requerir pulsar el botón naranja.

4. **Resolución Proactiva de Cloudflare Turnstile:**
   - Despacho de secuencia física de puntero (`pointerdown`, `mousedown`, `pointerup`, `mouseup`, `click`) y exclusión en `NativeAdBlockEngine` para evitar falsos positivos de bloqueo sobre dominios de validación.

---

## 3. Modo de Consulta Recomendado

Para consultar clases decompiladas específicas de WVC sin consumir memoria ni CPU:
```bash
# Buscar métodos o cadenas en las fuentes decompiladas
grep -rn "com.instantbits.cast" /home/sataca/Descargas/referencias/web_video_caster/decompiled/sources/
```
