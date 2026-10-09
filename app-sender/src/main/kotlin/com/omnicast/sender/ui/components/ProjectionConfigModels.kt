package com.omnicast.sender.ui.components

import androidx.compose.ui.graphics.Color
import com.omnicast.core.domain.models.StreamProfile

data class ZoomSliderStep(val id: String, val label: String)

val ZOOM_STEPS = listOf(
    ZoomSliderStep("ZOOM_130", "130%"),
    ZoomSliderStep("ZOOM_115", "115%"),
    ZoomSliderStep("FIT_100", "Fit 100%"),
    ZoomSliderStep("ZOOM_85", "85%"),
    ZoomSliderStep("ZOOM_75", "75%"),
    ZoomSliderStep("FILL_16_9", "16:9")
)

data class ProfileOption(
    val profile: StreamProfile,
    val resLabel: String,
    val badgeText: String,
    val badgeIsPrimary: Boolean,
    val badgeIsWarning: Boolean,
    val infoTip: String
)

val PROFILE_OPTIONS = listOf(
    ProfileOption(
        profile = StreamProfile.MAX_60FPS,
        resLabel = "1280p @ 60 fps",
        badgeText = "EXPERIMENTAL",
        badgeIsPrimary = false,
        badgeIsWarning = true,
        infoTip = "Alta exigencia de CPU/GPU. Para teléfonos de gama alta con señal Wi-Fi 5GHz limpia."
    ),
    ProfileOption(
        profile = StreamProfile.BALANCED_30FPS,
        resLabel = "1280p @ 30 fps",
        badgeText = "• Estable • Recomendado",
        badgeIsPrimary = true,
        badgeIsWarning = false,
        infoTip = "Configuración recomendada. Máxima estabilidad de transmisión sin cortes."
    ),
    ProfileOption(
        profile = StreamProfile.CINEMA_1080P_30FPS,
        resLabel = "1080p @ 30 fps",
        badgeText = "Cine HD",
        badgeIsPrimary = false,
        badgeIsWarning = false,
        infoTip = "Máxima definición y nitidez visual para ver películas y series."
    ),
    ProfileOption(
        profile = StreamProfile.FLUID_45FPS,
        resLabel = "720p @ 45 fps",
        badgeText = "Fluido",
        badgeIsPrimary = false,
        badgeIsWarning = false,
        infoTip = "Excelente balance entre velocidad de refresco y bajo consumo de red."
    ),
    ProfileOption(
        profile = StreamProfile.ULTRA_60FPS,
        resLabel = "540p @ 60 fps",
        badgeText = "Bajo Consumo",
        badgeIsPrimary = false,
        badgeIsWarning = false,
        infoTip = "60 cuadros por segundo reales. Menor consumo de batería y ancho de banda."
    )
)
