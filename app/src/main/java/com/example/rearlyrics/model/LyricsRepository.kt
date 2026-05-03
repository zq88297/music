package com.example.rearlyrics.model

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

object LyricsRepository {
    private val _uiState = MutableStateFlow(AppUiState())
    val uiState: StateFlow<AppUiState> = _uiState

    fun updatePlayback(
        packageName: String?,
        title: String?,
        artist: String?,
        lyricCandidate: String?,
        metadataDump: List<String> = emptyList(),
        extrasDump: List<String> = emptyList(),
        detectedLyricCandidates: List<String> = emptyList(),
    ) {
        _uiState.update {
            it.copy(
                currentPackage = packageName,
                trackTitle = title,
                artist = artist,
                sessionLyrics = lyricCandidate.orEmpty(),
                metadataDump = metadataDump,
                extrasDump = extrasDump,
                detectedLyricCandidates = detectedLyricCandidates,
            )
        }
    }

    fun setManualLyrics(text: String) {
        _uiState.update { it.copy(manualLyrics = text) }
    }

    fun setDisplayAvailability(available: Boolean) {
        _uiState.update { it.copy(hasPresentationDisplay = available) }
    }

    fun updateDisplays(
        hasPresentationDisplay: Boolean,
        activeDisplayName: String?,
        displaySummaries: List<String>,
    ) {
        _uiState.update {
            it.copy(
                hasPresentationDisplay = hasPresentationDisplay,
                activeDisplayName = activeDisplayName,
                displaySummaries = displaySummaries,
                displayError = null,
            )
        }
    }

    fun setDisplayError(message: String) {
        _uiState.update { it.copy(displayError = message) }
    }
}
