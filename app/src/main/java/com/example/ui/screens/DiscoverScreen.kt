package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.StremioAccountSession
import com.example.data.model.StremioMetaSummary
import com.example.data.repository.CatalogRepository
import com.example.data.repository.VaultRepository
import com.example.ui.components.BackgroundContextMenuDialog
import com.example.ui.components.MediaPosterCard
import com.example.ui.theme.LocalHarborTheme
import kotlinx.coroutines.launch

@Composable
fun DiscoverScreen(
    catalogRepository: CatalogRepository,
    vaultRepository: VaultRepository,
    onMediaSelected: (type: String, id: String) -> Unit,
    onPlayDirect: (title: String, streamUrl: String) -> Unit,
    onOpenSearch: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val theme = LocalHarborTheme.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var activePillTab by remember { mutableStateOf("continue_watching") } // "continue_watching", "tv_shows", "movies"
    var multiSelectedCategories by remember { mutableStateOf<Set<String>>(emptySet()) }
    var searchQuery by remember { mutableStateOf("") }
    var showBackgroundMenu by remember { mutableStateOf(false) }

    var catalogItems by remember { mutableStateOf<List<StremioMetaSummary>>(emptyList()) }
    var isLoadingCatalog by remember { mutableStateOf(false) }
    var isFetchingMoreCatalog by remember { mutableStateOf(false) }
    var currentSkip by remember { mutableIntStateOf(0) }

    val continueWatchingList by vaultRepository.continueWatching.collectAsState(initial = emptyList())

    val categories = listOf("Action", "Sci-Fi", "Drama", "Animation", "Comedy", "Thriller", "Adventure", "Fantasy", "Horror", "Mystery", "Documentary")

    // Load catalog items whenever activePillTab changes
    fun fetchInitialCatalog() {
        if (activePillTab == "continue_watching") return
        scope.launch {
            isLoadingCatalog = true
            currentSkip = 0
            try {
                val mediaType = if (activePillTab == "tv_shows") "series" else "movie"
                val results = catalogRepository.getCatalogPage(mediaType, skip = 0)
                catalogItems = results
            } catch (e: Exception) {
                catalogItems = emptyList()
            } finally {
                isLoadingCatalog = false
            }
        }
    }

    LaunchedEffect(activePillTab) {
        fetchInitialCatalog()
    }

    // Function to load next page (infinite scrolling)
    val fetchNextPage = {
        if (!isFetchingMoreCatalog && activePillTab != "continue_watching") {
            isFetchingMoreCatalog = true
            val nextSkip = currentSkip + 100
            currentSkip = nextSkip
            scope.launch {
                try {
                    val mediaType = if (activePillTab == "tv_shows") "series" else "movie"
                    val newItems = catalogRepository.getCatalogPage(mediaType, skip = nextSkip)
                    if (newItems.isNotEmpty()) {
                        val existingIds = catalogItems.map { it.id }.toSet()
                        val filteredNew = newItems.filter { !existingIds.contains(it.id) }
                        catalogItems = catalogItems + filteredNew
                    }
                } catch (e: Exception) {
                    // Fail silently
                } finally {
                    isFetchingMoreCatalog = false
                }
            }
        }
    }

    // Filter displayed items by category multi-select & search query
    val displayedItems = remember(activePillTab, catalogItems, continueWatchingList, multiSelectedCategories, searchQuery) {
        if (activePillTab == "continue_watching") {
            var list = continueWatchingList.map { history ->
                val posterUrl = history.poster ?: if (history.mediaId.startsWith("tt")) {
                    "https://images.metahub.space/poster/medium/${history.mediaId}/img"
                } else null
                StremioMetaSummary(
                    id = history.mediaId,
                    type = history.type,
                    name = history.title,
                    poster = posterUrl,
                    background = history.background ?: posterUrl,
                    releaseInfo = if (history.season != null && history.episode != null) {
                        "S${history.season}:E${history.episode}"
                    } else null
                )
            }
            if (searchQuery.isNotBlank()) {
                list = list.filter { it.name.contains(searchQuery, ignoreCase = true) }
            }
            list
        } else {
            var list = catalogItems
            if (searchQuery.isNotBlank()) {
                list = list.filter { it.name.contains(searchQuery, ignoreCase = true) }
            }
            if (multiSelectedCategories.isNotEmpty()) {
                list = list.filter { item ->
                    multiSelectedCategories.all { cat ->
                        item.genres?.any { g -> g.contains(cat, ignoreCase = true) } == true ||
                        (cat == "Sci-Fi" && item.genres?.any { it.contains("Science", true) || it.contains("Cyber", true) } == true) ||
                        (cat == "Action" && item.genres?.any { it.contains("Adventure", true) || it.contains("Superhero", true) } == true) ||
                        (cat == "Horror" && item.genres?.any { it.contains("Mystery", true) || it.contains("Thriller", true) } == true)
                    }
                }
            }
            list
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(theme.background)
            .pointerInput(Unit) {
                detectTapGestures(
                    onLongPress = { showBackgroundMenu = true }
                )
            }
            .testTag("discover_screen")
    ) {
        // TOP FLOATING HEADER BAR: Continue Watching, TV Shows, Movies | Search Bubble, Settings Bubble
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Pills Group
            Row(
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                PillButton(
                    title = "Continue Watching",
                    isSelected = activePillTab == "continue_watching",
                    onClick = {
                        activePillTab = "continue_watching"
                        multiSelectedCategories = emptySet()
                    }
                )
                PillButton(
                    title = "TV Shows",
                    isSelected = activePillTab == "tv_shows",
                    onClick = {
                        activePillTab = "tv_shows"
                        multiSelectedCategories = emptySet()
                    }
                )
                PillButton(
                    title = "Movies",
                    isSelected = activePillTab == "movies",
                    onClick = {
                        activePillTab = "movies"
                        multiSelectedCategories = emptySet()
                    }
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Search Bubble & Settings Bubble
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Search Bubble
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(theme.surfaceVariant)
                        .clickable { onOpenSearch() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Settings Bubble
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(theme.surfaceVariant)
                        .clickable { onOpenSettings() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // Search filter within current tab
        if (activePillTab != "continue_watching" || continueWatchingList.size > 5) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = {
                    Text(
                        text = when (activePillTab) {
                            "continue_watching" -> "Filter continue watching..."
                            "tv_shows" -> "Filter TV shows..."
                            else -> "Filter movies..."
                        },
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = theme.primary, modifier = Modifier.size(18.dp))
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Clear", tint = Color.White, modifier = Modifier.size(16.dp))
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = theme.primary,
                    unfocusedBorderColor = theme.surfaceVariant,
                    focusedContainerColor = theme.surface,
                    unfocusedContainerColor = theme.surface
                ),
                singleLine = true
            )
        }

        // Multi-Category Filtering Bar for Movies & TV Shows
        if (activePillTab == "movies" || activePillTab == "tv_shows") {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.FilterList, contentDescription = "Filter", tint = theme.primary, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (multiSelectedCategories.isEmpty()) "Multi-Category Filter:" else "Matching ALL (${multiSelectedCategories.joinToString(" + ")})",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = theme.primary
                        )
                    }

                    if (multiSelectedCategories.isNotEmpty()) {
                        TextButton(
                            onClick = { multiSelectedCategories = emptySet() },
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text("Clear", fontSize = 11.sp, color = Color(0xFFEF4444), fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    categories.forEach { category ->
                        val isSelected = multiSelectedCategories.contains(category)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(18.dp))
                                .background(if (isSelected) theme.primary else theme.surfaceVariant)
                                .clickable {
                                    multiSelectedCategories = if (isSelected) {
                                        multiSelectedCategories - category
                                    } else {
                                        multiSelectedCategories + category
                                    }
                                }
                                .padding(horizontal = 12.dp, vertical = 5.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Selected",
                                        tint = Color.Black,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                }
                                Text(
                                    text = category,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) Color.Black else Color.White
                                )
                            }
                        }
                    }
                }
            }
        }

        // MAIN CONTENT AREA GRID
        if (isLoadingCatalog && activePillTab != "continue_watching") {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = theme.primary, modifier = Modifier.size(36.dp))
            }
        } else if (activePillTab == "continue_watching" && displayedItems.isEmpty()) {
            // Empty state for continue watching
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "No Actively Watched Titles Yet",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Select TV Shows or Movies above to start watching, or tap below to sync your Stremio Cloud watched library.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    TextButton(
                        onClick = {
                            val stremioSession = StremioAccountSession(context)
                            val authKey = stremioSession.getAuthKey()
                            if (authKey.isNullOrBlank()) {
                                Toast.makeText(context, "Log in to Stremio in Settings to sync watched library", Toast.LENGTH_LONG).show()
                            } else {
                                scope.launch {
                                    try {
                                        val apiClient = com.example.data.api.StremioApiClient()
                                        val libRes = apiClient.getLibraryItems(authKey)
                                        if (libRes.isSuccess) {
                                            val count = vaultRepository.syncLibraryFromStremio(libRes.getOrThrow())
                                            Toast.makeText(context, "Synced $count items from Stremio Cloud!", Toast.LENGTH_SHORT).show()
                                        }
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Sync error: ${e.message}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        }
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Refresh, contentDescription = "Sync", tint = theme.primary)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Sync Stremio Cloud", color = theme.primary, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 115.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                contentPadding = PaddingValues(bottom = 40.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(displayedItems) { media ->
                    val historyMatch = continueWatchingList.firstOrNull { it.mediaId == media.id }
                    val fraction = if (historyMatch != null && historyMatch.durationMs > 0) {
                        historyMatch.positionMs.toFloat() / historyMatch.durationMs.toFloat()
                    } else null

                    MediaPosterCard(
                        media = media,
                        progressFraction = fraction,
                        onClick = { onMediaSelected(media.type, media.id) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (activePillTab != "continue_watching") {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isFetchingMoreCatalog) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = theme.primary,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                TextButton(onClick = { fetchNextPage() }) {
                                    Text(
                                        text = "Load More Titles...",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = theme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showBackgroundMenu) {
        BackgroundContextMenuDialog(
            onRefresh = {
                fetchInitialCatalog()
                Toast.makeText(context, "Refreshing catalog...", Toast.LENGTH_SHORT).show()
            },
            onOpenSearch = { onOpenSearch() },
            onOpenSettings = { onOpenSettings() },
            onSyncLibrary = {
                val stremioSession = StremioAccountSession(context)
                val authKey = stremioSession.getAuthKey()
                if (authKey.isNullOrBlank()) {
                    Toast.makeText(context, "Log in to Stremio in Settings to sync watched library", Toast.LENGTH_LONG).show()
                } else {
                    scope.launch {
                        try {
                            val apiClient = com.example.data.api.StremioApiClient()
                            val libRes = apiClient.getLibraryItems(authKey)
                            if (libRes.isSuccess) {
                                val count = vaultRepository.syncLibraryFromStremio(libRes.getOrThrow())
                                Toast.makeText(context, "Synced $count items from Stremio Cloud!", Toast.LENGTH_SHORT).show()
                            }
                        } catch (e: Exception) {
                            Toast.makeText(context, "Sync error: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            },
            onDismiss = { showBackgroundMenu = false }
        )
    }
}

@Composable
private fun PillButton(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val theme = LocalHarborTheme.current
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (isSelected) theme.primary else theme.surfaceVariant)
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(
            text = title,
            color = if (isSelected) Color.Black else Color.White,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
        )
    }
}
