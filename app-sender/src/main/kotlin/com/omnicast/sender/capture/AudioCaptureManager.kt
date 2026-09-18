package com.omnicast.sender.capture

import android.annotation.SuppressLint
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioPlaybackCaptureConfiguration
import android.media.AudioRecord
import android.media.projection.MediaProjection
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.omnicast.core.domain.ports.IAudioCaptureEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Gestor de captura digital interna de audio a nivel de SO (AudioPlaybackCapture).
 * Captura audio a 48kHz en estéreo sin usar el micrófono del dispositivo.
 */
@RequiresApi(Build.VERSION_CODES.Q)
class AudioCaptureManager(
    private val mediaProjection: MediaProjection,
    private val onAudioChunkCaptured: (ByteArray, Int) -> Unit
) : IAudioCaptureEngine {

    companion object {
        private const val TAG = "AudioCaptureManager"
        private const val SAMPLE_RATE = 48000
    }

    private var audioRecord: AudioRecord? = null
    private var captureJob: Job? = null
    private var isMuted = false

    @SuppressLint("MissingPermission")
    override fun startAudioCapture(sampleRate: Int, isStereo: Boolean) {
        val channelConfig = if (isStereo) AudioFormat.CHANNEL_IN_STEREO else AudioFormat.CHANNEL_IN_MONO
        val encoding = AudioFormat.ENCODING_PCM_16BIT

        val minBufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, channelConfig, encoding)
        val bufferSize = (minBufferSize * 2).coerceAtLeast(4096)

        val config = AudioPlaybackCaptureConfiguration.Builder(mediaProjection)
            .addMatchingUsage(AudioAttributes.USAGE_MEDIA)
            .addMatchingUsage(AudioAttributes.USAGE_GAME)
            .addMatchingUsage(AudioAttributes.USAGE_UNKNOWN)
            .build()

        audioRecord = AudioRecord.Builder()
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(encoding)
                    .setSampleRate(SAMPLE_RATE)
                    .setChannelMask(channelConfig)
                    .build()
            )
            .setBufferSizeInBytes(bufferSize)
            .setAudioPlaybackCaptureConfig(config)
            .build()

        audioRecord?.startRecording()
        Log.i(TAG, "Captura digital de audio iniciada a ${SAMPLE_RATE}Hz estéreo")

        captureJob = CoroutineScope(Dispatchers.IO).launch {
            val buffer = ByteArray(1024)
            while (isActive && audioRecord?.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                val readBytes = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                if (readBytes > 0 && !isMuted) {
                    onAudioChunkCaptured(buffer, readBytes)
                } else {
                    kotlinx.coroutines.delay(20)
                }
            }
        }
    }

    override fun setMuted(isMuted: Boolean) {
        this.isMuted = isMuted
    }

    override fun stopAudioCapture() {
        captureJob?.cancel()
        try {
            audioRecord?.stop()
            audioRecord?.release()
        } catch (e: Exception) {
            Log.e(TAG, "Error deteniendo AudioRecord", e)
        }
        audioRecord = null
        Log.i(TAG, "Captura de audio finalizada")
    }
}
