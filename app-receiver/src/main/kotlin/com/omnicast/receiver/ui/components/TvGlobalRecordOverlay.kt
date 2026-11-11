package com.omnicast.receiver.ui.components

import android.widget.Toast
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.omnicast.receiver.directstream.PvrStreamRecorder
import com.omnicast.receiver.directstream.TvRecordTimerManager

/**
 * Overlay global persistente de grabación y descarga para Android TV en modo Standby.
 * Permite seguir supervisando la captura y ofrece parada/descarte fuera del reproductor.
 */
@Composable
fun BoxScope.TvGlobalRecordOverlay(isStandby: Boolean) {
    TvTelemetryOverlay()
    TvVirtualPointerOverlay()

    if (!isStandby) return

    val context = LocalContext.current
    val pvrProgress by PvrStreamRecorder.progress.collectAsState()
    val timerState by TvRecordTimerManager.timerState.collectAsState()
    var showStopConfirm by remember { mutableStateOf(false) }

    if (pvrProgress.isRecording) {
        TvRecFloatingBadge(
            isRecording = true,
            durationSeconds = pvrProgress.durationSeconds,
            remainingSeconds = if (timerState.isRunning && timerState.remainingSeconds > 0) timerState.remainingSeconds else -1L,
            isMovieDownload = pvrProgress.isMovieDownload,
            progressPct = pvrProgress.progressPct,
            modifier = Modifier.align(Alignment.TopEnd).padding(end = 28.dp, top = 28.dp).zIndex(99f),
            onClick = { showStopConfirm = true }
        )

        TvConfirmStopRecordDialog(
            isOpen = showStopConfirm,
            recordedDurationSec = pvrProgress.durationSeconds,
            onKeep = {
                showStopConfirm = false
                PvrStreamRecorder.stopRecording("💾 Grabación conservada")
                Toast.makeText(context, "💾 Grabación conservada exitosamente", Toast.LENGTH_SHORT).show()
            },
            onDiscard = {
                showStopConfirm = false
                PvrStreamRecorder.stopAndDiscardRecording("🗑️ Grabación descartada y eliminada")
                Toast.makeText(context, "🗑️ Grabación descartada y eliminada", Toast.LENGTH_SHORT).show()
            },
            onContinue = {
                showStopConfirm = false
            }
        )
    }
}
