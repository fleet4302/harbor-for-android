package com.example.ui.screens

import android.app.Activity
import android.app.PictureInPictureParams
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.net.Uri
import android.os.Build
import android.util.Rational
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PictureInPicture
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.example.data.repository.VaultRepository
import com.example.ui.theme.LocalHarborTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale
import java.util.concurrent.TimeUnit

enum class HarborShaderMode(val label: String) {
    OFF("Standard"),
    ANIME4K("Anime4K Upscale"),
    CINEMA_CONTRAST("Cinema HDR")
}

@OptIn(UnstableApi::class)
@Composable
fun HarborPlayerScreen(
    title: String,
    streamUrl: String,
    mediaId: String,
    season: Int? = null,
    episode: Int? = null,
    episodeTitle: String? = null,
    vaultRepository: VaultRepository,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val theme = LocalHarborTheme.current
    val context = LocalContext.current
    val activity = context as? Activity
    val scope = rememberCoroutineScope()

    var isPlaying by remember { mutableStateOf(true) }
    var isBuffering by remember { mutableStateOf(true) }
    var currentPositionMs by remember { mutableLongStateOf(0L) }
    var durationMs by remember { mutableLongStateOf(0L) }
    var areControlsVisible by remember { mutableStateOf(true) }
    var playbackSpeed by remember { mutableFloatStateOf(1.0f) }
    var resizeMode by remember { mutableIntStateOf(AspectRatioFrameLayout.RESIZE_MODE_FIT) }
    var shaderMode by remember { mutableStateOf(HarborShaderMode.OFF) }
    var isUserSeeking by remember { mutableStateOf(false) }
    var seekSliderPosition by remember { mutableFloatStateOf(0f) }

    var playerError by remember { mutableStateOf<String?>(null) }

    // ExoPlayer initialization with cross-protocol redirects & streaming support
    val exoPlayer = remember {
        val httpDataSourceFactory = DefaultHttpDataSource.Factory()
            .setUserAgent("Harbor-Android/1.0.0 (Linux; Android)")
            .setAllowCrossProtocolRedirects(true)
            .setConnectTimeoutMs(15000)
            .setReadTimeoutMs(30000)

        val mediaSourceFactory = DefaultMediaSourceFactory(context)
            .setDataSourceFactory(httpDataSourceFactory)

        ExoPlayer.Builder(context)
            .setMediaSourceFactory(mediaSourceFactory)
            .build().apply {
                playWhenReady = true
                val mediaItem = MediaItem.fromUri(Uri.parse(streamUrl))
                setMediaItem(mediaItem)
                prepare()
            }
    }

    BackHandler {
        exoPlayer.stop()
        onBack()
    }

    DisposableEffect(Unit) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                isBuffering = playbackState == Player.STATE_BUFFERING
                if (playbackState == Player.STATE_READY) {
                    playerError = null
                    durationMs = exoPlayer.duration.coerceAtLeast(0L)
                }
            }

            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }

            override fun onPlayerError(error: PlaybackException) {
                isBuffering = false
                val reason = error.localizedMessage ?: "Playback error: ${error.errorCodeName}"
                playerError = reason
            }
        }
        exoPlayer.addListener(listener)

        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.release()
        }
    }

    // Progress update loop and auto-save to Vault
    LaunchedEffect(Unit) {
        while (true) {
            if (!isUserSeeking) {
                currentPositionMs = exoPlayer.currentPosition.coerceAtLeast(0L)
                durationMs = exoPlayer.duration.coerceAtLeast(0L)
            }
            delay(1000)
        }
    }

    // Periodic position persistence every 4 seconds
    LaunchedEffect(currentPositionMs) {
        if (durationMs > 0 && currentPositionMs > 2000) {
            val key = if (season != null && episode != null) "$mediaId:$season:$episode" else mediaId
            vaultRepository.savePlaybackProgress(
                id = key,
                mediaId = mediaId,
                title = title,
                type = if (season != null) "series" else "movie",
                poster = null,
                background = null,
                season = season,
                episode = episode,
                episodeTitle = episodeTitle,
                positionMs = currentPositionMs,
                durationMs = durationMs,
                streamUrl = streamUrl
            )
        }
    }

    // Auto-hide controls timer
    LaunchedEffect(areControlsVisible, isPlaying) {
        if (areControlsVisible && isPlaying) {
            delay(4500)
            areControlsVisible = false
        }
    }

    fun formatTime(ms: Long): String {
        val totalSecs = (ms / 1000).coerceAtLeast(0)
        val hours = totalSecs / 3600
        val mins = (totalSecs % 3600) / 60
        val secs = totalSecs % 60
        return if (hours > 0) {
            String.format(Locale.ROOT, "%d:%02d:%02d", hours, mins, secs)
        } else {
            String.format(Locale.ROOT, "%02d:%02d", mins, secs)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .testTag("harbor_player_screen")
    ) {
        // Video View
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = false
                    this.resizeMode = resizeMode
                }
            },
            update = { playerView ->
                playerView.resizeMode = resizeMode
            },
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    areControlsVisible = !areControlsVisible
                }
        )

        // Harbor Shader Overlay Filter (Simulated Anime4K / Cinema mode)
        if (shaderMode != HarborShaderMode.OFF) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        if (shaderMode == HarborShaderMode.ANIME4K) {
                            Color(0x0A00E5FF) // subtle vibrant boost
                        } else {
                            Color(0x12000000) // contrast boost
                        }
                    )
            )
        }

        // Buffering Indicator
        if (isBuffering && playerError == null) {
            Box(
                modifier = Modifier.align(Alignment.Center)
            ) {
                CircularProgressIndicator(
                    color = theme.primary,
                    strokeWidth = 3.dp,
                    modifier = Modifier.size(52.dp)
                )
            }
        }

        // Playback Error Overlay
        if (playerError != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.92f))
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth(0.9f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFEF4444).copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = null,
                            tint = Color(0xFFEF4444),
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Native Playback Error",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = playerError ?: "Unable to stream source",
                        color = Color(0xFFEF4444),
                        fontSize = 13.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        androidx.compose.material3.Button(
                            onClick = {
                                playerError = null
                                isBuffering = true
                                exoPlayer.prepare()
                                exoPlayer.play()
                            },
                            colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                containerColor = theme.primary,
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Retry Playback", fontWeight = FontWeight.Bold)
                        }

                        androidx.compose.material3.OutlinedButton(
                            onClick = {
                                val intent = Intent(Intent.ACTION_VIEW).apply {
                                    if (streamUrl.startsWith("magnet:")) {
                                        data = Uri.parse(streamUrl)
                                    } else {
                                        setDataAndType(Uri.parse(streamUrl), "video/*")
                                    }
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                }
                                try {
                                    context.startActivity(Intent.createChooser(intent, "Open stream with"))
                                } catch (e: Exception) {
                                    // ignore
                                }
                            },
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(imageVector = Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("External Player", color = Color.White)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    androidx.compose.material3.TextButton(onClick = {
                        exoPlayer.stop()
                        onBack()
                    }) {
                        Text("Back to Details", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        // Harbor HUD Overlay
        AnimatedVisibility(
            visible = areControlsVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.Black.copy(alpha = 0.8f),
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.85f)
                            )
                        )
                    )
            ) {
                // Top Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        IconButton(
                            onClick = {
                                exoPlayer.stop()
                                onBack()
                            },
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.5f))
                                .size(40.dp)
                                .testTag("player_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.White
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            Text(
                                text = title,
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                            if (season != null && episode != null) {
                                Text(
                                    text = "Season $season • Episode $episode${if (!episodeTitle.isNullOrBlank()) " - $episodeTitle" else ""}",
                                    color = theme.primary,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }

                    // Top action buttons (PiP, External player, Shaders)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        // Picture in picture button
                        IconButton(
                            onClick = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && activity != null) {
                                    val params = PictureInPictureParams.Builder()
                                        .setAspectRatio(Rational(16, 9))
                                        .build()
                                    activity.enterPictureInPictureMode(params)
                                }
                            },
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.5f))
                                .size(36.dp)
                                .testTag("player_pip_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.PictureInPicture,
                                contentDescription = "Picture in Picture",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        // External Player
                        IconButton(
                            onClick = {
                                val intent = Intent(Intent.ACTION_VIEW).apply {
                                    setDataAndType(Uri.parse(streamUrl), "video/*")
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                }
                                context.startActivity(Intent.createChooser(intent, "Play in"))
                            },
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.5f))
                                .size(36.dp)
                                .testTag("player_external_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.OpenInNew,
                                contentDescription = "External Player",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                // Center Play / Pause & 10s Skip controls
                Row(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalArrangement = Arrangement.spacedBy(28.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            val newPos = (exoPlayer.currentPosition - 10000).coerceAtLeast(0)
                            exoPlayer.seekTo(newPos)
                        },
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.5f))
                            .testTag("player_replay_10")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Replay10,
                            contentDescription = "Rewind 10s",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    // Play / Pause main button
                    IconButton(
                        onClick = {
                            if (exoPlayer.isPlaying) {
                                exoPlayer.pause()
                            } else {
                                exoPlayer.play()
                            }
                        },
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(theme.primary)
                            .testTag("player_play_pause")
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = "Toggle Playback",
                            tint = Color.Black,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    IconButton(
                        onClick = {
                            val newPos = (exoPlayer.currentPosition + 10000).coerceAtMost(durationMs)
                            exoPlayer.seekTo(newPos)
                        },
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.5f))
                            .testTag("player_forward_10")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Forward10,
                            contentDescription = "Forward 10s",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                // Bottom Bar Controls
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    // Scrub slider
                    val progressRatio = if (durationMs > 0) {
                        if (isUserSeeking) seekSliderPosition else (currentPositionMs.toFloat() / durationMs.toFloat())
                    } else 0f

                    Slider(
                        value = progressRatio.coerceIn(0f, 1f),
                        onValueChange = {
                            isUserSeeking = true
                            seekSliderPosition = it
                        },
                        onValueChangeFinished = {
                            isUserSeeking = false
                            val targetMs = (seekSliderPosition * durationMs).toLong()
                            exoPlayer.seekTo(targetMs)
                        },
                        colors = SliderDefaults.colors(
                            thumbColor = theme.primary,
                            activeTrackColor = theme.primary,
                            inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(28.dp)
                            .testTag("player_seek_slider")
                    )

                    // Timestamps and Control shortcuts
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${formatTime(currentPositionMs)} / ${formatTime(durationMs)}",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )

                        // Bottom buttons row: Aspect Ratio, Speed, Shaders
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Shaders button (Harbor specialty!)
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(
                                        if (shaderMode != HarborShaderMode.OFF) theme.primary else Color.Black.copy(alpha = 0.5f)
                                    )
                                    .clickable {
                                        shaderMode = when (shaderMode) {
                                            HarborShaderMode.OFF -> HarborShaderMode.ANIME4K
                                            HarborShaderMode.ANIME4K -> HarborShaderMode.CINEMA_CONTRAST
                                            HarborShaderMode.CINEMA_CONTRAST -> HarborShaderMode.OFF
                                        }
                                    }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = shaderMode.label,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (shaderMode != HarborShaderMode.OFF) Color.Black else Color.White
                                )
                            }

                            // Aspect Ratio Cycler
                            IconButton(
                                onClick = {
                                    resizeMode = when (resizeMode) {
                                        AspectRatioFrameLayout.RESIZE_MODE_FIT -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                                        AspectRatioFrameLayout.RESIZE_MODE_ZOOM -> AspectRatioFrameLayout.RESIZE_MODE_FILL
                                        else -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                                    }
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AspectRatio,
                                    contentDescription = "Aspect Ratio",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            // Speed Cycler
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color.Black.copy(alpha = 0.5f))
                                    .clickable {
                                        val speeds = listOf(0.75f, 1.0f, 1.25f, 1.5f, 2.0f)
                                        val nextIdx = (speeds.indexOf(playbackSpeed) + 1) % speeds.size
                                        playbackSpeed = speeds[nextIdx]
                                        exoPlayer.setPlaybackSpeed(playbackSpeed)
                                    }
                                    .padding(horizontal = 6.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = "${playbackSpeed}x",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
