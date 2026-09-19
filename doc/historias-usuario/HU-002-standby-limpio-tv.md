# 📌 HU-002: Interfaz Limpia y Standby en TV Receiver (Estilo Seekee TV)

- **Estado:** 🟢 Aprobado / En Producción (v3.28)
- **Módulo Principal:** `:app-receiver` (`TvStandbyCleanScreen`, `TvCleanComponents`, `TvReceiverActivity`)
- **Última Actualización:** 2026-09-19 (v3.28 - Diseño Seekee TV con Dock Inferior)

---

## 1. Descripción de la Historia
Como usuario de OmniCast en Android TV Box,  
Quiero una interfaz de bienvenida y espera (standby) moderna, limpia y despejada cuando no hay transmisión activa,  
Para poder ver mis listas y videos recientes organizados en carpetas con control remoto y consultar claramente los datos de conexión (IP y puerto) en la parte inferior.

---

## 2. Criterios de Aceptación Inamovibles

### CA-01: Barra Superior (Top Bar)
- Identidad de la app con el logo `OmniCast TV`.
- Barra de navegación con pestañas: `Recientes` | `Todo` | `Favoritos`.
- Insignia de versión actual visible (ej: `v3.28`).

### CA-02: Cuerpo Central (Carpetas y Elementos)
- Contador de elementos (`1 elemento` / `X elementos`).
- Lista de carpetas limpias con icono amber `📁` (ej: `FromWebSite`, `⭐ Favoritos`), fecha/hora y conteo de videos.
- Navegación completa mediante mando a distancia (D-Pad). Al pulsar OK en una carpeta se expanden sus videos.
- Botón píldora central de ayuda.

### CA-03: Dock Inferior de Conexión
- Tarjeta fija anclada en la parte inferior de la pantalla.
- Indicador de estado: `🟢 Listo para conectar`.
- Dirección IP y puerto local claramente visibles: `$ipAddress:8088`.
- Subtítulo con instrucciones breves de conexión desde el celular.

---

## 3. Historial de Ajustes
- **v3.28:** Reemplazada la interfaz anterior sobrecargada por el diseño limpio modular inspirado en Seekee TV, manteniendo el dock inferior de conexión y separando el código en archivos menores a 300 líneas.
