package com.omnicast.sender.ui.components

import com.omnicast.core.domain.models.StreamProfile

data class ZoomSliderStep(val id: String, val label: String)

val ZOOM_STEPS = listOf(
    ZoomSliderStep("ZOOM_130", "130%"),
    ZoomSliderStep("ZOOM_115", "115%"),
    ZoomSliderStep("FIT_100", "Fit 100%"),
    ZoomSliderStep("FILL_16_9", "16:9"),
    ZoomSliderStep("ZOOM_85", "85%"),
    ZoomSliderStep("ZOOM_75", "75%")
)

data class ProfileOption(
    val profile: StreamProfile,
    val displayLabel: String,
    val infoTip: String
)

val PROFILE_OPTIONS = listOf(
    ProfileOption(
        StreamProfile.MAX_60FPS,
        "1280p@60fps - Experimental",
        "Alta exigencia de CPU/GPU. Para teléfonos de gama alta con señal Wi-Fi 5GHz limpia."
    ),
    ProfileOption(
        StreamProfile.BALANCED_30FPS,
        "1280p@30fps - Estable",
        "Configuración recomendada. Máxima estabilidad de transmisión sin cortes."
    ),
    ProfileOption(
        StreamProfile.CINEMA_1080P_30FPS,
        "1080p@30fps - Cine",
        "Máxima definición y nitidez visual para ver películas y series."
    ),
    ProfileOption(
        StreamProfile.FLUID_45FPS,
        "720p@45fps - Fluido",
        "Excelente balance entre velocidad de refresco y bajo consumo de red."
    ),
    ProfileOption(
        StreamProfile.ULTRA_60FPS,
        "540p@60fps - 60fps",
        "60 cuadros por segundo reales. Ideal para juegos y partidos de fútbol dinámicos."
    )
)
