# HU-010: Interfaz Minimalista Flat Estilo WVC, Control Multimedia Desacoplado y Salida Limpia

**Estado:** Aprobado  
**Fecha:** 2026-10-17  
**Módulos afectados:** `:app-sender`, `:core:domain`, `:core:protocol`  
**Versión objetivo:** v3.32.85  

---

## 1. Contexto y Problema

Durante las pruebas de campo en sitios cinematográficos (Pelisplus, DropCDN), la experiencia de usuario presentó tres fricciones críticas:
1. **Obstrucción por Superposiciones:** La pastilla verde y tarjeta flotante del Asistente (`FloatingIndicatorService`) se dibujaban sobre el navegador web (`SYSTEM_ALERT_WINDOW`), tapando botones clave (Latino, Castellano, Play, verificación Cloudflare Turnstile) e interfiriendo con los eventos táctiles del WebView.
2. **Saturación en Barra Superior:** Se agruparon botones con fondos y bordes pesados (botón azul `🎛️` junto al botón naranja `▶`), reduciendo drásticamente el ancho de la caja de búsqueda y creando confusión sobre la función de cada botón.
3. **Control Multimedia Invasivo:** La barra multimedia ocupaba casi media pantalla con deslizadores y volumen incluso durante la navegación, sin permitir al usuario ocultarla al 100% para visualizar la página web completa.
4. **Falta de Salida Explícita de la App:** No existía un botón discreto en la pantalla principal para cerrar y matar la aplicación liberando todos los recursos en memoria RAM y sockets de red.

---

## 2. Criterios de Aceptación (Contrato Inamovible)

### CA-01: Supresión de Superposiciones Flotantes en Navegador
- Al entrar en el navegador web integrado (`InAppBrowserScreen`), el servicio `FloatingIndicatorService` oculta automáticamente el indicador flotante (`rootLayout.visibility = View.GONE`).
- Al salir del navegador de regreso a la pantalla principal, el estado previo se restaura.
- La pantalla web queda 100% libre de capas superpuestas que bloqueen formularios o el checkbox de Cloudflare Turnstile.

### CA-02: Barra Superior Minimalista Flat (Estilo Web Video Caster)
- Eliminación de cajitas, marcos y fondos oscuros en los botones de navegación; todos los iconos se presentan como glifos planos (*Flat Glyphs*) en escala 18dp–20dp color gris suave (`#CBD5E1`).
- Eliminación del botón redundante `🎛️`.
- Botón de **Play Circular (`▶`) exclusivo para fuentes y calidades:** Abre única y exclusivamente la hoja de videos detectados (`WvcVideoListSheet`).
- Inclusión del **Icono de Transmisión/TV (Cast):** Al tocarlo cuando hay un video en reproducción, alterna o restaura la barra de control multimedia.
- Botón explícito de **Cerrar Navegador (`✕`):** Permite retornar directamente al menú principal de OmniCast.

### CA-03: Barra Multimedia con Ocultamiento Total a Voluntad (0 Píxeles)
- Aparece únicamente cuando se está transmitiendo y reproduciendo un flujo en la TV (`isCasting && isPlaying`).
- **Modo Compacto por Defecto:** Franja delgada de una sola línea (44dp) con flecha de expansión (`^`), título con elipsis, Play/Pausa, Stop y botón de minimizar (`⌵`).
- **Ocultamiento Total:** Al pulsar `⌵`, la barra se oculta al 100%, dejando el 100% de la altura de la pantalla disponible para la página web.
- **Restauración:** Se recupera al instante pulsando el icono de TV/Cast en la barra superior o deslizando hacia arriba desde el borde inferior.

### CA-04: Replicación del Control de Zoom TV en Controles Multimedia
- En los controles multimedia expandidos, se integra un botón de **Zoom TV** conectado directamente a `TvZoomManager`:
  - Cicla en tiempo real: `Fit 100%` $\rightarrow$ `85%` $\rightarrow$ `75%` $\rightarrow$ `115%` $\rightarrow$ `130%` $\rightarrow$ `16:9 Llenar Pantalla`.
  - Envía `SignalingMessage.SetDisplayMode` a la Android TV sin depender de menús flotantes.

### CA-05: Botón Discreto y Explícito de Salida en Pantalla Principal
- En `SenderHeaderBar`, junto al botón de ayuda, se ubica un botón circular discreto con el icono estándar de encendido/apagado (`PowerSettingsNew`) en tono rojizo tenue, sin texto.
- Al pulsarlo, detiene todos los servicios en segundo plano (`ProjectionForegroundService`, `CastMediaNotificationService`, `FloatingIndicatorService`, `LocalStreamProxyServer`), cierra conexiones WebSocket y ejecuta `finishAffinity()` liberando el 100% de recursos del sistema.
