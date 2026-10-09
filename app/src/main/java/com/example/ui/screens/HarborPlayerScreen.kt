package com.example.ui.screens

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PictureInPicture
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import android.widget.Toast
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.example.MainActivity
import com.example.data.model.StremioMetaDetail
import com.example.data.model.StremioVideo
import com.example.data.repository.CatalogRepository
import com.example.data.repository.StreamResolverRepository
import com.example.data.repository.VaultRepository
import com.example.ui.theme.LocalHarborTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

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
    poster: String? = null,
    background: String? = null,
    vaultRepository: VaultRepository,
    catalogRepository: CatalogRepository? = null,
    streamResolverRepository: StreamResolverRepository? = null,
    onBack: () -> Unit,
    onSwitchStream: ((title: String, streamUrl: String, mediaId: String, season: Int?, episode: Int?, episodeTitle: String?, poster: String?, background: String?) -> Unit)? = null,
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

    // Dialog & Drawer States
    var showSubtitleDialog by remember { mutableStateOf(false) }
    var showAudioDialog by remember { mutableStateOf(false) }
    var showEpisodeDrawer by remember { mutableStateOf(false) }
    var showLongPressMenu by remember { mutableStateOf(false) }

    // Series detail & episode switching
    var seriesDetail by remember { mutableStateOf<StremioMetaDetail?>(null) }
    var selectedDrawerSeason by remember { mutableIntStateOf(season ?: 1) }
    var isSwitchingEpisode by remember { mutableStateOf(false) }

    // ExoPlayer initialization
    val exoPlayer = remember {
        val renderersFactory = DefaultRenderersFactory(context)
            .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER)
            .setEnableDecoderFallback(true)

        val httpDataSourceFactory = DefaultHttpDataSource.Factory()
            .setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36")
            .setAllowCrossProtocolRedirects(true)
            .setConnectTimeoutMs(20000)
            .setReadTimeoutMs(40000)

        val mediaSourceFactory = DefaultMediaSourceFactory(context)
            .setDataSourceFactory(httpDataSourceFactory)

        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(15000, 50000, 2000, 5000)
            .build()

        ExoPlayer.Builder(context, renderersFactory)
            .setMediaSourceFactory(mediaSourceFactory)
            .setLoadControl(loadControl)
            .setSeekForwardIncrementMs(10000)
            .setSeekBackIncrementMs(10000)
            .build().apply {
                playWhenReady = true
                val mediaItem = MediaItem.fromUri(Uri.parse(streamUrl))
                setMediaItem(mediaItem)
                prepare()
            }
    }

    // Auto Picture-In-Picture lifecycle flag
    DisposableEffect(Unit) {
        (activity as? MainActivity)?.isPlayingVideo = true
        onDispose {
            (activity as? MainActivity)?.isPlayingVideo = false
            exoPlayer.release()
        }
    }

    BackHandler {
        if (showEpisodeDrawer) {
            showEpisodeDrawer = false
        } else {
            exoPlayer.stop()
            onBack()
        }
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
                val errorMsg = error.localizedMessage ?: error.errorCodeName
                val isAudioCodecError = error.errorCode == PlaybackException.ERROR_CODE_AUDIO_TRACK_INIT_FAILED ||
                        error.errorCode == PlaybackException.ERROR_CODE_AUDIO_TRACK_WRITE_FAILED ||
                        errorMsg.contains("AUDIO", ignoreCase = true) ||
                        errorMsg.contains("AudioTrack", ignoreCase = true)

                if (isAudioCodecError) {
                    try {
                        exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
                            .buildUpon()
                            .setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, false)
                            .build()
                        exoPlayer.prepare()
                        exoPlayer.play()
                        Toast.makeText(context, "Recovered playback via standard audio output", Toast.LENGTH_SHORT).show()
                        return
                    } catch (e: Exception) {
                        // Fallthrough to error display
                    }
                }

                val reason = when (error.errorCode) {
                    PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED -> "Network connection interrupted. Check internet or Debrid link."
                    PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS -> "Stream link expired or access rejected by host."
                    PlaybackException.ERROR_CODE_DECODER_INIT_FAILED -> "Codec unsupported on hardware. Using software decoder fallback."
                    PlaybackException.ERROR_CODE_AUDIO_TRACK_INIT_FAILED -> "Audio format unsupported. Switched to standard audio."
                    else -> "Playback error: $errorMsg"
                }
                playerError = reason
            }
        }
        exoPlayer.addListener(listener)

        onDispose {
            exoPlayer.removeListener(listener)
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

    val stremioSession = remember { com.example.data.local.StremioAccountSession(context) }
    val stremioAuthKey = remember { stremioSession.getAuthKey() }
    val stremioApiClient = remember { com.example.data.api.StremioApiClient() }

    // Initial playback position resume
    LaunchedEffect(streamUrl) {
        try {
            val cleanShowId = if (mediaId.startsWith("tt") && mediaId.contains(":")) mediaId.substringBefore(":") else mediaId
            val key = if (season != null && episode != null) "$cleanShowId:$season:$episode" else cleanShowId
            val savedHistory = vaultRepository.getHistoryById(key) ?: vaultRepository.getHistoryById(cleanShowId)
            if (savedHistory != null && savedHistory.positionMs > 2000L) {
                val isFinished = savedHistory.durationMs > 0 && (savedHistory.positionMs.toFloat() / savedHistory.durationMs.toFloat()) >= 0.92f
                if (!isFinished) {
                    exoPlayer.seekTo(savedHistory.positionMs)
                    Toast.makeText(context, "Resumed at ${formatTime(savedHistory.positionMs)}", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: Exception) {
            // Ignore initial seek exception
        }
    }

    // Load series details for in-player episode switching
    LaunchedEffect(mediaId) {
        if (catalogRepository != null && season != null) {
            try {
                val cleanShowId = if (mediaId.startsWith("tt") && mediaId.contains(":")) mediaId.substringBefore(":") else mediaId
                val detail = catalogRepository.getMetaDetail("series", cleanShowId)
                seriesDetail = detail
            } catch (e: Exception) {
                // Ignore detail load error in player
            }
        }
    }

    // Progress update loop
    LaunchedEffect(Unit) {
        while (true) {
            if (!isUserSeeking) {
                currentPositionMs = exoPlayer.currentPosition.coerceAtLeast(0L)
                durationMs = exoPlayer.duration.coerceAtLeast(0L)
            }
            delay(1000)
        }
    }

    // Position persistence every 4s
    LaunchedEffect(currentPositionMs) {
        if (durationMs > 0 && currentPositionMs > 2000) {
            val cleanShowId = if (mediaId.startsWith("tt") && mediaId.contains(":")) mediaId.substringBefore(":") else mediaId
            val key = if (season != null && episode != null) "$cleanShowId:$season:$episode" else cleanShowId
            vaultRepository.savePlaybackProgress(
                id = key,
                mediaId = cleanShowId,
                title = title,
                type = if (season != null) "series" else "movie",
                poster = poster,
                background = background,
                season = season,
                episode = episode,
                episodeTitle = episodeTitle,
                positionMs = currentPositionMs,
                durationMs = durationMs,
                streamUrl = streamUrl,
                stremioAuthKey = stremioAuthKey,
                apiClient = stremioApiClient
            )
        }
    }

    // Auto-hide controls timer
    LaunchedEffect(areControlsVisible, isPlaying) {
        if (areControlsVisible && isPlaying && !showSubtitleDialog && !showAudioDialog && !showEpisodeDrawer) {
            delay(5000)
            areControlsVisible = false
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .testTag("harbor_player_screen")
    ) {
        // Main Video Player View
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

        // Buffering Indicator
        if (isBuffering && playerError == null) {
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(54.dp),
                    color = theme.primary,
                    strokeWidth = 3.dp
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Loading Stream...",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // Error State Card
        if (playerError != null) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = theme.surface),
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(24.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Playback Error", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFFEF4444))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = playerError ?: "Stream failed to load.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            playerError = null
                            exoPlayer.prepare()
                            exoPlayer.play()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = theme.primary, contentColor = Color.Black)
                    ) {
                        Text("Retry Stream", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // HUD Overlay
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
                                Color.Black.copy(alpha = 0.85f),
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.9f)
                            )
                        )
                    )
            ) {
                // Top Action HUD Bar
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
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (season != null && episode != null) {
                                Text(
                                    text = "Season $season • Episode $episode${if (!episodeTitle.isNullOrBlank()) " - $episodeTitle" else ""}",
                                    color = theme.primary,
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    // Quick Action Icons (Subtitles, Audio, Episodes Drawer, PiP)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        // Subtitle Track Button
                        IconButton(
                            onClick = { showSubtitleDialog = true },
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.5f))
                                .size(36.dp)
                                .testTag("player_subtitles_button")
                        ) {
                            Icon(imageVector = Icons.Default.Subtitles, contentDescription = "Subtitles", tint = Color.White, modifier = Modifier.size(18.dp))
                        }

                        // Audio Track Button
                        IconButton(
                            onClick = { showAudioDialog = true },
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.5f))
                                .size(36.dp)
                                .testTag("player_audio_button")
                        ) {
                            Icon(imageVector = Icons.Default.Audiotrack, contentDescription = "Audio Tracks", tint = Color.White, modifier = Modifier.size(18.dp))
                        }

                        // Episodes Drawer Toggle Button (for TV shows)
                        if (season != null && seriesDetail?.videos != null) {
                            IconButton(
                                onClick = { showEpisodeDrawer = true },
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(theme.primary.copy(alpha = 0.3f))
                                    .size(36.dp)
                                    .testTag("player_episodes_drawer_button")
                            ) {
                                Icon(imageVector = Icons.Default.List, contentDescription = "Episodes", tint = theme.primary, modifier = Modifier.size(20.dp))
                            }
                        }

                        // PiP Button
                        IconButton(
                            onClick = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && activity != null) {
                                    try {
                                        val params = android.app.PictureInPictureParams.Builder()
                                            .setAspectRatio(android.util.Rational(16, 9))
                                            .build()
                                        activity.enterPictureInPictureMode(params)
                                    } catch (e: Exception) {
                                        // PiP fallback
                                    }
                                }
                            },
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.5f))
                                .size(36.dp)
                        ) {
                            Icon(imageVector = Icons.Default.PictureInPicture, contentDescription = "Picture in Picture", tint = Color.White, modifier = Modifier.size(18.dp))
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
                        ) {
                            Icon(imageVector = Icons.Default.OpenInNew, contentDescription = "External Player", tint = Color.White, modifier = Modifier.size(18.dp))
                        }
                    }
                }

                // Center Play/Pause & Seek Controls (Hidden while buffering)
                if (!isBuffering) {
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
                                .size(52.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.6f))
                                .border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape)
                                .testTag("player_replay_10")
                        ) {
                            Icon(imageVector = Icons.Default.Replay10, contentDescription = "Rewind 10s", tint = Color.White, modifier = Modifier.size(28.dp))
                        }

                        IconButton(
                            onClick = {
                                if (exoPlayer.isPlaying) {
                                    exoPlayer.pause()
                                } else {
                                    exoPlayer.play()
                                }
                            },
                            modifier = Modifier
                                .size(68.dp)
                                .clip(CircleShape)
                                .background(theme.primary)
                                .testTag("player_play_pause")
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = "Toggle Playback",
                                tint = Color.Black,
                                modifier = Modifier.size(38.dp)
                            )
                        }

                        IconButton(
                            onClick = {
                                val newPos = (exoPlayer.currentPosition + 10000).coerceAtMost(durationMs)
                                exoPlayer.seekTo(newPos)
                            },
                            modifier = Modifier
                                .size(52.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.6f))
                                .border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape)
                                .testTag("player_forward_10")
                        ) {
                            Icon(imageVector = Icons.Default.Forward10, contentDescription = "Forward 10s", tint = Color.White, modifier = Modifier.size(28.dp))
                        }
                    }
                }

                // Bottom Timeline Controls Bar
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
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

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Shader Mode Toggle
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (shaderMode != HarborShaderMode.OFF) theme.primary else Color.Black.copy(alpha = 0.5f))
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

                            // Aspect Ratio Toggle
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
                                Icon(imageVector = Icons.Default.AspectRatio, contentDescription = "Aspect Ratio", tint = Color.White, modifier = Modifier.size(18.dp))
                            }

                            // Playback Speed Toggle
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

        // Subtitles Selector Dialog
        if (showSubtitleDialog) {
            val tracks = exoPlayer.currentTracks
            val textGroups = tracks.groups.filter { it.type == C.TRACK_TYPE_TEXT }

            AlertDialog(
                onDismissRequest = { showSubtitleDialog = false },
                containerColor = theme.surface,
                title = { Text("Subtitles & Captions", color = Color.White, fontWeight = FontWeight.Bold) },
                text = {
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
                                        .buildUpon()
                                        .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
                                        .build()
                                    showSubtitleDialog = false
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Subtitles Off", color = Color.White, fontSize = 13.sp, modifier = Modifier.weight(1f))
                        }

                        if (textGroups.isEmpty()) {
                            Text("No embedded subtitle tracks detected in this stream.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        } else {
                            textGroups.forEachIndexed { groupIdx, group ->
                                val mediaTrackGroup = group.mediaTrackGroup
                                for (i in 0 until mediaTrackGroup.length) {
                                    val format = mediaTrackGroup.getFormat(i)
                                    val lang = format.language?.uppercase() ?: "Track ${i + 1}"
                                    val label = format.label ?: "Subtitle $lang"
                                    val isSelected = group.isTrackSelected(i)

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
                                                    .buildUpon()
                                                    .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
                                                    .setOverrideForType(TrackSelectionOverride(mediaTrackGroup, i))
                                                    .build()
                                                showSubtitleDialog = false
                                            }
                                            .padding(vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("$label ($lang)", color = if (isSelected) theme.primary else Color.White, fontSize = 13.sp, modifier = Modifier.weight(1f))
                                        RadioButton(
                                            selected = isSelected,
                                            onClick = null,
                                            colors = RadioButtonDefaults.colors(selectedColor = theme.primary)
                                        )
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showSubtitleDialog = false }) {
                        Text("Close", color = theme.primary)
                    }
                }
            )
        }

        // Audio Tracks Selector Dialog
        if (showAudioDialog) {
            val tracks = exoPlayer.currentTracks
            val audioGroups = tracks.groups.filter { it.type == C.TRACK_TYPE_AUDIO }

            AlertDialog(
                onDismissRequest = { showAudioDialog = false },
                containerColor = theme.surface,
                title = { Text("Audio Language & Dubs", color = Color.White, fontWeight = FontWeight.Bold) },
                text = {
                    Column {
                        if (audioGroups.isEmpty()) {
                            Text("Standard Stereo / Default Audio active.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        } else {
                            audioGroups.forEachIndexed { groupIdx, group ->
                                val mediaTrackGroup = group.mediaTrackGroup
                                for (i in 0 until mediaTrackGroup.length) {
                                    val format = mediaTrackGroup.getFormat(i)
                                    val lang = format.language?.uppercase() ?: "Default Audio"
                                    val label = format.label ?: "Audio $lang (${format.channelCount} ch)"
                                    val isSelected = group.isTrackSelected(i)

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
                                                    .buildUpon()
                                                    .setOverrideForType(TrackSelectionOverride(mediaTrackGroup, i))
                                                    .build()
                                                showAudioDialog = false
                                            }
                                            .padding(vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(label, color = if (isSelected) theme.primary else Color.White, fontSize = 13.sp, modifier = Modifier.weight(1f))
                                        RadioButton(
                                            selected = isSelected,
                                            onClick = null,
                                            colors = RadioButtonDefaults.colors(selectedColor = theme.primary)
                                        )
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showAudioDialog = false }) {
                        Text("Close", color = theme.primary)
                    }
                }
            )
        }

        // In-Player Episodes Drawer (Side Sheet)
        if (showEpisodeDrawer && seriesDetail?.videos != null) {
            val videos = seriesDetail!!.videos!!
            val seasons = videos.mapNotNull { it.season }.distinct().sorted()
            val currentSeasonVideos = videos.filter { (it.season ?: 1) == selectedDrawerSeason }

            AnimatedVisibility(
                visible = showEpisodeDrawer,
                enter = slideInHorizontally(initialOffsetX = { it }),
                exit = slideOutHorizontally(targetOffsetX = { it }),
                modifier = Modifier.align(Alignment.CenterEnd)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(320.dp)
                        .background(theme.surface.copy(alpha = 0.95f))
                        .padding(16.dp)
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Drawer Header
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Episodes",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                            IconButton(onClick = { showEpisodeDrawer = false }) {
                                Icon(imageVector = Icons.Default.Close, contentDescription = "Close Drawer", tint = Color.White)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Season Selector Pills
                        if (seasons.size > 1) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                seasons.forEach { s ->
                                    val isCurrent = selectedDrawerSeason == s
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isCurrent) theme.primary else theme.surfaceVariant)
                                            .clickable { selectedDrawerSeason = s }
                                            .padding(horizontal = 10.dp, vertical = 5.dp)
                                    ) {
                                        Text(
                                            text = "S$s",
                                            fontSize = 11.sp,
                                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isCurrent) Color.Black else Color.White
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        // Episodes List inside Drawer
                        if (isSwitchingEpisode) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(color = theme.primary, modifier = Modifier.size(32.dp))
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(currentSeasonVideos) { ep ->
                                    val isCurrentEp = season == ep.season && episode == ep.episode
                                    Card(
                                        shape = RoundedCornerShape(10.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (isCurrentEp) theme.primary.copy(alpha = 0.2f) else theme.surfaceVariant
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                if (streamResolverRepository != null && onSwitchStream != null) {
                                                    scope.launch {
                                                        isSwitchingEpisode = true
                                                        val cleanShowId = if (mediaId.startsWith("tt") && mediaId.contains(":")) mediaId.substringBefore(":") else mediaId
                                                        val queryEpId = "${cleanShowId}:${ep.season ?: 1}:${ep.episode ?: 1}"
                                                        val resolved = streamResolverRepository.resolveStreams("series", queryEpId)
                                                        if (resolved.isNotEmpty()) {
                                                            val topStream = resolved.first()
                                                            var rawUrl = topStream.rawStream.url ?: if (!topStream.rawStream.infoHash.isNullOrBlank()) {
                                                                "magnet:?xt=urn:btih:${topStream.rawStream.infoHash}"
                                                            } else ""

                                                            if (rawUrl.startsWith("magnet:")) {
                                                                val debridRes = streamResolverRepository.resolveMagnetViaDebrid(rawUrl)
                                                                if (debridRes.isSuccess) {
                                                                    rawUrl = debridRes.getOrThrow()
                                                                }
                                                            }

                                                            isSwitchingEpisode = false
                                                            if (rawUrl.isNotBlank()) {
                                                                val newTitle = "${seriesDetail?.name ?: title} - ${ep.computedTitle}"
                                                                showEpisodeDrawer = false
                                                                onSwitchStream(
                                                                    newTitle,
                                                                    rawUrl,
                                                                    mediaId,
                                                                    ep.season,
                                                                    ep.episode,
                                                                    ep.computedTitle,
                                                                    poster,
                                                                    background
                                                                )
                                                            }
                                                        } else {
                                                            isSwitchingEpisode = false
                                                        }
                                                    }
                                                }
                                            }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(28.dp)
                                                    .clip(CircleShape)
                                                    .background(if (isCurrentEp) theme.primary else Color.Black.copy(alpha = 0.4f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = "E${ep.episode}",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isCurrentEp) Color.Black else Color.White
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = ep.computedTitle,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.White,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                if (!ep.overview.isNullOrBlank()) {
                                                    Text(
                                                        text = ep.overview,
                                                        fontSize = 10.sp,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
