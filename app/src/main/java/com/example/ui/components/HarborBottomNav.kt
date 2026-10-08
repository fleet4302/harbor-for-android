package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.LocalHarborTheme

enum class HarborNavTab(val title: String, val icon: ImageVector) {
    DISCOVER("Discover", Icons.Default.Home),
    SEARCH("Search", Icons.Default.Search),
    VAULT("Vault", Icons.Default.BookmarkBorder),
    ADDONS("Addons", Icons.Default.Extension),
    PARTY("Party", Icons.Default.Group),
    SETTINGS("Settings", Icons.Default.Settings)
}

@Composable
fun HarborBottomNav(
    currentTab: HarborNavTab,
    onTabSelected: (HarborNavTab) -> Unit
) {
    val theme = LocalHarborTheme.current

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(theme.surface.copy(alpha = 0.96f))
            .navigationBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp, horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            HarborNavTab.entries.forEach { tab ->
                val isSelected = currentTab == tab
                val iconColor = if (isSelected) theme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f)
                val textColor = if (isSelected) theme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f)

                Column(
                    modifier = Modifier
                        .testTag("nav_tab_${tab.name.lowercase()}")
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onTabSelected(tab) }
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = tab.icon,
                        contentDescription = tab.title,
                        tint = iconColor,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = tab.title,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = textColor,
                        maxLines = 1
                    )
                }
            }
        }
    }
}
