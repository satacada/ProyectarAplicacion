package com.omnicast.receiver.directstream

data class PvrRecordProgress(
    val isRecording: Boolean = false,
    val isPaused: Boolean = false,
    val isMovieDownload: Boolean = false,
    val recordedBytes: Long = 0L,
    val totalExpectedBytes: Long = 0L,
    val progressPct: Int = 0,
    val durationSeconds: Long = 0L,
    val filePath: String = "",
    val channelTitle: String = "",
    val targetFolder: String = "",
    val errorMsg: String? = null,
    val statusNotice: String? = null,
    val isResumable: Boolean = false
)
