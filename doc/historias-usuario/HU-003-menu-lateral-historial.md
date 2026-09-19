# 📌 HU-003: Menú Lateral Móvil con Historial y Videos Guardados con Tiempo

- **Estado:** 🟢 Aprobado / En Producción (v3.28)
- **Módulo Principal:** `:app-sender` (`SideMediaDrawer`, `SenderMainScreen`, `PlayHistoryManager`)
- **Última Actualización:** 2026-09-19 (v3.28)

---

## 1. Descripción de la Historia
Como usuario de OmniCast en el celular,  
Quiero abrir un menú lateral desplegable que me muestre el historial de videos reproducidos y los videos guardados, con el tiempo exacto donde se quedó la reproducción,  
Para poder retomar mis videos en la TV o volver a abrirlos en la web con un solo toque.

---

## 2. Criterios de Aceptación Inamovibles

### CA-01: Acceso y Pestañas
- Botón hamburguesa `☰` ubicado en la cabecera superior izquierda de la pantalla principal.
- Dos pestañas superiores:
  - `🕒 Historial`: Lista de videos reproducidos recientemente.
  - `⭐ Guardadas`: Lista de videos marcados como favoritos.

### CA-02: Indicador de Posición Exacta
- Cada tarjeta de video muestra claramente el punto donde se quedó:  
  `▶ Se quedó en: mm:ss / mm:ss`.
- El progreso se actualiza en tiempo real mediante la telemetría enviada por la TV Box vía WebSocket (`SignalingMessage.PlaybackStatus`).

### CA-03: Acciones Rápidas
- `▶ TV`: Proyecta el video directamente a la TV Box iniciando en el segundo exacto donde se quedó.
- `🌐 Web`: Abre la página web original del video en el navegador web integrado.
- `✕`: Elimina el elemento del historial.
- `⭐`: Alterna el estado de guardado/favorito.

---

## 3. Historial de Ajustes
- **v3.28:** Integración del menú lateral desplegable con soporte de persistencia local y sincronización bidireccional de estado con la TV.
