package com.omnicast.core.domain.ports

/**
 * Puerto de salida para el filtrado temprano de telemetría, analíticas, balizas y recursos no reproducibles.
 */
interface IStreamTelemetryFilterPort {
    /**
     * Retorna true si la URL corresponde a una petición de estadísticas, telemetría o recurso espurio.
     */
    fun isTelemetryOrGarbage(url: String): Boolean
}
