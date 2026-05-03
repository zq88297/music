package com.example.rearlyrics.overlay

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.WindowManager
import android.widget.TextView
import androidx.core.app.NotificationCompat
import com.example.rearlyrics.model.LyricsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class LyricsOverlayService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var windowManager: WindowManager? = null
    private var lyricsView: TextView? = null
    private var isAttached = false

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WindowManager::class.java)
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification("悬浮歌词运行中"))
        LyricsRepository.uiState
            .onEach { state ->
                val text = state.displayLyrics.ifBlank { "Rear Lyrics 正在等待歌词" }
                ensureOverlay()
                lyricsView?.text = text
                lyricsView?.alpha = if (state.displayLyrics.isBlank()) 0.75f else 1f
            }
            .launchIn(scope)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> stopSelf()
            ACTION_START, null -> {
                if (!Settings.canDrawOverlays(this)) {
                    LyricsRepository.setDisplayError("没有悬浮窗权限，请先开启“悬浮歌词权限”后再重试。")
                    stopSelf()
                    return START_NOT_STICKY
                }
                ensureOverlay()
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        detachOverlay()
        scope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun ensureOverlay() {
        if (isAttached || !Settings.canDrawOverlays(this)) return

        val wm = windowManager ?: return
        val view = lyricsView ?: TextView(this).apply {
            textSize = 20f
            setTextColor(0xFFFFFFFF.toInt())
            setBackgroundColor(0xAA000000.toInt())
            setPadding(28, 20, 28, 20)
            text = "Rear Lyrics 正在等待歌词"
        }.also { lyricsView = it }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            x = 0
            y = 180
        }

        runCatching {
            wm.addView(view, params)
            isAttached = true
            LyricsRepository.setDisplayError("已切换为悬浮歌词模式。若小米背屏支持把悬浮歌词镜像到背屏，这条链路更接近酷狗/网易云的实现方式。")
        }.onFailure { throwable ->
            LyricsRepository.setDisplayError("创建悬浮歌词失败：${throwable.message ?: throwable.javaClass.simpleName}")
        }
    }

    private fun detachOverlay() {
        val wm = windowManager ?: return
        val view = lyricsView ?: return
        if (!isAttached) return
        runCatching { wm.removeView(view) }
        isAttached = false
    }

    private fun buildNotification(content: String): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Rear Lyrics")
            .setContentText(content)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setOngoing(true)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService(NotificationManager::class.java) ?: return
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Rear Lyrics Overlay",
            NotificationManager.IMPORTANCE_LOW,
        )
        manager.createNotificationChannel(channel)
    }

    companion object {
        private const val CHANNEL_ID = "rear_lyrics_overlay"
        private const val NOTIFICATION_ID = 1001
        private const val ACTION_START = "com.example.rearlyrics.action.START_OVERLAY"
        private const val ACTION_STOP = "com.example.rearlyrics.action.STOP_OVERLAY"

        fun start(context: Context) {
            val intent = Intent(context, LyricsOverlayService::class.java).apply {
                action = ACTION_START
            }
            androidx.core.content.ContextCompat.startForegroundService(context, intent)
        }

        fun stop(context: Context) {
            val intent = Intent(context, LyricsOverlayService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }
}
