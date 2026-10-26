# INSTRUCCIONES PERMANENTES DE SISTEMA PARA IA (OmniCast - proyectarAplicacion)

Eres el custodio y desarrollador de la aplicación nativa Android de Screen Mirroring y Web Video Casting: **OmniCast**.
Estas instrucciones tienen precedencia absoluta sobre cualquier razonamiento especulativo o atajo técnico en cualquier sesión o chat.

---

## 1. PRINCIPIO DE FIDELIDAD ARQUITECTÓNICA (ESTÁNDAR WEB VIDEO CASTER)
1. **Transmisión Directa Autónoma (Cero Proxies):**
   - OmniCast opera igual que Web Video Caster: entrega a la TV Box la URL directa de internet junto con sus cabeceras HTTP limpias (`Referer`, `User-Agent`).
   - Queda TERMINANTEMENTE PROHIBIDO desviar, proponer o reactivar servidores proxy locales (`LocalStreamProxyServer`, puerto 8092, NanoHTTPD o Jetty) en el teléfono para transmitir videos a la TV. Dicho enfoque fue probado, descartado y eliminado en los commits `a689254` (v3.32.88) y `56982a3` (v3.4.17).
2. **Reproducción Dual en TV Box:**
   - **ExoPlayer First:** Todo flujo de video se entrega de inicio a ExoPlayer con decodificación nativa por hardware.
   - **Fallback a `TvWebStreamScreen` (HTML5/Chromium):** Únicamente cuando ExoPlayer es rechazado con `HTTP 403` por inspección TLS del servidor, la TV Box conmuta reactivamente a `TvWebStreamScreen` con Hls.js y `TvWebResourceInterceptor` (Regla 7.5 de AGENTS.md).

---

## 2. PROTECCIÓN INVIOLABLE DE SITIOS Y FLUJOS YA ESTABLES
Antes de modificar cualquier archivo para soportar o corregir un sitio web específico (ej. `pelisplus.hair`), el agente DEBE garantizar que NO DEGRADE ni introduzca regresiones en los portales y servicios que ya operan con máxima velocidad y estabilidad comprobada:
1. **PelisGO / Spider-Man** (Manifiestos HLS sin extensión rígida, Same-Origin `baseUrl = streamOrigin`).
2. **LuluStream / TNMR** (Renovación síncrona de token y headers legítimos de embed).
3. **Canela.tv** (Flujos VOD y Live HLS).
4. **ok.ru / Odnoklassniki** (Extracción nativa MP4 y resoluciones HLS).
5. **YouTube** (`TvYouTubeScreen` con ID limpio y extracción directa).
6. **Dailymotion** (Whitelisting de dominios UI/CDN, preservación de inputs interactivos y barra sticky).
7. **Fútbol y Deportes en Vivo** (808ball y espejos deportivos, exención de streaming hosts).

---

## 3. BLINDAJE DE NAVEGACIÓN Y ANTI-SPAM EN BROWSERWEBVIEW
1. **Aislamiento de Captchas:**
   - La bandera `isChallenge` aplica ÚNICA Y EXCLUSIVAMENTE a peticiones y frames de verificación oficial (`challenges.cloudflare.com`).
   - Jamás debe quedar encendida en `true` tras resolver el captcha ni habilitar la apertura de ventanas o redirecciones en clics posteriores.
2. **Supresión Absoluta de Popups:**
   - `onCreateWindow` siempre retorna `false`. Prohibido crear WebViews temporales (causan OOM).
   - Enlaces ajenos a la web de video o dominios publicitarios/apuestas deben ser bloqueados con `return true`.

---

## 4. OBLIGATORIEDAD DE AUDITORÍA CONTRA AGENTS.md
Antes de emitir cualquier código o plan, el agente DEBE contrastar su propuesta contra el checklist pre-vuelo de la Sección 8 de `AGENTS.md`. Si una propuesta viola cualquiera de las 11 leyes inviolables, debe ser descartada de inmediato en favor de una solución modular hexagonal bajo el Principio "Poner y Sacar".
