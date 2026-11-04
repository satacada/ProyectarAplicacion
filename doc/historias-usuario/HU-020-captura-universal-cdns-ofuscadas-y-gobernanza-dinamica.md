# HU-020: Captura Universal de CDNs Ofuscadas y Gobernanza Dinámica por Datos

**Estado:** Aprobada y Validada  
**Fecha de Implementación:** 2026-11-04  
**Versión de Producción:** v3.5.89 (Sender: 340 / Receiver: 323)  
**Módulos Clave:** `:core:domain`, `:app-sender`  
**Referencia Técnica:** [ADR-018](file:///home/sataca/Descargas/APLICACIONES/proyectarAplicacion/doc/adr/ADR-018-deteccion-dual-tenaza-red-dom-y-validador-estructural-cdns.md)

---

## 1. Descripción de la Historia de Usuario

**Como** usuario de OmniCast Pro que navega en portales de cine y series (Cinecalidad, Pelisplus, Repelisplus, Doodstream),  
**Quiero** que la aplicación capture de inmediato el video de la película al pulsar Play dentro de cualquier reproductor incrustado en un `<iframe>`,  
**Para que** el selector de casteo se abra automáticamente con las calidades disponibles y la película se transmita a mi Android TV / TV Box sin errores `HTTP 403 Forbidden` ni bloqueos por rotación de dominios en las CDNs.

---

## 2. Criterios de Aceptación Inviolables

1. **Captura en Iframes Cross-Origin:**
   * Al reproducirse un video dentro de un `<iframe>` de terceros (ej. PlayMogo), `shouldInterceptRequest` de `BrowserWebView.kt` debe interceptar la solicitud HTTP de streaming real emitida por la CDN (`cloudatacdn.com`, `doodcdn.co`, etc.).
   * El contador de videos debe incrementarse de `0` a `>= 1` inmediatamente tras el inicio de la decodificación.

2. **Detección Binaria Estructural (Sin Dominios Hardcodeados):**
   * `StreamCandidateValidator.isBinaryMediaStream()` debe certificar flujos que contengan tokens firmados y marcas de expiración con virgulilla (`~` + `token=` + `expiry=`), reconociendo el formato criptográfico de entrega aunque el subdominio de la CDN rote dinámicamente.

3. **Inyección de Cabeceras Legítimas:**
   * La cabecera `Referer` capturada desde el contexto del iframe (ej. `https://playmogo.com/`) debe adjuntarse al `MediaStream` para ser enviada a la TV Box, garantizando respuesta `HTTP 200 OK` en lugar de `HTTP 403 Forbidden`.

4. **Persistencia Dinámica en SQLite y Nube (Supabase):**
   * El host verificado se guarda en `omnicast_stream_profiles.db` (`StreamProfileDatabase.kt`) para reconocimiento en 0 ms.
   * Tras 2 minutos de reproducción continua, `CloudStreamRuleSyncManager.reportVerifiedSuccess()` reporta anónimamente el servidor a Supabase para sincronización comunitaria.

---

## 3. Pruebas de Campo y Aprobación

* **Entorno de Prueba:** Dispositivo móvil Xiaomi HyperOS (Android 15) conectado a TV Box Skyworth FlowBox-F2 (Android 14).
* **Portales Validados:** Cinecalidad (`cinecalidad.men`), Pelisplus (`pelisplus.hair`), Doodstream / PlayMogo.
* **Resultado:** Detección de stream `uha211.cloudatacdn.com/.../xmobbs6ryg~1mD7KEshPZ?token=...` en menos de 200 ms tras pulsar Play. Despacho exitoso a TV Box sin degradación de los flujos ya probados (YouTube, Fastream, Screen Mirroring).
