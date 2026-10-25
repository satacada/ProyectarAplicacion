# HU-007: Bloqueador de Anuncios con Autoaprendizaje Proactivo y Resiliencia de Flujos

## 1. DESCRIPCIÓN DEL REQUERIMIENTO
Como usuario del navegador integrado en OmniCast,
quiero que el bloqueador de anuncios aprenda de forma inmediata y proactiva de cualquier intento de publicidad o redirección foránea sin depender únicamente de listas remotas,
para que, si un anuncio intenta abrirse una vez, quede registrado permanentemente en la base de datos local y quede bloqueado de raíz en todas las interacciones futuras;
y además, quiero que los flujos de video detectados que se están reproduciendo activamente nunca se borren ni se muestren en "cero videos" si una comprobación previa de red recibe restricciones de CDN (HTTP 403).

---

## 2. CRITERIOS DE ACEPTACIÓN (CONTRATO INAMOVIBLE)

### Criterio 1: Autoaprendizaje Proactivo en Caliente (Zero-Day Ad Host Learning)
- **Dado** que el usuario navega en una página de películas, series o streaming multimedia (`inMovie == true` o con streams detectados),
- **Cuando** un script o capa invisible (click-shield / fake player) intenta secuestrar la navegación hacia un dominio externo (`targetHost != curHost`),
- **Entonces** el sistema debe:
  1. Interceptar y bloquear la navegación inmediatamente, sin importar si `hasGesture()` es verdadero (el toque del usuario iba dirigido a reproducir el video).
  2. Guardar dicho `targetHost` directamente en la base de datos SQLite local (`AdBlockDatabase`) de forma asíncrona pero instantánea.
  3. No permitir que dicho dominio vuelva a cargarse en ninguna solicitud posterior de la aplicación.
  4. Emitir un Toast informativo: `🚫 Publicidad bloqueada y aprendida: <dominio>`.

### Criterio 2: Neutralización de Capas Trampa en Reproductores
- **Dado** que un reproductor embebido (Fastream, Dropload, Streamtape, etc.) coloca un enlace transparente `<a>` o capa con z-index sobre el botón de Play,
- **Cuando** el usuario toca el reproductor,
- **Entonces** el script de protección reactiva en fase de captura cancela el evento sobre enlaces foráneos que no pertenezcan al host de la película ni a proveedores de video legítimos, removiendo el elemento trampa y permitiendo que el toque llegue al video subyacente.

### Criterio 3: Resiliencia de Flujos Detectados (No Descarte por HTTP 403 de Sonda)
- **Dado** que un flujo multimedia (.m3u8 / .mp4) fue interceptado por la red o por el reproductor activo del WebView,
- **Cuando** la sonda previa (`StreamManifestProbe`) recibe HTTP 403 Forbidden debido a restricciones de CDN (tokens efímeros, firmas TLS o validaciones estrictas de cabeceras),
- **Entonces** el sistema:
  1. NUNCA debe borrar el flujo de la lista de `detectedStreams`.
  2. NUNCA debe mostrar "0 videos" si el video está activo en el navegador.
  3. Debe generar automáticamente una opción de calidad de respaldo ("▶ Flujo Directo Nativo") con las cabeceras originales del navegador, permitiendo al usuario transmitirlo a la TV Box sin interrupción.

### Criterio 4: Inmunidad Colectiva Descentralizada (Crowd-sourced Cloud Learning)
- **Dado** que un usuario detecta y aprende un nuevo dominio publicitario emergente en su SQLite local,
- **Cuando** el dominio se almacena con éxito en `TABLE_DOMAINS`,
- **Entonces** el sistema:
  1. Despacha en segundo plano y de forma asíncrona dicho dominio a la base de datos centralizada de Supabase (`omnicast_system_logs` con evento `ADBLOCK_LEARNED_DOMAIN`).
  2. En el inicio del navegador o sincronización periódica (`syncCrowdLearnedDomains`), todos los demás dispositivos consultan los dominios descubiertos por otros usuarios y los incorporan a su SQLite local.
  3. Esto confiere inmunidad colectiva previa a toda la comunidad de usuarios antes de que el nuevo anuncio llegue a desplegarse en sus pantallas.

### Criterio 5: Supresión Nativa de Popups sin WebViews Temporales (Anti-OOM) y Calibración de Gestos (v3.4.59)
- **Dado** que un sitio de streaming con alta densidad publicitaria ejecuta llamadas repetitivas a `window.open()` (popups / popunders),
- **Cuando** se activa el evento `WebChromeClient.onCreateWindow`,
- **Entonces** el sistema:
  1. Retorna inmediatamente `false` de forma nativa para suprimir la creación de la ventana emergente sin instanciar ningún `WebView` temporal.
  2. Garantiza un consumo de 0 MB de memoria RAM adicional, impidiendo que el heap de la aplicación alcance el límite de 512 MB y previniendo el error `OutOfMemoryError`.
  3. En `shouldOverrideUrlLoading`, respeta los clics intencionales del usuario mediante `r.hasGesture() == true`, permitiendo navegar normalmente hacia enlaces de películas o servidores externos que no sean anuncios comprobados.

