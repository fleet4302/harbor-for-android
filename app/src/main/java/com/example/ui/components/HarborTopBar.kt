package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Anchor
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.LocalHarborTheme

@Composable
fun HarborTopBar(
    title: String = "HARBOR",
    showLogo: Boolean = true,
    onSearchClick: (() -> Unit)? = null,
    onWatchPartyClick: (() -> Unit)? = null,
    onSettingsClick: (() -> Unit)? = null
) {
    val theme = LocalHarborTheme.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (showLogo) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(theme.surfaceVariant, theme.surface)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Anchor,
                    contentDescription = "Harbor Logo",
                    tint = theme.primary,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
        }

        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Black,
                letterSpacing = 1.5.sp
            ),
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.weight(1f)
        )

        // Stremio Addon active pill indicator
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(theme.primary.copy(alpha = 0.15f))
                .padding(horizontal = 8.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF10B981))
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "v3 ENGINE",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = theme.primary
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        if (onSearchClick != null) {
            IconButton(
                onClick = onSearchClick,
                modifier = Modifier
                    .testTag("top_bar_search_button")
                    .size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
        }

        if (onWatchPartyClick != null) {
            IconButton(
                onClick = onWatchPartyClick,
                modifier = Modifier
                    .testTag("top_bar_watch_party_button")
                    .size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Group,
                    contentDescription = "Watch Party",
                    tint = theme.primary
                )
            }
        }

        if (onSettingsClick != null) {
            IconButton(
                onClick = onSettingsClick,
                modifier = Modifier
                    .testTag("top_bar_settings_button")
                    .size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
        }
    }
}
