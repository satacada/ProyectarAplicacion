# ADR-017: Sistema Inteligente de Grabación PVR y Almacenamiento Seguro con Blindaje de 2.0 GB

## Estado
Aprobado e Implementado (v3.5.59)

## Contexto
En dispositivos Android TV Box y Smart TVs con almacenamiento flash eMMC reducido (usualmente 8 GB a 16 GB brutos, con 1.5 GB a 3 GB libres reales), la grabación de transmisiones en vivo sin supervisión conllevaba el riesgo crítico de saturar completamente la partición `/data`, provocando el bloqueo del sistema operativo en bucle de arranque (*bootloop* o *brick*). Asimismo, la opción de descarga de video se encontraba oculta dentro de la tuerca ⚙️ de ajustes de reproducción, reduciendo la ergonomía de acceso directo tanto en la TV como desde el control remoto móvil.

## Decisión de Arquitectura
1. **Reserva Sagrada de Seguridad Inviolable:**
   - Memoria Interna: Se reserva obligatoriamente un piso sagrado intocable de **2.0 GB** libres (`INTERNAL_SAFETY_RESERVE_BYTES = 2.0 * 1024 * 1024 * 1024L`). Si la memoria interna tiene $\le 2.0\text{ GB}$, se bloquea la grabación en esa unidad y se orienta al usuario a conectar una memoria USB.
   - Memoria USB / Externa: Se reserva un piso de **500 MB** libres (`EXTERNAL_USB_SAFETY_RESERVE_BYTES = 500 * 1024 * 1024L`).
2. **Fórmula de Espacio Útil:**
   $$\text{Espacio Útil} = \max(0, \text{Espacio Libre Real} - \text{Reserva de Seguridad})$$
3. **Estimador Preventivo de Minutos y Tasa de Bits:**
   - Se calculan los minutos seguros máximos antes de iniciar según la tasa estimada del flujo (1080p: ~26.2 MB/min, 720p: ~15.0 MB/min, 480p: ~7.5 MB/min).
4. **Temporizador Desatendido con Auto-Tope (Modo "Me voy de casa"):**
   - El temporizador regresivo (`TvRecordTimerManager`) limita automáticamente la duración elegida si excede el tiempo seguro de la unidad, garantizando siempre la reserva de 2.0 GB. Al finalizar, cierra el archivo, corta los sockets de red y pasa la TV Box a Standby en reposo.
5. **Supervisor Activo en Caliente (Storage Watchdog cada 5s):**
   - `TvStorageSafetyManager.startWatchdog` supervisa la unidad cada 5 segundos durante la grabación activa. Ante riesgo de desborde, finaliza de inmediato la captura de forma ordenada, asegurando que el video guardado sea 100% reproducible y no se corrompa.
6. **Acceso Directo [📥 Guardar / 🔴 REC]:**
   - Se retira la opción de descarga de la tuerca de ajustes y se ubica un botón directo en la barra de controles de la TV y del celular. Durante la grabación en vivo, se despliega la insignia viva `[ 🔴 REC 00:14:22 ⏹ ]` con soporte de pausa por escenas acumulando en el mismo archivo.

## Consecuencias
- Cero riesgo de colapso de almacenamiento o daño a la TV Box por grabaciones desatendidas.
- Ergonomía directa y unificada para guardar películas (VOD) y grabar canales en vivo (PVR 1x).
- Cumplimiento de Clean Architecture y límite estricto de <= 300 líneas por archivo.
