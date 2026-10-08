package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.api.StremioApiClient
import com.example.data.local.StremioAccountSession
import com.example.data.model.HarborParsedStream
import com.example.data.model.StremioMetaDetail
import com.example.data.model.StremioVideo
import com.example.data.model.StreamResolution
import com.example.data.repository.AddonRepository
import com.example.data.repository.CatalogRepository
import com.example.data.repository.StreamResolverRepository
import com.example.data.repository.VaultRepository
import com.example.ui.components.EpisodeCard
import com.example.ui.components.MediaDetailSkeleton
import com.example.ui.components.StreamItemCard
import com.example.ui.components.StreamLoadingSkeletonList
import com.example.ui.components.StreamRadarScanningCard
import com.example.ui.components.StremioLoginDialog
import com.example.ui.components.TorrentioSetupDialog
import com.example.ui.theme.LocalHarborTheme
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MediaDetailScreen(
    mediaType: String,
    mediaId: String,
    catalogRepository: CatalogRepository,
    streamResolverRepository: StreamResolverRepository,
    vaultRepository: VaultRepository,
    stremioSession: StremioAccountSession,
    apiClient: StremioApiClient,
    addonRepository: AddonRepository,
    onBack: () -> Unit,
    onOpenSettings: () -> Unit = {},
    onPlayStream: (title: String, streamUrl: String, mediaId: String, season: Int?, episode: Int?, episodeTitle: String?, poster: String?, background: String?) -> Unit,
    modifier: Modifier = Modifier
) {
    val theme = LocalHarborTheme.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var detail by remember { mutableStateOf<StremioMetaDetail?>(null) }
    var streams by remember { mutableStateOf<List<HarborParsedStream>>(emptyList()) }
    var isLoadingDetail by remember { mutableStateOf(true) }
    var isLoadingStreams by remember { mutableStateOf(false) }

    var selectedSeason by remember { mutableIntStateOf(1) }
    var selectedEpisode by remember { mutableStateOf<StremioVideo?>(null) }
    var streamFilterRes by remember { mutableStateOf<StreamResolution?>(null) }

    var showTorrentioSetup by remember { mutableStateOf(false) }
    var showStremioLogin by remember { mutableStateOf(false) }

    var isResolvingDebrid by remember { mutableStateOf(false) }
    var debridResolutionError by remember { mutableStateOf<String?>(null) }
    var showDebridRequiredDialog by remember { mutableStateOf<String?>(null) }
    var pendingExternalMagnetUrl by remember { mutableStateOf<String?>(null) }

    val isBookmarked by vaultRepository.isBookmarked(mediaId).collectAsState(initial = false)

    fun refreshStreams(targetDetail: StremioMetaDetail) {
        scope.launch {
            Log.d("MediaDetailScreen", "refreshStreams called for ${targetDetail.type} ${targetDetail.id} episode: ${selectedEpisode?.id}")
            isLoadingStreams = true
            try {
                val streamQueryId = if (targetDetail.type == "series" && selectedEpisode != null) {
                    val s = selectedEpisode?.season ?: 1
                    val e = selectedEpisode?.episode ?: 1
                    "${targetDetail.id}:$s:$e"
                } else {
                    targetDetail.id
                }
                Log.d("MediaDetailScreen", "Querying streams with ID: $streamQueryId")
                streams = streamResolverRepository.resolveStreams(targetDetail.type, streamQueryId)
                Log.d("MediaDetailScreen", "Found ${streams.size} streams")
            } catch (e: Exception) {
                Log.e("MediaDetailScreen", "Error refreshing streams", e)
            } finally {
                isLoadingStreams = false
            }
        }
    }

    BackHandler {
        onBack()
    }

    LaunchedEffect(mediaId) {
        isLoadingDetail = true
        try {
            val d = catalogRepository.getMetaDetail(mediaType, mediaId)
            detail = d
            if (d.videos != null && d.videos.isNotEmpty()) {
                val firstEp = d.videos.firstOrNull { it.season == 1 } ?: d.videos.first()
                selectedEpisode = firstEp
            }
        } finally {
            isLoadingDetail = false
        }
    }

    // Load streams for either the movie or the currently selected episode
    LaunchedEffect(detail, selectedEpisode) {
        val currentDetail = detail ?: return@LaunchedEffect
        refreshStreams(currentDetail)
    }

    if (isLoadingDetail) {
        MediaDetailSkeleton()
        return
    }

    val item = detail ?: return

    val filteredStreams = if (streamFilterRes == null) {
        streams
    } else {
        streams.filter { it.resolution == streamFilterRes }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(theme.background)
            .testTag("media_detail_screen")
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize()
        ) {
            // Backdrop Header
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                ) {
                    val bgUrl = item.background ?: item.poster
                    if (!bgUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(bgUrl)
                                .crossfade(true)
                                .build(),
                            contentDescription = item.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    // Dark gradient overlay
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Black.copy(alpha = 0.5f),
                                        Color.Transparent,
                                        theme.background.copy(alpha = 0.85f),
                                        theme.background
                                    )
                                )
                            )
                    )

                    // Top Action Bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.6f))
                                .size(40.dp)
                                .testTag("detail_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.White
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            IconButton(
                                onClick = {
                                    scope.launch {
                                        vaultRepository.toggleWatchlist(
                                            id = item.id,
                                            title = item.name,
                                            type = item.type,
                                            poster = item.poster,
                                            background = item.background,
                                            releaseYear = item.releaseInfo,
                                            imdbRating = item.imdbRating,
                                            genres = item.genres?.joinToString(", ")
                                        )
                                    }
                                },
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.6f))
                                    .size(40.dp)
                                    .testTag("detail_bookmark_button")
                            ) {
                                Icon(
                                    imageVector = if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                    contentDescription = "Bookmark",
                                    tint = if (isBookmarked) theme.primary else Color.White
                                )
                            }
                        }
                    }
                }
            }

            // Media Info block (Poster + Title + Badges)
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    // Poster thumbnail
                    Box(
                        modifier = Modifier
                            .width(100.dp)
                            .height(145.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(theme.surfaceVariant)
                    ) {
                        if (!item.poster.isNullOrBlank()) {
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(item.poster)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = item.name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = item.name,
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Black
                            ),
                            color = Color.White
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (!item.imdbRating.isNullOrBlank()) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color(0xFF262626))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = "IMDb",
                                        tint = Color(0xFFFFB703),
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = item.imdbRating,
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            if (!item.releaseInfo.isNullOrBlank()) {
                                Text(
                                    text = item.releaseInfo,
                                    color = Color(0xFF94A3B8),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            if (!item.runtime.isNullOrBlank()) {
                                Text(
                                    text = "•  ${item.runtime}",
                                    color = Color(0xFF94A3B8),
                                    fontSize = 12.sp
                                )
                            }
                        }

                        // Genres chips
                        if (!item.genres.isNullOrEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                item.genres.forEach { genre ->
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(theme.surfaceVariant)
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = genre,
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Overview synopsis
            if (!item.description.isNullOrBlank()) {
                item {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Synopsis",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = item.description,
                            fontSize = 13.sp,
                            lineHeight = 18.sp,
                            color = Color(0xFFCBD5E1)
                        )
                    }
                }
            }

            // TV Series Season & Episode Picker
            if (item.type == "series" && !item.videos.isNullOrEmpty()) {
                val seasons = item.videos.mapNotNull { it.season }.distinct().sorted()
                val currentSeasonVideos = item.videos.filter { (it.season ?: 1) == selectedSeason }

                item {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                        Text(
                            text = "Episodes",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )

                        // Season selector tabs
                        if (seasons.size > 1) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState())
                                    .padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                seasons.forEach { s ->
                                    val isCurrent = selectedSeason == s
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isCurrent) theme.primary else theme.surfaceVariant)
                                            .clickable { selectedSeason = s }
                                            .padding(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = "Season $s",
                                            fontSize = 12.sp,
                                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isCurrent) Color.Black else Color.White
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                items(currentSeasonVideos) { ep ->
                    val isSelected = selectedEpisode?.id == ep.id
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) theme.primary.copy(alpha = 0.15f) else Color.Transparent)
                    ) {
                        EpisodeCard(
                            episode = ep,
                            onSelect = {
                                Log.d("MediaDetailScreen", "Episode clicked: ${ep.id}")
                                selectedEpisode = ep
                            }
                        )
                    }
                }
            }

            // Harbor Streams Section
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Harbor Streams",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(theme.primary.copy(alpha = 0.2f))
                                        .padding(horizontal = 5.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "${filteredStreams.size} available",
                                        fontSize = 10.sp,
                                        color = theme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            if (selectedEpisode != null && item.type == "series") {
                                Text(
                                    text = "S${selectedEpisode?.season}:E${selectedEpisode?.episode} - ${selectedEpisode?.title}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        if (isLoadingStreams) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = theme.primary,
                                strokeWidth = 2.dp
                            )
                        }
                    }

                    // Resolution filter pills
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterPill("All", streamFilterRes == null) { streamFilterRes = null }
                        FilterPill("4K UHD", streamFilterRes == StreamResolution.RES_4K) { streamFilterRes = StreamResolution.RES_4K }
                        FilterPill("1080p", streamFilterRes == StreamResolution.RES_1080P) { streamFilterRes = StreamResolution.RES_1080P }
                        FilterPill("720p", streamFilterRes == StreamResolution.RES_720P) { streamFilterRes = StreamResolution.RES_720P }
                    }
                }
            }

            // Stream cards list
            if (isLoadingStreams) {
                item {
                    StreamRadarScanningCard()
                }
                item {
                    StreamLoadingSkeletonList(count = 4)
                }
            } else if (filteredStreams.isEmpty()) {
                item {
                    val isSourceLinked = streamResolverRepository.isStreamSourceLinked()
                    if (!isSourceLinked) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = theme.surface)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(theme.primary.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Bolt,
                                        contentDescription = "Torrentio",
                                        tint = theme.primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "No Stream Sources Linked",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Harbor displays zero fake streams. Configure Torrentio or connect your Stremio cloud account to aggregate real torrents and Debrid streams across major trackers.",
                                    fontSize = 12.sp,
                                    textAlign = TextAlign.Center,
                                    lineHeight = 16.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(14.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            scope.launch {
                                                isLoadingStreams = true
                                                streamResolverRepository.linkTorrentio()
                                                refreshStreams(item)
                                                Toast.makeText(context, "Torrentio linked! Aggregating torrent streams...", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = theme.primary,
                                            contentColor = Color.Black
                                        ),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Icon(imageVector = Icons.Default.Bolt, contentDescription = null, modifier = Modifier.size(15.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Quick Link", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                    }

                                    OutlinedButton(
                                        onClick = { showTorrentioSetup = true },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text("Setup Wizard", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                    }

                                    OutlinedButton(
                                        onClick = { showStremioLogin = true },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text("Login", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No torrent streams found on tracked sources for this selection.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(filteredStreams) { st ->
                    Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 5.dp)) {
                        StreamItemCard(
                            stream = st,
                            onPlay = {
                                val url = st.rawStream.url ?: if (!st.rawStream.infoHash.isNullOrBlank()) {
                                    "magnet:?xt=urn:btih:${st.rawStream.infoHash}"
                                } else ""

                                val displayTitle = if (selectedEpisode != null && item.type == "series") {
                                    "${item.name} - S${selectedEpisode?.season}:E${selectedEpisode?.episode}"
                                } else {
                                    item.name
                                }

                                if (url.startsWith("http://") || url.startsWith("https://")) {
                                    // Direct HTTP stream (Debrid-resolved or direct CDN stream) -> play in native video player!
                                    onPlayStream(
                                        displayTitle,
                                        url,
                                        item.id,
                                        selectedEpisode?.season,
                                        selectedEpisode?.episode,
                                        selectedEpisode?.title,
                                        item.poster,
                                        item.background
                                    )
                                } else if (url.startsWith("magnet:")) {
                                    val debridInfo = streamResolverRepository.getLinkedDebridInfo()
                                    if (debridInfo != null) {
                                        // Resolve magnet directly via Real-Debrid API to obtain high-speed HTTP video stream
                                        scope.launch {
                                            isResolvingDebrid = true
                                            val res = streamResolverRepository.resolveMagnetViaDebrid(url)
                                            isResolvingDebrid = false
                                            if (res.isSuccess) {
                                                val directHttpUrl = res.getOrThrow()
                                                onPlayStream(
                                                    displayTitle,
                                                    directHttpUrl,
                                                    item.id,
                                                    selectedEpisode?.season,
                                                    selectedEpisode?.episode,
                                                    selectedEpisode?.title,
                                                    item.poster,
                                                    item.background
                                                )
                                            } else {
                                                debridResolutionError = res.exceptionOrNull()?.message ?: "Torrent not cached in Debrid"
                                                pendingExternalMagnetUrl = url
                                            }
                                        }
                                    } else {
                                        // Prompt user to connect Debrid or choose external player
                                        showDebridRequiredDialog = url
                                    }
                                }
                            },
                            onExternalPlay = {
                                val url = st.rawStream.url ?: if (!st.rawStream.infoHash.isNullOrBlank()) {
                                    "magnet:?xt=urn:btih:${st.rawStream.infoHash}"
                                } else ""
                                if (url.isNotBlank()) {
                                    val intent = Intent(Intent.ACTION_VIEW).apply {
                                        if (url.startsWith("magnet:")) {
                                            data = Uri.parse(url)
                                        } else {
                                            setDataAndType(Uri.parse(url), "video/*")
                                        }
                                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                    }
                                    try {
                                        context.startActivity(Intent.createChooser(intent, "Open stream with"))
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "No external app installed to handle stream", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(40.dp))
            }
        }

        if (showTorrentioSetup) {
            val prefs = remember { context.getSharedPreferences("harbor_prefs", android.content.Context.MODE_PRIVATE) }
            TorrentioSetupDialog(
                streamResolverRepository = streamResolverRepository,
                initialCustomUrl = prefs.getString("custom_torrentio_url", "") ?: "",
                initialDebridKey = prefs.getString("debrid_key", "") ?: "",
                onDismiss = { showTorrentioSetup = false },
                onSaved = {
                    showTorrentioSetup = false
                    refreshStreams(item)
                }
            )
        }

        if (showStremioLogin) {
            StremioLoginDialog(
                stremioSession = stremioSession,
                apiClient = apiClient,
                addonRepository = addonRepository,
                onDismiss = { showStremioLogin = false },
                onSuccess = {
                    showStremioLogin = false
                    refreshStreams(item)
                }
            )
        }

        // Resolving direct HTTP stream via Debrid loading dialog
        if (isResolvingDebrid) {
            androidx.compose.material3.AlertDialog(
                onDismissRequest = {},
                containerColor = theme.surface,
                title = {
                    Text("Debrid Cloud Stream", fontWeight = FontWeight.Bold, color = Color.White)
                },
                text = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.padding(vertical = 8.dp)
                    ) {
                        CircularProgressIndicator(
                            color = theme.primary,
                            strokeWidth = 3.dp,
                            modifier = Modifier.size(36.dp)
                        )
                        Column {
                            Text("Resolving cached torrent...", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = Color.White)
                            Text("Generating direct high-speed HTTP link for native playback.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                },
                confirmButton = {}
            )
        }

        // Debrid Required Dialog (when clicking raw magnet without Debrid)
        if (showDebridRequiredDialog != null) {
            val magnetUrl = showDebridRequiredDialog ?: ""
            androidx.compose.material3.AlertDialog(
                onDismissRequest = { showDebridRequiredDialog = null },
                containerColor = theme.surface,
                icon = {
                    Icon(imageVector = Icons.Default.Bolt, contentDescription = null, tint = theme.primary)
                },
                title = {
                    Text("Native Streaming Setup", fontWeight = FontWeight.Bold, color = Color.White)
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Harbor's built-in native player requires Real-Debrid or TorBox to convert P2P torrents into direct high-speed HTTP streams without buffering.",
                            fontSize = 13.sp,
                            lineHeight = 18.sp,
                            color = Color.White
                        )
                        Text(
                            text = "If you don't have Real-Debrid, you can open this torrent with an external torrent player (like Flux or Stremio).",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                confirmButton = {
                    androidx.compose.material3.Button(
                        onClick = {
                            showDebridRequiredDialog = null
                            onOpenSettings()
                        },
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                            containerColor = theme.primary,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Setup Real-Debrid", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    androidx.compose.material3.OutlinedButton(
                        onClick = {
                            showDebridRequiredDialog = null
                            val intent = Intent(Intent.ACTION_VIEW).apply {
                                data = Uri.parse(magnetUrl)
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                            try {
                                context.startActivity(Intent.createChooser(intent, "Stream torrent with"))
                            } catch (e: Exception) {
                                Toast.makeText(context, "Magnet copied to clipboard!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("External App")
                    }
                }
            )
        }

        // Debrid Resolution Error / Not Cached Dialog
        if (debridResolutionError != null) {
            val magnetUrl = pendingExternalMagnetUrl ?: ""
            androidx.compose.material3.AlertDialog(
                onDismissRequest = {
                    debridResolutionError = null
                    pendingExternalMagnetUrl = null
                },
                containerColor = theme.surface,
                title = {
                    Text("Torrent Not Instantly Cached", fontWeight = FontWeight.Bold, color = Color.White)
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = debridResolutionError ?: "This torrent is not currently cached in your Debrid cloud.",
                            fontSize = 13.sp,
                            color = Color(0xFFEF4444)
                        )
                        Text(
                            text = "You can stream it using an external P2P torrent client (Flux / Stremio) or try another stream with higher seeds.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                confirmButton = {
                    if (magnetUrl.isNotBlank()) {
                        androidx.compose.material3.Button(
                            onClick = {
                                debridResolutionError = null
                                pendingExternalMagnetUrl = null
                                val intent = Intent(Intent.ACTION_VIEW).apply {
                                    data = Uri.parse(magnetUrl)
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                }
                                try {
                                    context.startActivity(Intent.createChooser(intent, "Stream torrent with"))
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Magnet copied!", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                containerColor = theme.primary,
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Stream with External App", fontWeight = FontWeight.Bold)
                        }
                    }
                },
                dismissButton = {
                    androidx.compose.material3.OutlinedButton(
                        onClick = {
                            debridResolutionError = null
                            pendingExternalMagnetUrl = null
                        },
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Dismiss")
                    }
                }
            )
        }
    }
}

@Composable
private fun FilterPill(text: String, isSelected: Boolean, onClick: () -> Unit) {
    val theme = LocalHarborTheme.current
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) theme.primary else theme.surface)
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text = text,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) Color.Black else Color.White
        )
    }
}
