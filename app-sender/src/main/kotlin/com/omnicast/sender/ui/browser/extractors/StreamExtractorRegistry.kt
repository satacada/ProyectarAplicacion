package com.omnicast.sender.ui.browser.extractors

import com.omnicast.core.domain.usecases.DetectMediaStreamUseCase

/**
 * Registro central de adaptadores de extracción para la aplicación Sender.
 * Permite "poner y sacar" cualquier extractor de forma declarativa sin acoplamiento.
 */
object StreamExtractorRegistry {

    val useCase: DetectMediaStreamUseCase by lazy {
        DetectMediaStreamUseCase(
            telemetryFilter = TelemetryFilterAdapter(),
            extractors = listOf(
                // Extractores universales de la web abierta (Estables / Intocables)
                UniversalHlsExtractorAdapter(),
                UniversalMp4ExtractorAdapter(),
                UniversalDashExtractorAdapter(),

                // Extractores modulares por sitio ("poner y sacar")
                OkRuSiteExtractorAdapter(),
                YouTubeExtractorAdapter(),
                DailymotionExtractorAdapter()
            )
        )
    }
}
