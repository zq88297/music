package com.example.rearlyrics.display

import android.app.Presentation
import android.content.Context
import android.os.Bundle
import android.view.Display
import android.widget.FrameLayout
import android.widget.TextView
import androidx.core.content.ContextCompat

class RearLyricsPresentation(
    context: Context,
    display: Display,
) : Presentation(context, display) {
    private lateinit var lyricsView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        lyricsView = TextView(context).apply {
            textSize = 22f
            setTextColor(ContextCompat.getColor(context, android.R.color.white))
            setBackgroundColor(ContextCompat.getColor(context, android.R.color.black))
            setPadding(36, 36, 36, 36)
        }

        setContentView(
            FrameLayout(context).apply {
                setBackgroundColor(ContextCompat.getColor(context, android.R.color.black))
                addView(
                    lyricsView,
                    FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT,
                    ),
                )
            },
        )
    }

    fun updateLyrics(text: String) {
        if (::lyricsView.isInitialized) {
            lyricsView.text = text.ifBlank { "暂无歌词" }
        }
    }
}
