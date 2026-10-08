package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.HarborParsedStream
import com.example.data.model.StreamResolution
import com.example.ui.theme.LocalHarborTheme

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StreamItemCard(
    stream: HarborParsedStream,
    onPlay: () -> Unit,
    onExternalPlay: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val theme = LocalHarborTheme.current

    val resBadgeBg = when (stream.resolution) {
        StreamResolution.RES_4K -> Color(0xFFF59E0B)
        StreamResolution.RES_1080P -> Color(0xFF10B981)
        StreamResolution.RES_720P -> Color(0xFF3B82F6)
        StreamResolution.RES_480P -> Color(0xFF6B7280)
        StreamResolution.RES_CAM -> Color(0xFFEF4444)
        StreamResolution.RES_UNKNOWN -> theme.primary
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("stream_card_${stream.addonName}_${stream.resolution.name}")
            .clickable { onPlay() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = theme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Resolution Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(resBadgeBg)
                        .padding(horizontal = 6.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = stream.resolutionBadge,
                        color = Color.Black,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Debrid badge
                if (stream.isDebrid) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(theme.primary.copy(alpha = 0.2f))
                            .padding(horizontal = 6.dp, vertical = 3.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Bolt,
                                contentDescription = "Debrid Cached",
                                tint = theme.primary,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = stream.debridProvider ?: "DEBRID+",
                                color = theme.primary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                }

                Text(
                    text = stream.addonName,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )

                // Actions
                if (onExternalPlay != null) {
                    IconButton(
                        onClick = onExternalPlay,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.OpenInNew,
                            contentDescription = "External Player",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(theme.primary)
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Play Stream",
                            tint = Color.Black,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = "Play",
                            color = Color.Black,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stream.displayTitle,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Tech specs badges
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (stream.isDolbyVision) {
                    SpecBadge("Dolby Vision", Color(0xFF8B5CF6))
                }
                if (stream.isHdr) {
                    SpecBadge("HDR10", Color(0xFFEC4899))
                }
                if (!stream.codec.isNullOrBlank()) {
                    SpecBadge(stream.codec, Color(0xFF64748B))
                }
                if (!stream.audioChannels.isNullOrBlank()) {
                    SpecBadge(stream.audioChannels, Color(0xFF475569))
                }
                if (stream.seeders != null) {
                    SpecBadge("👤 ${stream.seeders} seeds", Color(0xFF10B981))
                }
                if (!stream.fileSizeFormatted.isNullOrBlank()) {
                    SpecBadge("💾 ${stream.fileSizeFormatted}", Color(0xFF3B82F6))
                }
            }
        }
    }
}

@Composable
private fun SpecBadge(label: String, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(color.copy(alpha = 0.2f))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            color = color
        )
    }
}
