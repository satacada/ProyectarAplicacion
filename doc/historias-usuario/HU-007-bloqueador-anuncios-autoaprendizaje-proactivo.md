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
