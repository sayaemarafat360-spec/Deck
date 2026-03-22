package com.sayaem.nebula.ui.screens

import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.Offset
import android.app.Activity
import android.app.PictureInPictureParams
import android.content.pm.ActivityInfo
import android.os.Build
import android.util.Rational
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.gestures.rememberTransformableState
import android.content.ContentValues
import android.provider.MediaStore
import android.os.Environment
import android.graphics.Bitmap
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.drawToBitmap
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.sayaem.nebula.data.models.Song
import com.sayaem.nebula.ui.theme.*
import kotlinx.coroutines.delay




@Composable
fun VideoPlayerScreen(
    video: Song,
    player: ExoPlayer?,
    onBack: () -> Unit,
) {
    val context  = LocalContext.current
    val activity = context as? Activity

    if (player == null) {
        Box(Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Filled.Error, null, tint = Color.White.copy(0.5f), modifier = Modifier.size(48.dp))
                Spacer(Modifier.height(12.dp))
                Text("Player loading...", color = Color.White.copy(0.7f))
            }
        }
        // Pinch-to-zoom
    val transformState = rememberTransformableState { zoomChange, offsetChange, _ ->
        videoScale = (videoScale * zoomChange).coerceIn(1f, 4f)
        if (videoScale > 1f) {
            videoOffsetX += offsetChange.x
            videoOffsetY += offsetChange.y
        } else {
            videoOffsetX = 0f; videoOffsetY = 0f
        }
    }

    BackHandler { onBack() }
        return
    }

    var showControls    by remember { mutableStateOf(true) }
    var brightness      by remember { mutableStateOf(0.5f) }
    var volume          by remember { mutableStateOf(player.volume) }
    var isPlaying       by remember { mutableStateOf(player.isPlaying) }
    var aspectIdx       by remember { mutableStateOf(0) }
    var playerViewRef   by remember { mutableStateOf<PlayerView?>(null) }
    var showBrightness  by remember { mutableStateOf(false) }
    var showVolume      by remember { mutableStateOf(false) }
    var seekLabel       by remember { mutableStateOf("") }
    var showSeekLabel   by remember { mutableStateOf(false) }
    var isLocked        by remember { mutableStateOf(false) }
    var showSubtitles   by remember { mutableStateOf(true) }

    // Detect .srt file alongside the video
    val srtPath = remember(video.filePath) {
        val base = video.filePath.substringBeforeLast(".")
        listOf("$base.srt", "$base.SRT").firstOrNull { java.io.File(it).exists() }
    }
    var videoScale      by remember { mutableStateOf(1f) }
    var videoOffsetX    by remember { mutableStateOf(0f) }
    var videoOffsetY    by remember { mutableStateOf(0f) }
    var videoSpeed      by remember { mutableStateOf(1.0f) }
    var showSpeed       by remember { mutableStateOf(false) }
    var swipeDownY      by remember { mutableStateOf(0f) }

    val aspectLabels = listOf("16:9", "4:3", "Fit", "Zoom")
    val aspectModes  = listOf(
        AspectRatioFrameLayout.RESIZE_MODE_FIT,
        AspectRatioFrameLayout.RESIZE_MODE_FIXED_HEIGHT,
        AspectRatioFrameLayout.RESIZE_MODE_FIT,
        AspectRatioFrameLayout.RESIZE_MODE_ZOOM,
    )

    // Force landscape, keep screen on
    LaunchedEffect(Unit) {
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    // Real screen brightness
    LaunchedEffect(brightness) {
        activity?.window?.attributes = activity?.window?.attributes?.also {
            it.screenBrightness = brightness.coerceIn(0.01f, 1.0f)
        }
    }

    // Auto-hide controls after 3s
    LaunchedEffect(showControls, isLocked) {
        if (showControls && !isLocked) {
            delay(3000)
            showControls = false
        }
    }

    // Hide brightness/volume indicators
    LaunchedEffect(showBrightness, showVolume) {
        if (showBrightness || showVolume) {
            delay(1500)
            showBrightness = false
            showVolume     = false
        }
    }

    // Restore on exit
    // Pinch-to-zoom
    val transformState = rememberTransformableState { zoomChange, offsetChange, _ ->
        videoScale = (videoScale * zoomChange).coerceIn(1f, 4f)
        if (videoScale > 1f) {
            videoOffsetX += offsetChange.x
            videoOffsetY += offsetChange.y
        } else {
            videoOffsetX = 0f; videoOffsetY = 0f
        }
    }

    BackHandler {
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onBack()
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {

        // ── Video surface ──────────────────────────────────────────────
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    this.player = player
                    useController = false
                    setBackgroundColor(android.graphics.Color.BLACK)
                    resizeMode = aspectModes[0]
                    playerViewRef = this

                    // Load subtitle track if .srt exists
                    srtPath?.let { path ->
                        val subConfig = androidx.media3.common.MediaItem.SubtitleConfiguration.Builder(
                            android.net.Uri.fromFile(java.io.File(path))
                        ).setMimeType(androidx.media3.common.MimeTypes.APPLICATION_SUBRIP)
                         .setSelectionFlags(androidx.media3.common.C.SELECTION_FLAG_DEFAULT)
                         .build()
                        val mediaItem = androidx.media3.common.MediaItem.Builder()
                            .setUri(video.uri)
                            .setSubtitleConfigurations(listOf(subConfig))
                            .build()
                        player?.clearMediaItems()
                        player?.setMediaItem(mediaItem)
                        player?.prepare()
                        player?.play()
                    }
                }
            },
            update = { pv ->
                pv.resizeMode = aspectModes[aspectIdx]
                pv.setPlaybackSpeed(videoSpeed)
            },
            modifier = Modifier
                .fillMaxSize()
                .transformable(state = transformState)
                .graphicsLayer(
                    scaleX         = videoScale,
                    scaleY         = videoScale,
                    translationX   = videoOffsetX,
                    translationY   = videoOffsetY,
                )
        )

        // ── Gesture layer ──────────────────────────────────────────────
        var gestureStartX by remember { mutableStateOf(0f) }
        var gestureStartY by remember { mutableStateOf(0f) }
        var gestureStartBrightness by remember { mutableStateOf(0f) }
        var gestureStartVolume by remember { mutableStateOf(0f) }
        var doubleTapPos by remember { mutableStateOf<Offset?>(null) }
        var lastTap by remember { mutableStateOf(0L) }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(isLocked) {
                    if (isLocked) return@pointerInput
                    detectTapGestures(
                        onTap = {
                            val now = System.currentTimeMillis()
                            if (now - lastTap < 300) {
                                // Double tap — seek ±10s
                                val screenW = size.width
                                if (it.x < screenW / 2) {
                                    player.seekTo((player.currentPosition - 10000).coerceAtLeast(0))
                                    seekLabel = "- 10s"
                                } else {
                                    player.seekTo(player.currentPosition + 10000)
                                    seekLabel = "+ 10s"
                                }
                                showSeekLabel = true
                                doubleTapPos  = it
                            } else {
                                showControls = !showControls
                            }
                            lastTap = now
                        }
                    )
                }
                .pointerInput(isLocked) {
                    if (isLocked) return@pointerInput
                    detectDragGestures(
                        onDragStart = { pos ->
                            gestureStartX          = pos.x
                            gestureStartY          = pos.y
                            gestureStartBrightness = brightness
                            gestureStartVolume     = volume
                        },
                        onDrag = { _, drag ->
                            val screenW = size.width
                            val isLeftSide = gestureStartX < screenW / 2
                            val delta = -drag.y / size.height

                            if (isLeftSide) {
                                brightness = (gestureStartBrightness + delta * 1.5f).coerceIn(0.01f, 1.0f)
                                showBrightness = true
                            } else {
                                volume = (gestureStartVolume + delta * 1.5f).coerceIn(0f, 1f)
                                player.volume = volume
                                showVolume = true
                            }
                        }
                    )
                }
        )

        // ── Seek label flash ───────────────────────────────────────────
        if (showSeekLabel) {
            LaunchedEffect(showSeekLabel) {
                delay(800)
                showSeekLabel = false
            }
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Box(
                    Modifier.clip(RoundedCornerShape(12.dp))
                        .background(Color.Black.copy(0.6f))
                        .padding(horizontal = 24.dp, vertical = 14.dp)
                ) {
                    Text(seekLabel, style = MaterialTheme.typography.headlineMedium,
                        color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }

        // ── Lock overlay ───────────────────────────────────────────────
        if (isLocked) {
            Box(Modifier.fillMaxSize().clickable { showControls = !showControls })
            AnimatedVisibility(visible = showControls, enter = fadeIn(), exit = fadeOut(),
                modifier = Modifier.align(Alignment.CenterStart).padding(start = 20.dp)) {
                IconButton(onClick = { isLocked = false }) {
                    Icon(Icons.Filled.LockOpen, null, tint = Color.White,
                        modifier = Modifier.size(28.dp))
                }
            }
            return@Box
        }

        // ── Controls overlay ───────────────────────────────────────────
        AnimatedVisibility(visible = showControls,
            enter = fadeIn(tween(200)), exit = fadeOut(tween(300))) {
            Box(
                Modifier.fillMaxSize().background(
                    Brush.verticalGradient(listOf(
                        Color.Black.copy(0.75f), Color.Transparent,
                        Color.Transparent, Color.Black.copy(0.85f)
                    ))
                ).clickable { showControls = false }
            ) {
                // ── Top bar ───────────────────────────────────────────
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)
                        .statusBarsPadding(),
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
                        modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                        maxLines = 1)

                    // Aspect ratio
                    TextButton(onClick = { aspectIdx = (aspectIdx + 1) % aspectLabels.size }) {
                        Text(aspectLabels[aspectIdx], color = Color.White,
                            style = MaterialTheme.typography.labelMedium)
                    }

                    // Subtitle toggle (only show if .srt exists)
                    if (srtPath != null) {
                        IconButton(onClick = { showSubtitles = !showSubtitles }) {
                            Icon(
                                if (showSubtitles) Icons.Filled.ClosedCaption
                                else Icons.Filled.ClosedCaptionDisabled,
                                null,
                                tint = if (showSubtitles) NebulaViolet else Color.White
                            )
                        }
                    }

                    // Lock
                    IconButton(onClick = { isLocked = true; showControls = false }) {
                        Icon(Icons.Filled.Lock, null, tint = Color.White)
                    }

                    // Screenshot
                    IconButton(onClick = {
                        try {
                            val view = playerViewRef ?: return@IconButton
                            val bmp = view.drawToBitmap()
                            val values = ContentValues().apply {
                                put(MediaStore.Images.Media.DISPLAY_NAME, "deck_screenshot_${System.currentTimeMillis()}.jpg")
                                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                                put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/Deck")
                            }
                            val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                            uri?.let { u ->
                                context.contentResolver.openOutputStream(u)?.use { out ->
                                    bmp.compress(Bitmap.CompressFormat.JPEG, 95, out)
                                }
                            }
                        } catch (_: Exception) {}
                    }) {
                        Icon(Icons.Filled.CameraAlt, null, tint = Color.White)
                    }

                    // Speed
                    TextButton(onClick = { showSpeed = true }) {
                        Text("${videoSpeed}×", color = Color.White,
                            style = MaterialTheme.typography.labelMedium)
                    }

                    // PiP
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        IconButton(onClick = {
                            try {
                                val params = PictureInPictureParams.Builder()
                                    .setAspectRatio(Rational(16, 9)).build()
                                activity?.enterPictureInPictureMode(params)
                            } catch (_: Exception) {}
                        }) {
                            Icon(Icons.Filled.PictureInPicture, null, tint = Color.White)
                        }
                    }
                }

                // ── Center controls ───────────────────────────────────
                Row(
                    Modifier.align(Alignment.Center),
                    horizontalArrangement = Arrangement.spacedBy(40.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = {
                        player.seekTo((player.currentPosition - 10000).coerceAtLeast(0))
                    }, modifier = Modifier.size(56.dp)) {
                        Icon(Icons.Filled.Replay10, null, tint = Color.White,
                            modifier = Modifier.size(40.dp))
                    }

                    Box(
                        Modifier.size(70.dp).clip(CircleShape)
                            .background(Color.White.copy(0.2f))
                            .border(2.dp, Color.White.copy(0.5f), CircleShape)
                            .clickable {
                                if (player.isPlaying) player.pause() else player.play()
                                isPlaying = player.isPlaying
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        // Poll player state
                        val playing by produceState(player.isPlaying) {
                            while (true) { value = player.isPlaying; delay(200) }
                        }
                        Icon(
                            if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            null, tint = Color.White, modifier = Modifier.size(34.dp)
                        )
                    }

                    IconButton(onClick = {
                        player.seekTo(player.currentPosition + 10000)
                    }, modifier = Modifier.size(56.dp)) {
                        Icon(Icons.Filled.Forward10, null, tint = Color.White,
                            modifier = Modifier.size(40.dp))
                    }
                }

                // ── Bottom seek bar ───────────────────────────────────
                Column(
                    Modifier.fillMaxWidth().align(Alignment.BottomCenter)
                        .padding(horizontal = 16.dp).navigationBarsPadding().padding(bottom = 8.dp)
                ) {
                    val position by produceState(player.currentPosition) {
                        while (true) { value = player.currentPosition; delay(300) }
                    }
                    val duration = player.duration.coerceAtLeast(1L)
                    val progress = (position.toFloat() / duration).coerceIn(0f, 1f)

                    Row(Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(formatVideoMs(position), style = MaterialTheme.typography.labelMedium,
                            color = Color.White.copy(0.8f))
                        Text(formatVideoMs(duration), style = MaterialTheme.typography.labelMedium,
                            color = Color.White.copy(0.8f))
                    }
                    Slider(
                        value    = progress,
                        onValueChange = { player.seekTo((it * duration).toLong()) },
                        modifier = Modifier.fillMaxWidth(),
                        colors   = SliderDefaults.colors(
                            activeTrackColor   = NebulaViolet,
                            thumbColor         = Color.White,
                            inactiveTrackColor = Color.White.copy(0.3f)
                        )
                    )
                }

                // ── Brightness indicator (left) ───────────────────────
                AnimatedVisibility(visible = showBrightness,
                    enter = fadeIn(), exit = fadeOut(),
                    modifier = Modifier.align(Alignment.CenterStart).padding(start = 24.dp)) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Filled.BrightnessHigh, null, tint = Color.White,
                            modifier = Modifier.size(20.dp))
                        Box(
                            Modifier.width(4.dp).height(100.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(Color.White.copy(0.2f))
                        ) {
                            Box(Modifier.fillMaxWidth().fillMaxHeight(brightness)
                                .align(Alignment.BottomCenter)
                                .background(Color.White))
                        }
                        Text("${(brightness * 100).toInt()}%",
                            style = MaterialTheme.typography.labelSmall, color = Color.White)
                    }
                }

                // ── Volume indicator (right) ──────────────────────────
                AnimatedVisibility(visible = showVolume,
                    enter = fadeIn(), exit = fadeOut(),
                    modifier = Modifier.align(Alignment.CenterEnd).padding(end = 24.dp)) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Filled.VolumeUp, null, tint = Color.White,
                            modifier = Modifier.size(20.dp))
                        Box(
                            Modifier.width(4.dp).height(100.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(Color.White.copy(0.2f))
                        ) {
                            Box(Modifier.fillMaxWidth().fillMaxHeight(volume)
                                .align(Alignment.BottomCenter)
                                .background(NebulaViolet))
                        }
                        Text("${(volume * 100).toInt()}%",
                            style = MaterialTheme.typography.labelSmall, color = Color.White)
                    }
                }
            }
        }
    }

    // Speed picker for video
    if (showSpeed) {
        SpeedPickerSheet(
            current   = videoSpeed,
            onSelect  = { s ->
                videoSpeed = s
                player?.setPlaybackSpeed(s)
                showSpeed = false
            },
            onDismiss = { showSpeed = false }
        )
    }
}

private fun formatVideoMs(ms: Long): String {
    if (ms <= 0) return "00:00"
    val total = ms / 1000
    val h = total / 3600
    val m = (total % 3600) / 60
    val s = total % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
}
