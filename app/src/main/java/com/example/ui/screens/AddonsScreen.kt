package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.api.DefaultAddons
import com.example.data.local.AddonEntity
import com.example.data.repository.AddonRepository
import com.example.ui.theme.LocalHarborTheme
import kotlinx.coroutines.launch

@Composable
fun AddonsScreen(
    addonRepository: AddonRepository,
    modifier: Modifier = Modifier
) {
    val theme = LocalHarborTheme.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var showInstallDialog by remember { mutableStateOf(false) }
    var manifestUrlInput by remember { mutableStateOf("") }
    var isInstalling by remember { mutableStateOf(false) }

    val installedAddons by addonRepository.allAddons.collectAsState(initial = emptyList())

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(theme.background)
            .statusBarsPadding()
            .testTag("addons_screen")
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "ADDONS ROOM",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        ),
                        color = Color.White
                    )
                    Text(
                        text = "Stremio Addon Ecosystem",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(
                    onClick = {
                        scope.launch {
                            addonRepository.resetToDefaults()
                            Toast.makeText(context, "Addons reset to defaults", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Reset Addons",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Tab bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TabPill(
                    title = "Installed (${installedAddons.size})",
                    isSelected = selectedTabIndex == 0,
                    onClick = { selectedTabIndex = 0 }
                )
                TabPill(
                    title = "Community Store",
                    isSelected = selectedTabIndex == 1,
                    onClick = { selectedTabIndex = 1 }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (selectedTabIndex == 0) {
                // Installed Addons List
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(installedAddons) { addon ->
                        InstalledAddonCard(
                            addon = addon,
                            onToggle = { enabled ->
                                scope.launch {
                                    addonRepository.toggleAddon(addon.id, enabled)
                                }
                            },
                            onDelete = if (!addon.isOfficial) {
                                {
                                    scope.launch {
                                        addonRepository.uninstallAddon(addon.id)
                                    }
                                }
                            } else null
                        )
                    }
                    item { Spacer(modifier = Modifier.height(80.dp)) }
                }
            } else {
                // Community Addons Store
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(DefaultAddons.COMMUNITY_STORE) { listing ->
                        val isAlreadyInstalled = installedAddons.any { it.id == listing.id }
                        CommunityAddonCard(
                            listing = listing,
                            isInstalled = isAlreadyInstalled,
                            onInstall = {
                                scope.launch {
                                    val res = addonRepository.installAddonFromUrl(listing.manifestUrl)
                                    if (res.isSuccess) {
                                        Toast.makeText(context, "${listing.name} installed!", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "Could not install: ${res.exceptionOrNull()?.message}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        )
                    }
                    item { Spacer(modifier = Modifier.height(80.dp)) }
                }
            }
        }

        // Install Custom Addon FAB
        FloatingActionButton(
            onClick = {
                manifestUrlInput = ""
                showInstallDialog = true
            },
            containerColor = theme.primary,
            contentColor = Color.Black,
            shape = CircleShape,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .testTag("install_addon_fab")
        ) {
            Icon(imageVector = Icons.Default.Add, contentDescription = "Install Addon from URL")
        }

        // Install URL Dialog
        if (showInstallDialog) {
            AlertDialog(
                onDismissRequest = { if (!isInstalling) showInstallDialog = false },
                containerColor = theme.surface,
                title = {
                    Text(
                        text = "Install Addon by URL",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Column {
                        Text(
                            text = "Paste any Stremio manifest URL (https://.../manifest.json or stremio://...)",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = manifestUrlInput,
                            onValueChange = { manifestUrlInput = it },
                            placeholder = { Text("https://example.com/manifest.json") },
                            singleLine = true,
                            enabled = !isInstalling,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("addon_url_input")
                        )
                        if (isInstalling) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                CircularProgressIndicator(
                                    color = theme.primary,
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp
                                )
                                Text("Verifying manifest...", fontSize = 12.sp, color = theme.primary)
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (manifestUrlInput.isNotBlank()) {
                                scope.launch {
                                    isInstalling = true
                                    val res = addonRepository.installAddonFromUrl(manifestUrlInput)
                                    isInstalling = false
                                    if (res.isSuccess) {
                                        Toast.makeText(context, "Addon installed successfully!", Toast.LENGTH_SHORT).show()
                                        showInstallDialog = false
                                    } else {
                                        Toast.makeText(context, "Failed: ${res.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                                    }
                                }
                            }
                        },
                        enabled = !isInstalling && manifestUrlInput.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = theme.primary, contentColor = Color.Black),
                        modifier = Modifier.testTag("confirm_install_button")
                    ) {
                        Text("Install")
                    }
                },
                dismissButton = {
                    OutlinedButton(
                        onClick = { showInstallDialog = false },
                        enabled = !isInstalling
                    ) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
private fun InstalledAddonCard(
    addon: AddonEntity,
    onToggle: (Boolean) -> Unit,
    onDelete: (() -> Unit)?
) {
    val theme = LocalHarborTheme.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = theme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(theme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Extension,
                    contentDescription = null,
                    tint = if (addon.isEnabled) theme.primary else Color.Gray,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = addon.name,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "v${addon.version}",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (addon.description.isNotBlank()) {
                    Text(
                        text = addon.description,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }

                // Capability pills
                Row(
                    modifier = Modifier.padding(top = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (addon.supportsCatalog) CapabilityTag("Catalog")
                    if (addon.supportsStream) CapabilityTag("Streams")
                    if (addon.supportsSubtitles) CapabilityTag("Subtitles")
                    if (addon.isOfficial) CapabilityTag("Official", isHighlight = true)
                }
            }

            // Actions
            if (onDelete != null) {
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Uninstall Addon",
                        tint = Color(0xFFEF4444)
                    )
                }
            }

            Switch(
                checked = addon.isEnabled,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = theme.primary,
                    checkedTrackColor = theme.primary.copy(alpha = 0.3f),
                    uncheckedTrackColor = theme.surfaceVariant
                ),
                modifier = Modifier.testTag("toggle_addon_${addon.id}")
            )
        }
    }
}

@Composable
private fun CommunityAddonCard(
    listing: com.example.data.api.CommunityAddonListing,
    isInstalled: Boolean,
    onInstall: () -> Unit
) {
    val theme = LocalHarborTheme.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = theme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(theme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Extension,
                    contentDescription = null,
                    tint = theme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = listing.name,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = Color(0xFFFFB703),
                            modifier = Modifier.size(11.dp)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = "${listing.stars}",
                            fontSize = 10.sp,
                            color = Color(0xFFFFB703),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Text(
                    text = listing.description,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    modifier = Modifier.padding(top = 2.dp)
                )

                Row(
                    modifier = Modifier.padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    CapabilityTag(listing.category)
                    if (listing.isDebridSupported) CapabilityTag("Debrid", isHighlight = true)
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            if (isInstalled) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF10B981).copy(alpha = 0.2f))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Installed",
                            tint = Color(0xFF10B981),
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "Installed",
                            color = Color(0xFF10B981),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            } else {
                Button(
                    onClick = onInstall,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = theme.primary,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("install_${listing.id}")
                ) {
                    Text("Get", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun CapabilityTag(label: String, isHighlight: Boolean = false) {
    val theme = LocalHarborTheme.current
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(
                if (isHighlight) theme.primary.copy(alpha = 0.2f) else theme.surfaceVariant
            )
            .padding(horizontal = 5.dp, vertical = 2.dp)
    ) {
        Text(
            text = label,
            fontSize = 9.sp,
            fontWeight = FontWeight.Medium,
            color = if (isHighlight) theme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun TabPill(title: String, isSelected: Boolean, onClick: () -> Unit) {
    val theme = LocalHarborTheme.current
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (isSelected) theme.primary else theme.surfaceVariant)
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(
            text = title,
            color = if (isSelected) Color.Black else MaterialTheme.colorScheme.onSurface,
            fontSize = 13.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
        )
    }
}
