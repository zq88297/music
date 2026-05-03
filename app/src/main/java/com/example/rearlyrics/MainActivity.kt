package com.example.rearlyrics

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.rearlyrics.display.RearDisplayController
import com.example.rearlyrics.model.AppUiState
import com.example.rearlyrics.model.LyricsRepository
import com.example.rearlyrics.overlay.LyricsOverlayService
import com.example.rearlyrics.ui.theme.RearLyricsTheme

class MainActivity : ComponentActivity() {
    private lateinit var rearDisplayController: RearDisplayController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        rearDisplayController = RearDisplayController(this)
        RearDisplayController.refreshAvailability(this)
        enableEdgeToEdge()

        setContent {
            RearLyricsTheme {
                val state by LyricsRepository.uiState.collectAsState()
                LaunchedEffect(state) {
                    rearDisplayController.sync(state)
                }
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppScreen(
                        state = state,
                        onOpenNotificationSettings = {
                            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                        },
                        onOpenOverlaySettings = {
                            startActivity(
                                Intent(
                                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                    android.net.Uri.parse("package:$packageName"),
                                ),
                            )
                        },
                        onManualLyricsChanged = LyricsRepository::setManualLyrics,
                        onRefreshDisplays = { RearDisplayController.refreshAvailability(this) },
                        onMirrorRequest = { rearDisplayController.showLyrics(state) },
                        onCloseMirrorRequest = rearDisplayController::dismissLyrics,
                        onStartOverlay = { LyricsOverlayService.start(this) },
                        onStopOverlay = { LyricsOverlayService.stop(this) },
                    )
                }
            }
        }
    }

    override fun onDestroy() {
        rearDisplayController.dismissLyrics()
        super.onDestroy()
    }
}

@Composable
private fun AppScreen(
    state: AppUiState,
    onOpenNotificationSettings: () -> Unit,
    onOpenOverlaySettings: () -> Unit,
    onManualLyricsChanged: (String) -> Unit,
    onRefreshDisplays: () -> Unit,
    onMirrorRequest: () -> Unit,
    onCloseMirrorRequest: () -> Unit,
    onStartOverlay: () -> Unit,
    onStopOverlay: () -> Unit,
) {
    var manualText by remember(state.manualLyrics) { mutableStateOf(state.manualLyrics) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF08070C), Color(0xFF1B1230), Color(0xFF0F2B36)),
                ),
            )
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = "Rear Lyrics",
            style = MaterialTheme.typography.headlineMedium,
            color = Color.White,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = "监听手机正在播放的音乐信息，并尝试把歌词投到小米背屏。如果系统没有把背屏暴露成副屏，APP 会先显示同样的预览内容，方便真机适配。",
            color = Color(0xFFE2DDF7),
            lineHeight = 22.sp,
        )

        StatusCard(state)

        if (state.displayError != null) {
            ErrorCard(state.displayError)
        }

        Card {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("权限与控制", style = MaterialTheme.typography.titleMedium)
                Text(
                    text = "1. 打开通知读取权限。2. 如果要走酷狗/网易云类似方案，再打开悬浮窗权限。3. 播放音乐并选择投放方式。",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row {
                    Button(onClick = onOpenNotificationSettings) {
                        Text("开启通知监听")
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Button(onClick = onMirrorRequest) {
                        Text("投到背屏")
                    }
                }
                Button(onClick = onCloseMirrorRequest) {
                    Text("关闭背屏显示")
                }
                Button(onClick = onRefreshDisplays) {
                    Text("刷新副屏列表")
                }
                Row {
                    Button(onClick = onOpenOverlaySettings) {
                        Text("开启悬浮歌词权限")
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Button(onClick = onStartOverlay) {
                        Text("启动悬浮歌词")
                    }
                }
                Button(onClick = onStopOverlay) {
                    Text("关闭悬浮歌词")
                }
            }
        }

        Card {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("手动歌词兜底", style = MaterialTheme.typography.titleMedium)
                Text(
                    text = "很多播放器不会公开实时歌词。拿不到时，可以先把歌词粘贴到这里，验证背屏展示链路。",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                BasicTextField(
                    value = manualText,
                    onValueChange = {
                        manualText = it
                        onManualLyricsChanged(it)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .background(Color(0xFFF4F1FA), RoundedCornerShape(16.dp))
                        .padding(14.dp),
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = Color(0xFF221B2D)),
                    decorationBox = { innerTextField ->
                        if (manualText.isBlank()) {
                            Text("把歌词粘贴到这里……", color = Color(0xFF7F738F))
                        }
                        innerTextField()
                    },
                )
            }
        }

        Card {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("背屏预览", style = MaterialTheme.typography.titleMedium)
                RearScreenPreview(text = state.displayLyrics)
            }
        }

        DebugCard(state)
    }
}

@Composable
private fun StatusCard(state: AppUiState) {
    Card {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("当前状态", style = MaterialTheme.typography.titleMedium)
            StatusLine("播放器", state.currentPackage ?: "未检测到")
            StatusLine("歌曲", state.trackTitle ?: "未知")
            StatusLine("歌手", state.artist ?: "未知")
            StatusLine("副屏", if (state.hasPresentationDisplay) "已检测到可投屏副屏" else "未检测到系统副屏，使用预览模式")
            StatusLine("目标显示", state.activeDisplayName ?: "未命中")
            StatusLine("背屏错误", state.displayError ?: "无")
            StatusLine("歌词来源", state.lyricSourceLabel)
        }
    }
}

@Composable
private fun StatusLine(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            value,
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun ErrorCard(message: String) {
    Card {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("背屏拦截提示", style = MaterialTheme.typography.titleMedium)
            Text(
                text = message,
                color = MaterialTheme.colorScheme.error,
                lineHeight = 22.sp,
            )
            Text(
                text = "这通常说明系统虽然暴露了一个副屏对象，但不允许普通第三方应用往这个窗口类型上直接挂 Presentation。APP 仍会保留主界面的背屏预览，方便继续验证歌词获取。",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun RearScreenPreview(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
            .background(Color.Black, RoundedCornerShape(28.dp))
            .padding(20.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text.ifBlank { "背屏歌词会显示在这里" },
            color = Color.White,
            style = MaterialTheme.typography.headlineSmall,
            lineHeight = 34.sp,
        )
    }
}

@Composable
private fun DebugCard(state: AppUiState) {
    Card {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("调试信息", style = MaterialTheme.typography.titleMedium)
            DebugSection(
                title = "显示设备",
                lines = state.displaySummaries.ifEmpty { listOf("没有发现任何额外显示设备") },
            )
            HorizontalDivider()
            DebugSection(
                title = "歌词候选",
                lines = state.detectedLyricCandidates.ifEmpty { listOf("当前没有命中的歌词候选文本") },
            )
            HorizontalDivider()
            DebugSection(
                title = "MediaMetadata",
                lines = state.metadataDump.ifEmpty { listOf("当前播放器没有暴露 metadata 字符串字段") },
            )
            HorizontalDivider()
            DebugSection(
                title = "Controller Extras",
                lines = state.extrasDump.ifEmpty { listOf("当前控制器没有 extras 字段") },
            )
        }
    }
}

@Composable
private fun DebugSection(title: String, lines: List<String>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.titleSmall)
        lines.forEach { line ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0x14FFFFFF), RoundedCornerShape(12.dp))
                    .padding(10.dp),
            ) {
                Text(
                    text = line,
                    color = Color(0xFFF3F0FF),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}
