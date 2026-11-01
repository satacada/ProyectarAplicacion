package com.omnicast.receiver.directstream

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Locale

data class TvRecordTimerState(
    val isRunning: Boolean = false,
    val remainingSeconds: Long = 0L,
    val totalSeconds: Long = 0L,
    val isAutoCapped: Boolean = false,
    val autoCapMessage: String? = null
)

/**
 * Gestor del Temporizador Desatendido para Grabación PVR (Modo "Me voy de casa").
 * Aplica Auto-Tope preventivo de seguridad y notifica al finalizar para apagar a Standby.
 */
object TvRecordTimerManager {

    private val _timerState = MutableStateFlow(TvRecordTimerState())
    val timerState: StateFlow<TvRecordTimerState> = _timerState.asStateFlow()

    @Volatile
    private var timerJob: Job? = null

    /**
     * Aplica el Auto-Tope Preventivo según el espacio útil disponible.
     * Retorna el par (minutosEfectivos, mensajeDeAviso).
     */
    fun calculateAutoCappedMinutes(requestedMinutes: Int, maxSafeMinutes: Long): Pair<Int, String?> {
        if (requestedMinutes <= 0) return 0 to null // Modo continuo
        if (maxSafeMinutes > 0L && requestedMinutes.toLong() > maxSafeMinutes) {
            val capped = maxSafeMinutes.toInt().coerceAtLeast(1)
            val msg = "⚠️ El temporizador se ha limitado a $capped minutos para garantizar la reserva de seguridad."
            return capped to msg
        }
        return requestedMinutes to null
    }

    fun startTimer(
        durationMinutes: Int,
        scope: CoroutineScope,
        autoCapMessage: String? = null,
        onFinished: suspend () -> Unit
    ) {
        cancelTimer()
        if (durationMinutes <= 0) {
            _timerState.value = TvRecordTimerState(isRunning = true, remainingSeconds = -1L, totalSeconds = -1L)
            return
        }

        val totalSec = durationMinutes.toLong() * 60L
        _timerState.value = TvRecordTimerState(
            isRunning = true,
            remainingSeconds = totalSec,
            totalSeconds = totalSec,
            isAutoCapped = autoCapMessage != null,
            autoCapMessage = autoCapMessage
        )

        timerJob = scope.launch(Dispatchers.Default) {
            var left = totalSec
            while (isActive && left > 0L) {
                delay(1000L)
                left--
                _timerState.value = _timerState.value.copy(remainingSeconds = left)
            }
            if (isActive && left <= 0L) {
                _timerState.value = _timerState.value.copy(isRunning = false, remainingSeconds = 0L)
                onFinished()
            }
        }
    }

    fun cancelTimer() {
        timerJob?.cancel()
        timerJob = null
        _timerState.value = TvRecordTimerState(isRunning = false, remainingSeconds = 0L)
    }

    fun formatRemaining(seconds: Long): String {
        if (seconds < 0L) return "Continuo"
        val m = seconds / 60L
        val s = seconds % 60L
        val h = m / 60L
        val rm = m % 60L
        return if (h > 0L) {
            String.format(Locale.US, "%02d:%02d:%02d", h, rm, s)
        } else {
            String.format(Locale.US, "%02d:%02d", rm, s)
        }
    }
}
