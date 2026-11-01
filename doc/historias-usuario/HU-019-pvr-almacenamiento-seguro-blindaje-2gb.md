# HU-019: Sistema Inteligente de Grabación PVR y Almacenamiento Seguro con Blindaje de 2.0 GB

**Estado:** 🟢 Aprobado / Implementado  
**Versión:** v3.5.59  
**Módulos Afectados:** `:app-receiver`, `:app-sender`

---

## 1. Descripción
Como usuario de OmniCast en Android TV, deseo poder grabar emisiones de televisión en vivo (PVR 1x) y descargar películas (VOD) de manera desatendida y segura, garantizando que el sistema operativo de la TV Box jamás sufra un colapso por falta de espacio en disco (brick), disponiendo de un temporizador con auto-tope y supervisor en caliente (*watchdog*).

---

## 2. Criterios de Aceptación Inviolables

1. **Reserva Sagrada de Seguridad:**
   - En Memoria Interna: Se preserva un piso intocable de **2.0 GB** libres fijos (`INTERNAL_SAFETY_RESERVE_BYTES = 2,147,483,648L`). Si el espacio libre es $\le 2.0\text{ GB}$, se bloquea la grabación informando conectar un USB.
   - En Unidades Externas USB: Se preserva un piso sagrado de **500 MB** libres fijos (`EXTERNAL_USB_SAFETY_RESERVE_BYTES = 524,288,000L`).
2. **Estimador Preventivo de Minutos Máximos:**
   - 1080p Full HD: ~3,500 kbps ($\approx 26.2\text{ MB/min}$).
   - 720p HD: ~2,000 kbps ($\approx 15.0\text{ MB/min}$).
   - 480p SD: ~1,000 kbps ($\approx 7.5\text{ MB/min}$).
   - Cálculo en tiempo real antes de iniciar la captura.
3. **Temporizador Desatendido con Auto-Tope (Modo "Me voy de casa"):**
   - Botones rápidos D-Pad: `[ 30m ]`, `[ 45m ]`, `[ 60m ]`, `[ 90m ]`, `[ 120m ]`, `[ Continuo ]`.
   - Auto-recorte preventivo de minutos si el usuario selecciona una duración mayor a la permitida por el espacio útil.
   - Al finalizar, cierre limpio de archivo, corte de sockets de red y transición automática a Standby.
4. **Supervisor Activo en Caliente (Storage Watchdog cada 5s):**
   - Monitoreo periódico del espacio remanente cada 5 segundos durante la grabación.
   - Si se roza el umbral de reserva, detiene la captura inmediatamente y escribe el final del archivo sin corrupción.
5. **Acceso Directo en el Reproductor:**
   - Botón directo `[ 📥 Guardar / 🔴 REC ]` en la barra de controles de TV y del celular.
   - Retiro de la opción de descarga de la tuerca ⚙️ de ajustes.
   - Insignia viva `[ 🔴 REC 00:14:22 ⏹ ]` durante la captura en vivo con soporte de pausa/reanudación por escenas.

---

## 3. Pruebas Unitarias Asociadas
- `TvStorageSafetyManagerTest`: 100% aprobado verificando umbrales de 2.0 GB y 500 MB, estimación de minutos por bitrate y lógica de auto-tope.
