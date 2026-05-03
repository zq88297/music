package com.example.rearlyrics.display

import android.app.Activity
import android.content.Context
import android.hardware.display.DisplayManager
import android.view.Display
import android.view.WindowManager
import com.example.rearlyrics.model.AppUiState
import com.example.rearlyrics.model.LyricsRepository

class RearDisplayController(
    private val activity: Activity,
) {
    private var presentation: RearLyricsPresentation? = null
    private var isMirroringEnabled = false

    fun showLyrics(state: AppUiState) {
        isMirroringEnabled = true
        sync(state)
    }

    fun sync(state: AppUiState) {
        if (!isMirroringEnabled) return

        val snapshot = inspectDisplays(activity)
        val display = snapshot.presentationDisplay
        LyricsRepository.updateDisplays(
            hasPresentationDisplay = display != null,
            activeDisplayName = display?.name,
            displaySummaries = snapshot.summaries,
        )

        if (display == null) {
            presentation?.dismiss()
            presentation = null
            return
        }

        val existing = presentation
        if (existing?.display?.displayId == display.displayId) {
            existing.updateLyrics(state.displayLyrics)
            return
        }

        presentation?.dismiss()
        runCatching {
            RearLyricsPresentation(activity, display).also {
                it.show()
                it.updateLyrics(state.displayLyrics)
            }
        }.onSuccess {
            presentation = it
        }.onFailure { throwable ->
            presentation = null
            isMirroringEnabled = false
            val message = when (throwable) {
                is WindowManager.BadTokenException ->
                    "系统拒绝在该副屏创建窗口，当前背屏大概率不允许第三方应用直接投放内容。"
                is WindowManager.InvalidDisplayException ->
                    "副屏当前不可用或已失效，请刷新副屏列表后重试。"
                else ->
                    "背屏显示失败：${throwable.message ?: throwable.javaClass.simpleName}"
            }
            LyricsRepository.setDisplayError(message)
        }
    }

    fun dismissLyrics() {
        isMirroringEnabled = false
        presentation?.dismiss()
        presentation = null
    }

    companion object {
        fun refreshAvailability(context: Context) {
            val snapshot = inspectDisplays(context)
            LyricsRepository.updateDisplays(
                hasPresentationDisplay = snapshot.presentationDisplay != null,
                activeDisplayName = snapshot.presentationDisplay?.name,
                displaySummaries = snapshot.summaries,
            )
        }

        private fun inspectDisplays(context: Context): DisplaySnapshot {
            val manager = context.getSystemService(DisplayManager::class.java)
                ?: return DisplaySnapshot(null, emptyList())
            val displays = manager.displays.toList()
            val presentation = displays.firstOrNull { display ->
                display.displayId != Display.DEFAULT_DISPLAY &&
                    display.flags and Display.FLAG_PRESENTATION == Display.FLAG_PRESENTATION
            }
            val summaries = displays.map { display ->
                "id=${display.displayId} name=${display.name} flags=${display.flags}"
            }
            return DisplaySnapshot(presentation, summaries)
        }
    }

    private data class DisplaySnapshot(
        val presentationDisplay: Display?,
        val summaries: List<String>,
    )
}
