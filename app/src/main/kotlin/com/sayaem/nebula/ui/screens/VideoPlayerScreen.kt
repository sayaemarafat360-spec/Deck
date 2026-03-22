package com.sayaem.nebula.ui.screens

import android.app.Activity
import android.app.PictureInPictureParams
import android.content.pm.ActivityInfo
import android.os.Build
import android.util.Rational
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.sayaem.nebula.data.models.Song
import com.sayaem.nebula.ui.theme.*
import android.os.Build
import android.util.Rational
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.sayaem.nebula.data.models.Song
import com.sayaem.nebula.ui.theme.*

@Composable
fun VideoPlayerScreen(
    video: Song,
    player: ExoPlayer,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val activity = context as? Activity

    var showControls   by remember { mutableStateOf(true) }
    var isPlaying      by remember { mutableStateOf(true) }
    var brightness     by remember { mutableStateOf(0.5f) }
    var volume         by remember { mutableStateOf(1.0f) }
    var aspectIdx      by remember { mutableStateOf(0) }
    val aspectOptions  = listOf("16:9", "4:3", "Fit", "Zoom")
    val aspectLabel    = aspectOptions[aspectIdx]
    val resizeModes    = listOf(
        androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT,
        androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIXED_HEIGHT,
        androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT,
        androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_ZOOM,
    )
    var playerViewRef by remember { mutableStateOf<androidx.media3.ui.PlayerView?>(null) }

    // Force landscape
    LaunchedEffect(Unit) {
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    // Restore on exit
    BackHandler {
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onBack()
    }

    // Auto-hide controls
    LaunchedEffect(showControls) {
        if (showControls) {
            kotlinx.coroutines.delay(4000)
            showControls = false
        }
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {

        // Video surface
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    this.player = player
                    useController = false
                    setBackgroundColor(android.graphics.Color.BLACK)
                    playerViewRef = this
                }
            },
            modifier = Modifier.fillMaxSize()
                .clickable { showControls = !showControls }
        )

        // Brightness indicator (left side drag)
        var dragStartY by remember { mutableStateOf(0f) }
        Box(
            modifier = Modifier.fillMaxHeight().fillMaxWidth(0.5f).align(Alignment.CenterStart)
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            val pos   = event.changes.firstOrNull()?.position ?: continue
                            when (event.type) {
                                androidx.compose.ui.input.pointer.PointerEventType.Press ->
                                    dragStartY = pos.y
                                androidx.compose.ui.input.pointer.PointerEventType.Move -> {
                                    val delta = (dragStartY - pos.y) / size.height
                                    brightness = (brightness + delta * 0.5f).coerceIn(0f, 1f)
                                    dragStartY = pos.y
                                }
                                else -> {}
                            }
                        }
                    }
                }
        )

        // Volume indicator (right side drag)
        Box(
            modifier = Modifier.fillMaxHeight().fillMaxWidth(0.5f).align(Alignment.CenterEnd)
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            val pos   = event.changes.firstOrNull()?.position ?: continue
                            when (event.type) {
                                androidx.compose.ui.input.pointer.PointerEventType.Press ->
                                    dragStartY = pos.y
                                androidx.compose.ui.input.pointer.PointerEventType.Move -> {
                                    val delta = (dragStartY - pos.y) / size.height
                                    volume = (volume + delta * 0.5f).coerceIn(0f, 1f)
                                    player.volume = volume
                                    dragStartY = pos.y
                                }
                                else -> {}
                            }
                        }
                    }
                }
        )

        // Real screen brightness via WindowManager
        LaunchedEffect(brightness) {
            activity?.window?.attributes = activity?.window?.attributes?.also {
                it.screenBrightness = brightness.coerceIn(0.01f, 1.0f)
            }
        }

        // Controls overlay
        AnimatedVisibility(visible = showControls,
            enter = fadeIn(), exit = fadeOut()) {
            Box(Modifier.fillMaxSize().background(
                androidx.compose.ui.graphics.Brush.verticalGradient(
                    listOf(Color.Black.copy(0.7f), Color.Transparent,
                           Color.Transparent, Color.Black.copy(0.7f))
                )
            )) {
                // Top bar
                Row(
                    modifier = Modifier.fillMaxWidth().align(Alignment.TopStart)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = {
                        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                        activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                        onBack()
                    }) {
                        Icon(Icons.Filled.ArrowBack, null, tint = Color.White)
                    }
                    Text(video.title, style = MaterialTheme.typography.titleMedium,
                        color = Color.White, fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f).padding(horizontal = 8.dp))

                    // Aspect ratio
                    Box(
                        modifier = Modifier.clip(RoundedCornerShape(8.dp))
                            .background(Color.White.copy(0.15f))
                            .clickable {
                                val idx  = aspectOptions.indexOf(aspectLabel)
                                aspectLabel = aspectOptions[(idx + 1) % aspectOptions.size]
                            }.padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text(aspectLabel, style = MaterialTheme.typography.labelSmall, color = Color.White)
                    }
                    Spacer(Modifier.width(8.dp))

                    // PiP
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        IconButton(onClick = {
                            val params = PictureInPictureParams.Builder()
                                .setAspectRatio(Rational(16, 9)).build()
                            activity?.enterPictureInPictureMode(params)
                        }) {
                            Icon(Icons.Filled.PictureInPicture, null, tint = Color.White)
                        }
                    }
                }

                // Center controls
                Row(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalArrangement = Arrangement.spacedBy(32.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // -10s
                    IconButton(onClick = {
                        player.seekTo((player.currentPosition - 10000).coerceAtLeast(0))
                    }, modifier = Modifier.size(52.dp)) {
                        Icon(Icons.Filled.Replay10, null, tint = Color.White, modifier = Modifier.size(36.dp))
                    }

                    // Play/Pause
                    Box(
                        modifier = Modifier.size(66.dp).clip(CircleShape)
                            .background(Color.White.copy(0.2f))
                            .border(1.5.dp, Color.White.copy(0.5f), CircleShape)
                            .clickable {
                                if (player.isPlaying) player.pause() else player.play()
                                isPlaying = player.isPlaying
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            if (player.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            null, tint = Color.White, modifier = Modifier.size(32.dp)
                        )
                    }

                    // +10s
                    IconButton(onClick = {
                        player.seekTo(player.currentPosition + 10000)
                    }, modifier = Modifier.size(52.dp)) {
                        Icon(Icons.Filled.Forward10, null, tint = Color.White, modifier = Modifier.size(36.dp))
                    }
                }

                // Bottom seek bar
                Column(
                    modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter)
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    val position by produceState(0L) {
                        while (true) {
                            value = player.currentPosition
                            kotlinx.coroutines.delay(500)
                        }
                    }
                    val duration = player.duration.coerceAtLeast(1L)
                    val progress = (position.toFloat() / duration).coerceIn(0f, 1f)

                    Slider(
                        value    = progress,
                        onValueChange = { player.seekTo((it * duration).toLong()) },
                        colors   = SliderDefaults.colors(
                            activeTrackColor   = NebulaViolet,
                            thumbColor         = Color.White,
                            inactiveTrackColor = Color.White.copy(0.3f)
                        )
                    )
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(formatVideoMs(position), style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(0.7f))
                        Text(formatVideoMs(duration), style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(0.7f))
                    }
                }

                // Brightness indicator (top-left)
                Column(modifier = Modifier.align(Alignment.CenterStart).padding(start = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Filled.BrightnessHigh, null, tint = Color.White.copy(0.8f),
                        modifier = Modifier.size(18.dp))
                    Spacer(Modifier.height(4.dp))
                    Box(Modifier.width(4.dp).height(80.dp).clip(RoundedCornerShape(2.dp))
                        .background(Color.White.copy(0.2f))) {
                        Box(Modifier.fillMaxWidth().fillMaxHeight(brightness)
                            .align(Alignment.BottomCenter).background(Color.White.copy(0.8f)))
                    }
                }

                // Volume indicator (top-right)
                Column(modifier = Modifier.align(Alignment.CenterEnd).padding(end = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Filled.VolumeUp, null, tint = Color.White.copy(0.8f),
                        modifier = Modifier.size(18.dp))
                    Spacer(Modifier.height(4.dp))
                    Box(Modifier.width(4.dp).height(80.dp).clip(RoundedCornerShape(2.dp))
                        .background(Color.White.copy(0.2f))) {
                        Box(Modifier.fillMaxWidth().fillMaxHeight(volume)
                            .align(Alignment.BottomCenter).background(Color.White.copy(0.8f)))
                    }
                }
            }
        }
    }
}

private fun formatVideoMs(ms: Long): String {
    val total = ms / 1000
    val h = total / 3600
    val m = (total % 3600) / 60
    val s = total % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
}
