package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Tune
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
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import com.example.data.api.CommunityAddonListing
import com.example.data.api.DefaultAddons
import com.example.data.api.StremioApiClient
import com.example.data.local.AddonEntity
import com.example.data.local.StremioAccountSession
import com.example.data.repository.AddonRepository
import com.example.data.repository.StreamResolverRepository
import com.example.data.repository.VaultRepository
import com.example.ui.components.StremioLoginDialog
import com.example.ui.components.TorrentioSetupDialog
import com.example.ui.theme.LocalHarborTheme
import kotlinx.coroutines.launch

@Composable
fun AddonsScreen(
    addonRepository: AddonRepository,
    stremioSession: StremioAccountSession? = null,
    vaultRepository: VaultRepository? = null,
    streamResolverRepository: StreamResolverRepository? = null,
    modifier: Modifier = Modifier
) {
    val theme = LocalHarborTheme.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val apiClient = remember { StremioApiClient() }

    var selectedTabIndex by remember { mutableIntStateOf(0) } // 0: Installed, 1: Store
    var selectedCategoryFilter by remember { mutableStateOf("All") }

    var showInstallDialog by remember { mutableStateOf(false) }
    var showLoginDialog by remember { mutableStateOf(false) }
    var showTorrentioSetup by remember { mutableStateOf(false) }

    var manifestUrlInput by remember { mutableStateOf("") }
    var isInstalling by remember { mutableStateOf(false) }
    var isSyncingStremio by remember { mutableStateOf(false) }

    val installedAddons by addonRepository.allAddons.collectAsState(initial = emptyList())
    val categories = listOf("All", "Streams", "Catalogs", "Subtitles", "Anime", "Free Cinema")

    val filteredStore = remember(selectedCategoryFilter) {
        if (selectedCategoryFilter == "All") {
            DefaultAddons.COMMUNITY_STORE
        } else {
            DefaultAddons.COMMUNITY_STORE.filter { it.category.equals(selectedCategoryFilter, ignoreCase = true) }
        }
    }

    fun triggerStremioSync() {
        val authKey = stremioSession?.getAuthKey()
        if (authKey.isNullOrBlank()) {
            showLoginDialog = true
            return
        }
        val nonNullAuthKey: String = authKey
        scope.launch {
            isSyncingStremio = true
            try {
                val addonRes = addonRepository.syncAddonsFromStremioAccount(nonNullAuthKey)
                var libCount = 0
                if (vaultRepository != null) {
                    val libRes = apiClient.getLibraryItems(nonNullAuthKey)
                    if (libRes.isSuccess) {
                        libCount = vaultRepository.syncLibraryFromStremio(libRes.getOrThrow())
                    }
                }
                val addonCount = addonRes.getOrNull() ?: 0
                Toast.makeText(
                    context,
                    "Synced $addonCount addons & $libCount library titles to Love Vault!",
                    Toast.LENGTH_LONG
                ).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Sync failed: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                isSyncingStremio = false
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(theme.background)
            .statusBarsPadding()
            .testTag("addons_screen")
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "ADDONS ECOSYSTEM",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        ),
                        color = Color.White
                    )
                    Text(
                        text = "Modular Stremio Stream & Catalog Plugins",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    // Stremio Sync Action Button
                    Button(
                        onClick = { triggerStremioSync() },
                        enabled = !isSyncingStremio,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = theme.primary.copy(alpha = 0.2f),
                            contentColor = theme.primary
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("sync_stremio_button")
                    ) {
                        if (isSyncingStremio) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(14.dp),
                                color = theme.primary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.CloudSync,
                                contentDescription = "Sync Stremio",
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isSyncingStremio) "Syncing..." else "Sync Cloud",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
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
            }

            // Tab bar (Installed vs Store)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
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
                // INSTALLED TAB
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = theme.surfaceVariant.copy(alpha = 0.4f)),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
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
                                    Icon(
                                        imageVector = Icons.Default.Bolt,
                                        contentDescription = null,
                                        tint = theme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Active Torrent Engine",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "${installedAddons.count { it.isEnabled && it.supportsStream }} scrapers active for movies & series",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    items(installedAddons) { addon ->
                        InstalledAddonCard(
                            addon = addon,
                            onToggle = { enabled ->
                                scope.launch { addonRepository.toggleAddon(addon.id, enabled) }
                            },
                            onDelete = if (!addon.isOfficial) {
                                {
                                    scope.launch {
                                        addonRepository.uninstallAddon(addon.id)
                                        Toast.makeText(context, "${addon.name} uninstalled", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            } else null,
                            onConfigure = if (addon.id == "community.torrentio" || addon.name.contains("Torrentio", ignoreCase = true)) {
                                { showTorrentioSetup = true }
                            } else null
                        )
                    }
                    item { Spacer(modifier = Modifier.height(90.dp)) }
                }
            } else {
                // COMMUNITY STORE TAB
                Column(modifier = Modifier.fillMaxSize()) {
                    // Category filter chips
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        categories.forEach { cat ->
                            val isSelected = selectedCategoryFilter == cat
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(if (isSelected) theme.primary else theme.surface)
                                    .clickable { selectedCategoryFilter = cat }
                                    .padding(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = cat,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) Color.Black else Color.White
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(filteredStore) { listing ->
                            val isAlreadyInstalled = installedAddons.any { it.id == listing.id }
                            CommunityAddonCard(
                                listing = listing,
                                isInstalled = isAlreadyInstalled,
                                onInstall = {
                                    scope.launch {
                                        val res = addonRepository.installAddonFromUrl(listing.manifestUrl)
                                        if (res.isSuccess) {
                                            if (listing.id == "community.torrentio" || listing.manifestUrl.contains("torrentio")) {
                                                context.getSharedPreferences("harbor_prefs", android.content.Context.MODE_PRIVATE)
                                                    .edit()
                                                    .putBoolean("torrentio_linked", true)
                                                    .apply()
                                            }
                                            Toast.makeText(context, "${listing.name} installed successfully!", Toast.LENGTH_SHORT).show()
                                        } else {
                                            Toast.makeText(context, "Install failed: ${res.exceptionOrNull()?.message}", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                },
                                onConfigure = if (listing.id == "community.torrentio") {
                                    { showTorrentioSetup = true }
                                } else null
                            )
                        }
                        item { Spacer(modifier = Modifier.height(90.dp)) }
                    }
                }
            }
        }

        // Install Addon by URL FAB
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
                .padding(20.dp)
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
                            text = "Paste any Stremio manifest URL (e.g. https://.../manifest.json):",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = manifestUrlInput,
                            onValueChange = { manifestUrlInput = it },
                            placeholder = { Text("https://example.com/manifest.json", fontSize = 12.sp) },
                            singleLine = true,
                            enabled = !isInstalling,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("addon_url_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = theme.primary,
                                unfocusedBorderColor = theme.surfaceVariant,
                                focusedContainerColor = theme.surfaceVariant,
                                unfocusedContainerColor = theme.surfaceVariant
                            )
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
                                Text("Fetching and validating manifest...", fontSize = 12.sp, color = theme.primary)
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
                                        Toast.makeText(context, "Addon installed!", Toast.LENGTH_SHORT).show()
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
                        Text("Install", fontWeight = FontWeight.Bold)
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

        // Stremio Login Dialog
        if (showLoginDialog && stremioSession != null) {
            StremioLoginDialog(
                stremioSession = stremioSession,
                apiClient = apiClient,
                addonRepository = addonRepository,
                onDismiss = { showLoginDialog = false },
                onSuccess = {
                    showLoginDialog = false
                    triggerStremioSync()
                }
            )
        }

        // Torrentio Setup Dialog
        if (showTorrentioSetup) {
            TorrentioSetupDialog(
                streamResolverRepository = streamResolverRepository ?: StreamResolverRepository(context, addonRepository, apiClient),
                onDismiss = { showTorrentioSetup = false },
                onSaved = {
                    showTorrentioSetup = false
                    Toast.makeText(context, "Torrentio scraper updated!", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }
}

@Composable
private fun InstalledAddonCard(
    addon: AddonEntity,
    onToggle: (Boolean) -> Unit,
    onDelete: (() -> Unit)?,
    onConfigure: (() -> Unit)? = null
) {
    val theme = LocalHarborTheme.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = theme.surface),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
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
                    .background(if (addon.isEnabled) theme.primary.copy(alpha = 0.15f) else theme.surfaceVariant),
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
            if (onConfigure != null) {
                IconButton(onClick = onConfigure) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = "Configure Addon",
                        tint = theme.primary
                    )
                }
            }

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
    listing: CommunityAddonListing,
    isInstalled: Boolean,
    onInstall: () -> Unit,
    onConfigure: (() -> Unit)? = null
) {
    val theme = LocalHarborTheme.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = theme.surface),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
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
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(theme.primary.copy(alpha = 0.15f))
                            .padding(horizontal = 5.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = listing.category,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = theme.primary
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
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Rating",
                        tint = Color(0xFFFFB703),
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = listing.stars.toString(),
                        fontSize = 10.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    if (listing.isDebridSupported) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Debrid Supported",
                            fontSize = 10.sp,
                            color = Color(0xFF10B981),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                if (onConfigure != null) {
                    IconButton(onClick = onConfigure) {
                        Icon(imageVector = Icons.Default.Tune, contentDescription = "Configure", tint = theme.primary)
                    }
                }

                if (isInstalled) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF10B981).copy(alpha = 0.2f))
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Installed",
                                tint = Color(0xFF10B981),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "Installed",
                                fontSize = 11.sp,
                                color = Color(0xFF10B981),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                } else {
                    Button(
                        onClick = onInstall,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = theme.primary, contentColor = Color.Black),
                        modifier = Modifier.testTag("install_addon_${listing.id}")
                    ) {
                        Icon(imageVector = Icons.Default.Download, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(text = "Install", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
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
            .background(if (isHighlight) theme.primary.copy(alpha = 0.2f) else theme.surfaceVariant)
            .padding(horizontal = 5.dp, vertical = 2.dp)
    ) {
        Text(
            text = label,
            fontSize = 9.sp,
            color = if (isHighlight) theme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun TabPill(title: String, isSelected: Boolean, onClick: () -> Unit) {
    val theme = LocalHarborTheme.current
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (isSelected) theme.primary else theme.surface)
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = title,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) Color.Black else Color.White
        )
    }
}
