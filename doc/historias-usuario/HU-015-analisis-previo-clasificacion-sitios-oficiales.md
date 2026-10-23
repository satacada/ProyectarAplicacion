# HU-015: Clasificación Dinámica Dual de Páginas y Pre-Análisis de Películas

**Épica:** Análisis Inteligente, Detección de Streams y Resiliencia de Navegación  
**Versión de Introducción:** v3.4.34  
**Estado:** Implementado / Validado  

---

## 1. Descripción de la Historia

**Como** usuario del explorador web de OmniCast,  
**Quiero** que la aplicación distinga automáticamente entre plataformas oficiales (YouTube, Dailymotion, OK.ru, buscadores) y sitios web de películas y streaming pirata (PelisGO, Repelis, Cuevana, etc.) mediante una lista viva y sincronizada,  
**Para** que en plataformas oficiales se mantenga una experiencia 100% nativa con cero manipulación de inputs y foco intacto (Modo "Guantes de Seda"), mientras que en sitios de películas se realice un pre-análisis profundo de iframes, servidores y neutralización de velos invisibles (Modo "Escaneo Total de Películas") sin romper la navegación.

---

## 2. Criterios de Aceptación (Contrato Inamovible)

1. **Clasificación Dinámica y Lista Oficial Viva (`BrowserPageClassifier`):**
   - El clasificador mantiene una lista oficial inicial con más de 40 dominios globales de video y búsqueda (Dailymotion, YouTube, OK.ru, VK, Vimeo, Twitch, Canela TV, TikTok, Twitter/X, Reddit, buscadores Google/Bing/Yandex/DuckDuckGo).
   - **Persistencia y Sincronización en la Nube:** Almacenamiento local en SharedPreferences (`omnicast_official_platforms_prefs`) y sincronización silenciosa periódica con Supabase (`event = "OFFICIAL_PLATFORM"`). Si surgen nuevas plataformas o subdominios oficiales, la lista se actualiza dinámicamente sin requerir recompilar el APK.
   - **Caché en RAM de 0ms:** `ConcurrentHashMap.newKeySet()` en memoria permite clasificar la URL en menos de 0.05 ms.

2. **Modo A: Plataformas Oficiales / Guantes de Seda (`BrowserPageMode.OFFICIAL_PLATFORM`):**
   - **Cero inyección en el DOM:** No se inyectan scripts de autocompletado de formularios ni estilos CSS que modifiquen `<input>` o `<textarea>`.
   - **Foco 100% nativo de Chromium:** El teclado virtual de Android (Gboard, Samsung Keyboard, etc.) y los buscadores interactivos (Dailymotion, Google) operan de forma limpia e inmediata.
   - **Detección limpia por red:** La captura de video fluye por `shouldInterceptRequest` y extractores nativos (`YouTubeExtractor`, `DailymotionExtractor`).

3. **Modo B: Escaneo Total de Películas (`BrowserPageMode.MOVIE_STREAMING` / `MoviePagePreScanner`):**
   - **Pre-análisis de iframes:** Inspecciona y activa atributos `data-src` o `data-lazy-src` de reproductores embebidos (Streamtape, Voe, Filemoon, Doodstream, Vidhide, Pelismart, Fastream, Dropcdn) y reporta candidatos vía `OMNICAST_SERVER_CANDIDATE`.
   - **Pre-análisis de tabs de servidores:** Detecta botones y pestañas con `data-url`, `data-src` o `data-video` (Dooplay, Cuevana, Pelisplus) para alimentar el pipeline de detección sin requerir clics a ciegas del usuario.
   - **Neutralización de velos invisibles:** Oculta y desactiva capas transparentes `click-shield` o falsos reproductores que no contengan elementos multimedia o de formulario.

4. **Límite de Líneas y No Regresiones:**
   - Todos los archivos se mantienen estrictamente $\le 300$ líneas.
   - Se mantiene intacta la protección de hardware y WebRTC (`TvScreens.kt` y `WebRtcReceiverManager.kt`).
