package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.WatchHistoryEntity
import com.example.data.model.StremioMetaSummary
import com.example.data.repository.CatalogRepository
import com.example.data.repository.VaultRepository
import com.example.ui.components.HeroBillboard
import com.example.ui.components.MediaPosterCard
import com.example.ui.components.shimmerBrush
import com.example.ui.theme.LocalHarborTheme
import kotlinx.coroutines.launch

@Composable
fun DiscoverScreen(
    catalogRepository: CatalogRepository,
    vaultRepository: VaultRepository,
    onMediaSelected: (type: String, id: String) -> Unit,
    onPlayDirect: (title: String, streamUrl: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val theme = LocalHarborTheme.current
    val scope = rememberCoroutineScope()

    var selectedType by remember { mutableStateOf("all") }
    var selectedGenre by remember { mutableStateOf("All") }
    var isLoading by remember { mutableStateOf(true) }

    var popularMovies by remember { mutableStateOf<List<StremioMetaSummary>>(emptyList()) }
    var popularSeries by remember { mutableStateOf<List<StremioMetaSummary>>(emptyList()) }
    var featuredItem by remember { mutableStateOf<StremioMetaSummary?>(null) }

    val continueWatching by vaultRepository.continueWatching.collectAsState(initial = emptyList())

    val genres = listOf("All", "Action", "Sci-Fi", "Drama", "Animation", "Comedy", "Thriller", "Adventure", "Fantasy")

    fun loadData() {
        scope.launch {
            isLoading = true
            try {
                val movies = catalogRepository.getPopularMovies(if (selectedGenre == "All") null else selectedGenre)
                val series = catalogRepository.getPopularSeries(if (selectedGenre == "All") null else selectedGenre)
                popularMovies = movies
                popularSeries = series
                if (featuredItem == null) {
                    featuredItem = movies.firstOrNull() ?: series.firstOrNull()
                }
            } catch (e: Exception) {
                // Handled gracefully via repository fallbacks
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(selectedGenre) {
        loadData()
    }

    if (isLoading && popularMovies.isEmpty() && popularSeries.isEmpty()) {
        val brush = shimmerBrush()
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(theme.background)
        ) {
            // Billboard skeleton
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .background(brush)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Pills skeleton
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                repeat(3) {
                    Box(
                        modifier = Modifier
                            .size(width = 90.dp, height = 32.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(brush)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Posters skeleton row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                repeat(3) {
                    Box(
                        modifier = Modifier
                            .size(width = 110.dp, height = 165.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(brush)
                    )
                }
            }
        }
        return
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(theme.background)
            .testTag("discover_screen")
    ) {
        // Hero Billboard
        featuredItem?.let { featured ->
            item {
                HeroBillboard(
                    featured = featured,
                    onPlayClick = {
                        onMediaSelected(featured.type, featured.id)
                    },
                    onDetailsClick = {
                        onMediaSelected(featured.type, featured.id)
                    }
                )
            }
        }

        // Category Type Filter Pills (All / Movies / Series)
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FilterTabPill(
                    title = "All Catalogs",
                    isSelected = selectedType == "all",
                    onClick = { selectedType = "all" }
                )
                FilterTabPill(
                    title = "Movies",
                    isSelected = selectedType == "movie",
                    onClick = { selectedType = "movie" }
                )
                FilterTabPill(
                    title = "TV Series",
                    isSelected = selectedType == "series",
                    onClick = { selectedType = "series" }
                )
            }
        }

        // Genre filter row
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                genres.forEach { genre ->
                    val isSelected = selectedGenre == genre
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(
                                if (isSelected) theme.primary else theme.surfaceVariant
                            )
                            .clickable { selectedGenre = genre }
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = genre,
                            color = if (isSelected) Color.Black else MaterialTheme.colorScheme.onSurface,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        }

        // Continue Watching Shelf (Harbor Vault)
        if (continueWatching.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(16.dp))
                SectionHeader(title = "Continue Watching", subtitle = "Harbor Vault")
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(continueWatching) { historyItem ->
                        val summary = StremioMetaSummary(
                            id = historyItem.mediaId,
                            type = historyItem.type,
                            name = historyItem.title,
                            poster = historyItem.poster,
                            background = historyItem.background,
                            releaseInfo = if (historyItem.season != null && historyItem.episode != null) {
                                "S${historyItem.season}:E${historyItem.episode}"
                            } else null
                        )
                        val fraction = if (historyItem.durationMs > 0) {
                            historyItem.positionMs.toFloat() / historyItem.durationMs.toFloat()
                        } else 0.5f

                        MediaPosterCard(
                            media = summary,
                            progressFraction = fraction,
                            onClick = {
                                onMediaSelected(historyItem.type, historyItem.mediaId)
                            }
                        )
                    }
                }
            }
        }

        // Popular Movies Carousel
        if (selectedType == "all" || selectedType == "movie") {
            item {
                Spacer(modifier = Modifier.height(20.dp))
                SectionHeader(title = "Popular Movies", subtitle = "Cinemeta Addon")
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(popularMovies) { movie ->
                        MediaPosterCard(
                            media = movie,
                            onClick = { onMediaSelected("movie", movie.id) }
                        )
                    }
                }
            }
        }

        // Trending TV Series Carousel
        if (selectedType == "all" || selectedType == "series") {
            item {
                Spacer(modifier = Modifier.height(20.dp))
                SectionHeader(title = "Trending Series", subtitle = "Cinemeta Addon")
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(popularSeries) { series ->
                        MediaPosterCard(
                            media = series,
                            onClick = { onMediaSelected("series", series.id) }
                        )
                    }
                }
            }
        }

        // Anime & Animation Showcase
        item {
            Spacer(modifier = Modifier.height(20.dp))
            SectionHeader(title = "Anime & Sci-Fi", subtitle = "Kitsu & CyberFlix")
            val animeItems = (popularMovies + popularSeries).filter {
                it.genres?.any { g -> g.contains("Animation", true) || g.contains("Sci-Fi", true) } == true
            }
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(animeItems) { item ->
                    MediaPosterCard(
                        media = item,
                        onClick = { onMediaSelected(item.type, item.id) }
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
private fun FilterTabPill(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val theme = LocalHarborTheme.current
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (isSelected) theme.surfaceVariant else Color.Transparent)
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 6.dp)
    ) {
        Text(
            text = title,
            color = if (isSelected) theme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 13.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
        )
    }
}

@Composable
private fun SectionHeader(title: String, subtitle: String? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onBackground
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = "See All",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
    }
}
