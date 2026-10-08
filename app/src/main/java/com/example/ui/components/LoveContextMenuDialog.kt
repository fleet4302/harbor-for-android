package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.StremioMetaSummary
import com.example.ui.theme.LocalHarborTheme

@Composable
fun MediaItemContextMenuDialog(
    media: StremioMetaSummary,
    isBookmarked: Boolean = false,
    onAutoPlay: () -> Unit,
    onToggleBookmark: () -> Unit,
    onMarkWatched: () -> Unit,
    onViewDetails: () -> Unit,
    onDismiss: () -> Unit
) {
    val theme = LocalHarborTheme.current
    val context = LocalContext.current

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = theme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = media.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color.White,
                    maxLines = 1
                )
                Text(
                    text = "${media.type.replaceFirstChar { it.uppercase() }} ${if (!media.releaseInfo.isNullOrBlank()) "• ${media.releaseInfo}" else ""}",
                    fontSize = 12.sp,
                    color = theme.primary
                )

                Spacer(modifier = Modifier.height(16.dp))

                ContextMenuItem(
                    icon = Icons.Default.PlayArrow,
                    label = "Auto Play Best Stream",
                    tint = theme.primary,
                    onClick = {
                        onDismiss()
                        onAutoPlay()
                    }
                )

                ContextMenuItem(
                    icon = if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                    label = if (isBookmarked) "Remove from Watchlist" else "Save to Watchlist",
                    onClick = {
                        onDismiss()
                        onToggleBookmark()
                    }
                )

                ContextMenuItem(
                    icon = Icons.Default.CheckCircle,
                    label = "Mark as Watched",
                    onClick = {
                        onDismiss()
                        onMarkWatched()
                    }
                )

                ContextMenuItem(
                    icon = Icons.Default.Info,
                    label = "View Full Details & Cast",
                    onClick = {
                        onDismiss()
                        onViewDetails()
                    }
                )

                ContextMenuItem(
                    icon = Icons.Default.ContentCopy,
                    label = "Copy Media ID (${media.id})",
                    onClick = {
                        onDismiss()
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("Media ID", media.id)
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, "Copied ID: ${media.id}", Toast.LENGTH_SHORT).show()
                    }
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Close", color = Color.Gray, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun BackgroundContextMenuDialog(
    onRefresh: () -> Unit,
    onOpenSearch: () -> Unit,
    onOpenSettings: () -> Unit,
    onSyncLibrary: () -> Unit,
    onDismiss: () -> Unit
) {
    val theme = LocalHarborTheme.current

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = theme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = "Quick Actions Menu",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color.White
                )
                Text(
                    text = "Quick Context Options",
                    fontSize = 11.sp,
                    color = theme.primary
                )

                Spacer(modifier = Modifier.height(16.dp))

                ContextMenuItem(
                    icon = Icons.Default.Refresh,
                    label = "Refresh Catalogs & Streams",
                    onClick = {
                        onDismiss()
                        onRefresh()
                    }
                )

                ContextMenuItem(
                    icon = Icons.Default.Search,
                    label = "Search Movies, Shows & Anime",
                    onClick = {
                        onDismiss()
                        onOpenSearch()
                    }
                )

                ContextMenuItem(
                    icon = Icons.Default.Sync,
                    label = "Sync Stremio & Trakt Library",
                    tint = theme.primary,
                    onClick = {
                        onDismiss()
                        onSyncLibrary()
                    }
                )

                ContextMenuItem(
                    icon = Icons.Default.Settings,
                    label = "Open App Settings",
                    onClick = {
                        onDismiss()
                        onOpenSettings()
                    }
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Close", color = Color.Gray, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun ContextMenuItem(
    icon: ImageVector,
    label: String,
    tint: Color = Color.White,
    onClick: () -> Unit
) {
    val theme = LocalHarborTheme.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(vertical = 10.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = Color.White
        )
    }
}
