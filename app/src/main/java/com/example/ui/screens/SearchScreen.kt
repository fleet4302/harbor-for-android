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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.example.data.model.StremioMetaSummary
import com.example.data.repository.CatalogRepository
import com.example.ui.components.MediaPosterCard
import com.example.ui.theme.LocalHarborTheme
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SearchScreen(
    catalogRepository: CatalogRepository,
    onMediaSelected: (type: String, id: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val theme = LocalHarborTheme.current
    val scope = rememberCoroutineScope()

    var searchQuery by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf<String?>("movie") }
    var searchResults by remember { mutableStateOf<List<StremioMetaSummary>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }
    var debounceJob by remember { mutableStateOf<Job?>(null) }

    val popularSuggestions = listOf("Dune", "Arcane", "Cyberpunk", "Oppenheimer", "Interstellar", "Batman", "Attack on Titan")

    fun performSearch(query: String, type: String?) {
        if (query.isBlank()) {
            searchResults = emptyList()
            isSearching = false
            return
        }
        debounceJob?.cancel()
        debounceJob = scope.launch {
            delay(350)
            isSearching = true
            try {
                val results = catalogRepository.search(query, type)
                searchResults = results
            } catch (e: Exception) {
                searchResults = emptyList()
            } finally {
                isSearching = false
            }
        }
    }

    LaunchedEffect(searchQuery, selectedType) {
        performSearch(searchQuery, selectedType)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(theme.background)
            .statusBarsPadding()
            .testTag("search_screen")
    ) {
        // Search bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search movies, series, anime across addons...") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = theme.primary
                )
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Clear",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = theme.surface,
                unfocusedContainerColor = theme.surface,
                focusedBorderColor = theme.primary,
                unfocusedBorderColor = theme.surfaceVariant,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            ),
            shape = RoundedCornerShape(16.dp),
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp)
                .testTag("search_input_field")
        )

        // Type filter row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val types = listOf(
                Pair("movie", "Movies"),
                Pair("series", "Series"),
                Pair(null, "All Types")
            )
            types.forEach { (typeKey, typeLabel) ->
                val isSelected = selectedType == typeKey
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSelected) theme.primary else theme.surfaceVariant)
                        .clickable { selectedType = typeKey }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = typeLabel,
                        color = if (isSelected) Color.Black else MaterialTheme.colorScheme.onSurface,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }

        // Suggestions pills
        if (searchQuery.isEmpty()) {
            Text(
                text = "Trending Searches",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 6.dp)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                popularSuggestions.forEach { suggestion ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(theme.surface)
                            .clickable { searchQuery = suggestion }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = suggestion,
                            fontSize = 12.sp,
                            color = theme.primary
                        )
                    }
                }
            }
        }

        // Search Content or Empty State
        if (isSearching) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = theme.primary)
            }
        } else if (searchQuery.isNotBlank() && searchResults.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier.size(54.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "No results found for \"$searchQuery\"",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "Check installed catalog addons in the Addons tab",
                        color = theme.primary,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            ) {
                items(searchResults) { media ->
                    MediaPosterCard(
                        media = media,
                        onClick = { onMediaSelected(media.type, media.id) }
                    )
                }
            }
        }
    }
}
