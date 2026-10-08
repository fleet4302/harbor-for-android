package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.LocalHarborTheme
import kotlinx.coroutines.delay

@Composable
fun shimmerBrush(showShimmer: Boolean = true, targetValue: Float = 1000f): Brush {
    return if (showShimmer) {
        val shimmerColors = listOf(
            Color(0xFF1E293B).copy(alpha = 0.6f),
            Color(0xFF334155).copy(alpha = 0.9f),
            Color(0xFF1E293B).copy(alpha = 0.6f)
        )
        val transition = rememberInfiniteTransition(label = "shimmer")
        val translateAnimation = transition.animateFloat(
            initialValue = 0f,
            targetValue = targetValue,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1100, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "shimmer_anim"
        )
        Brush.linearGradient(
            colors = shimmerColors,
            start = Offset.Zero,
            end = Offset(x = translateAnimation.value, y = translateAnimation.value)
        )
    } else {
        Brush.linearGradient(
            colors = listOf(Color.Transparent, Color.Transparent),
            start = Offset.Zero,
            end = Offset.Zero
        )
    }
}

/**
 * Animated Scanning Radar Card displaying real-time aggregation stages
 */
@Composable
fun StreamRadarScanningCard(
    modifier: Modifier = Modifier
) {
    val theme = LocalHarborTheme.current
    var scanPhase by remember { mutableIntStateOf(0) }

    val scanMessages = listOf(
        "Scanning DHT Swarms & Trackers...",
        "Querying Torrentio aggregation engine...",
        "Resolving Real-Debrid cached torrents...",
        "Calculating Harbor health & seed scores..."
    )

    LaunchedEffect(Unit) {
        while (true) {
            delay(1200)
            scanPhase = (scanPhase + 1) % scanMessages.size
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = theme.surfaceVariant.copy(alpha = 0.6f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(theme.primary.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(28.dp),
                    color = theme.primary,
                    strokeWidth = 2.dp
                )
                Icon(
                    imageVector = Icons.Default.Radar,
                    contentDescription = null,
                    tint = theme.primary,
                    modifier = Modifier.size(16.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "AGGREGATING STREAMS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        color = theme.primary,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFF10B981).copy(alpha = 0.2f))
                            .padding(horizontal = 4.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = "LIVE",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF10B981)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = scanMessages[scanPhase],
                    fontSize = 12.sp,
                    color = Color.White
                )
            }
        }
    }
}

/**
 * Animated Shimmer Skeleton for Stream Cards during aggregation
 */
@Composable
fun StreamSkeletonCard(modifier: Modifier = Modifier) {
    val brush = shimmerBrush()
    val theme = LocalHarborTheme.current

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = theme.surface)
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
                // Resolution Badge Skeleton
                Box(
                    modifier = Modifier
                        .size(width = 56.dp, height = 22.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(brush)
                )

                Spacer(modifier = Modifier.width(8.dp))

                // Provider Skeleton
                Box(
                    modifier = Modifier
                        .size(width = 70.dp, height = 18.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(brush)
                )

                Spacer(modifier = Modifier.weight(1f))

                // Play Button Skeleton
                Box(
                    modifier = Modifier
                        .size(width = 60.dp, height = 28.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(brush)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Title Line Skeleton
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .height(14.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(brush)
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Subtitle Line Skeleton
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    modifier = Modifier
                        .size(width = 65.dp, height = 12.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(brush)
                )
                Box(
                    modifier = Modifier
                        .size(width = 80.dp, height = 12.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(brush)
                )
                Box(
                    modifier = Modifier
                        .size(width = 50.dp, height = 12.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(brush)
                )
            }
        }
    }
}

@Composable
fun StreamLoadingSkeletonList(count: Int = 3) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        repeat(count) {
            StreamSkeletonCard()
        }
    }
}

/**
 * Detail Screen Loading Skeleton
 */
@Composable
fun MediaDetailSkeleton() {
    val brush = shimmerBrush()
    val theme = LocalHarborTheme.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(theme.background)
    ) {
        // Hero Backdrop Skeleton
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp)
                .background(brush)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            // Poster Skeleton
            Box(
                modifier = Modifier
                    .size(width = 100.dp, height = 145.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(brush)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .height(24.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(brush)
                )
                Spacer(modifier = Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.5f)
                        .height(16.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(brush)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    repeat(3) {
                        Box(
                            modifier = Modifier
                                .size(width = 45.dp, height = 20.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(brush)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Synopsis Skeleton
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.3f)
                    .height(16.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(brush)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(brush)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .height(12.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(brush)
            )
        }
    }
}
