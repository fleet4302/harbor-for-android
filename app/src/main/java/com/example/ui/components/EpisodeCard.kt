package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.model.StremioVideo
import com.example.ui.theme.LocalHarborTheme

@Composable
fun EpisodeCard(
    episode: StremioVideo,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false
) {
    val theme = LocalHarborTheme.current
    val context = LocalContext.current

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("episode_card_${episode.id}")
            .clickable { onSelect() },
        shape = RoundedCornerShape(12.dp),
        border = if (isSelected) BorderStroke(1.5.dp, theme.primary) else BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) theme.primary.copy(alpha = 0.12f) else theme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Thumbnail
            Box(
                modifier = Modifier
                    .width(110.dp)
                    .height(68.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(theme.surfaceVariant)
            ) {
                if (!episode.thumbnail.isNullOrBlank()) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(episode.thumbnail)
                            .crossfade(true)
                            .build(),
                        contentDescription = episode.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // Play overlay
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(if (isSelected) theme.primary else Color.Black.copy(alpha = 0.6f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Play Episode",
                        tint = if (isSelected) Color.Black else Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                val epNum = episode.episode ?: 1
                val rawTitle = episode.title?.trim() ?: ""
                val displayTitle = when {
                    rawTitle.isBlank() -> "Episode $epNum"
                    rawTitle.equals("Episode $epNum", ignoreCase = true) ||
                            rawTitle.equals("episode $epNum", ignoreCase = true) ||
                            rawTitle.equals("Ep $epNum", ignoreCase = true) ||
                            rawTitle.equals("E$epNum", ignoreCase = true) ||
                            rawTitle.equals("$epNum", ignoreCase = true) -> "Episode $epNum"
                    rawTitle.startsWith("Episode $epNum - ", ignoreCase = true) -> "E$epNum • ${rawTitle.substringAfter("- ").trim()}"
                    rawTitle.startsWith("Episode $epNum: ", ignoreCase = true) -> "E$epNum • ${rawTitle.substringAfter(": ").trim()}"
                    rawTitle.startsWith("E$epNum - ", ignoreCase = true) -> "E$epNum • ${rawTitle.substringAfter("- ").trim()}"
                    else -> "E$epNum • $rawTitle"
                }

                Text(
                    text = displayTitle,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold
                    ),
                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (!episode.overview.isNullOrBlank()) {
                    Text(
                        text = episode.overview,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }

                if (!episode.released.isNullOrBlank()) {
                    Text(
                        text = episode.released,
                        fontSize = 10.sp,
                        color = theme.primary,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Action play pill
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (isSelected) theme.primary else theme.primary.copy(alpha = 0.15f))
                    .clickable { onSelect() }
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Watch Episode",
                        tint = if (isSelected) Color.Black else theme.primary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "Streams",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) Color.Black else theme.primary
                    )
                }
            }
        }
    }
}
