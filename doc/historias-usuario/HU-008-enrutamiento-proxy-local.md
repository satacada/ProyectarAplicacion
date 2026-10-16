# HU-008: Enrutamiento por Proxy HTTP Local para Flujos Protegidos y Preservación en Guardados

## 1. DESCRIPCIÓN DEL REQUERIMIENTO
Como usuario de OmniCast,
quiero que las películas y transmisiones en vivo que cuentan con protección estricta de navegador (Cloudflare Turnstile, DropCDN, tokens de sesión por TLS) se puedan reproducir en la TV Box mediante un enrutamiento por proxy HTTP local en el teléfono cuando el envío directo sea rechazado por el servidor (HTTP 403),
preservando como primera prioridad la reproducción directa y autónoma para todos los contenidos que no requieran proxy (para poder apagar el teléfono o salir de la casa sin interrumpir la película);
y además, quiero que el sistema me permita guardar estas películas protegidas en "Favoritos" o "Ver más tarde" como cualquier otra, advirtiéndome con un mensaje claro que para su reproducción en la TV se requiere que el teléfono emisor esté encendido y conectado a la misma red WiFi.

---

## 2. CRITERIOS DE ACEPTACIÓN (CONTRATO INAMOVIBLE)

### Criterio 1: Prioridad Absoluta al Flujo Directo e Independiente
- **Dado** cualquier contenido multimedia detectado (película, serie o stream en vivo),
- **Cuando** el usuario pulsa "Transmitir a TV Box",
- **Entonces** el sistema debe intentar SIEMPRE la reproducción directa desde internet como primera opción, garantizando que en el 95% de los sitios web la TV Box descargue de forma 100% autónoma y el teléfono emisor pueda apagarse o desconectarse sin cortar el video.

### Criterio 2: Activación Reactiva del Proxy Local ante HTTP 403 (Sin Hardcoding)
- **Dado** un flujo protegido por Cloudflare Turnstile o firmas de sesión (como DropCDN o Fastream) que devuelve `HTTP 403 Forbidden` al reproductor nativo de la TV Box,
- **Cuando** el receptor notifica `ERROR_FORBIDDEN_403` con la URL del flujo fallido,
- **Entonces** el teléfono emisor:
  1. Inicia automáticamente su micro-servidor proxy HTTP local (`LocalStreamProxyServer` en puerto `8092`).
  2. Inyecta de forma transparente las cookies de sesión de Chromium (`CookieManager`) y el `User-Agent` del móvil.
  3. Reenvía el comando de reproducción con la URL del proxy local (`http://<IP_CELULAR>:8092/master.m3u8?sid=...`).
  4. Muestra un aviso en el móvil: `🔒 Servidor protegido: Transmitiendo vía teléfono. No desconectes WiFi.`
  5. La TV Box reanuda y reproduce el flujo de inmediato sin errores.

### Criterio 3: Soporte para Transmisiones en Vivo (HLS)
- **Dado** un evento o canal en vivo protegido (fútbol, TV en directo) en formato `.m3u8`,
- **Cuando** se enruta a través del proxy local,
- **Entonces** el proxy reescribe dinámicamente las listas de reproducción y entrega los fragmentos `.ts`/`.m4s` en tiempo real a la TV Box por la red LAN con latencia imperceptible ($\le 2\text{ ms}$).

### Criterio 4: Guardado Universal en Favoritos / Ver Más Tarde con Advertencia Visual
- **Dado** una película o stream protegido que ha requerido o requiere proxy,
- **Cuando** el usuario abre la hoja de guardar ("Guardar en Lista", "Ver más tarde" o "⭐ Favoritos"),
- **Entonces** el sistema:
  1. Permite guardar la película exactamente igual que cualquier otro contenido.
  2. Almacena la URL original real (no la IP efímera de la LAN) junto con el indicador `requiresProxy = true`.
  3. Despliega una tarjeta de advertencia visible:
     `⚠️ Contenido con protección web: Para reproducirse en la TV, el teléfono debe estar encendido y en el mismo WiFi. Si el emisor se desconecta, la película se detendrá.`
  4. Muestra la insignia `🔒` y el subtítulo `🔒 Requiere emisor en WiFi` en las listas del Historial y de Guardados.
