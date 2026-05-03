package com.example.rearlyrics.model

data class AppUiState(
    val currentPackage: String? = null,
    val trackTitle: String? = null,
    val artist: String? = null,
    val sessionLyrics: String = "",
    val manualLyrics: String = "",
    val hasPresentationDisplay: Boolean = false,
    val activeDisplayName: String? = null,
    val displayError: String? = null,
    val displaySummaries: List<String> = emptyList(),
    val metadataDump: List<String> = emptyList(),
    val extrasDump: List<String> = emptyList(),
    val detectedLyricCandidates: List<String> = emptyList(),
) {
    val displayLyrics: String
        get() = if (sessionLyrics.isNotBlank()) sessionLyrics else manualLyrics

    val lyricSourceLabel: String
        get() = when {
            sessionLyrics.isNotBlank() -> "播放器元数据/通知"
            manualLyrics.isNotBlank() -> "手动输入"
            else -> "暂无歌词"
        }
}
