package com.example.ui.components

import android.content.Intent
import android.net.Uri
import android.widget.Toast
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.repository.StreamResolverRepository
import com.example.ui.theme.LocalHarborTheme
import kotlinx.coroutines.launch

enum class DebridService(val id: String, val title: String, val tokenUrl: String) {
    NONE("none", "None (Free P2P)", ""),
    REAL_DEBRID("realdebrid", "Real-Debrid", "https://real-debrid.com/apitoken"),
    ALL_DEBRID("alldebrid", "AllDebrid", "https://alldebrid.com/apikeys"),
    PREMIUMIZE("premiumize", "Premiumize", "https://www.premiumize.me/account"),
    TORBOX("torbox", "TorBox", "https://torbox.app/settings")
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TorrentioSetupDialog(
    streamResolverRepository: StreamResolverRepository,
    initialCustomUrl: String = "",
    initialDebridKey: String = "",
    onDismiss: () -> Unit,
    onSaved: () -> Unit
) {
    val theme = LocalHarborTheme.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedDebrid by remember {
        mutableStateOf(
            if (initialDebridKey.isNotBlank()) DebridService.REAL_DEBRID else DebridService.NONE
        )
    }
    var debridKeyInput by remember { mutableStateOf(initialDebridKey) }
    var customUrlInput by remember { mutableStateOf(initialCustomUrl) }
    var useAdvancedCustomUrl by remember { mutableStateOf(initialCustomUrl.isNotBlank()) }

    val allTrackers = listOf(
        "rarbg" to "RARBG",
        "1337x" to "1337x",
        "thepiratebay" to "TPB",
        "torrentgalaxy" to "TGx",
        "yts" to "YTS",
        "eztv" to "EZTV",
        "kickasstorrents" to "KAT",
        "magnetdl" to "MagnetDL"
    )

    var selectedTrackers by remember {
        mutableStateOf(setOf("rarbg", "1337x", "thepiratebay", "torrentgalaxy", "yts", "eztv"))
    }

    var sortOption by remember { mutableStateOf("qualitysize") } // qualitysize, seeders, size
    var maxStreamsPerRes by remember { mutableIntStateOf(4) }
    var excludeCam by remember { mutableStateOf(true) }
    var isSaving by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var validationError by remember { mutableStateOf<String?>(null) }
    var verifiedDebridAccount by remember { mutableStateOf<com.example.data.api.DebridAccountInfo?>(null) }
    var isValidatingKeyOnly by remember { mutableStateOf(false) }

    // Computes generated Torrentio manifest URL dynamically
    val generatedUrl = remember(selectedDebrid, debridKeyInput, selectedTrackers, sortOption, maxStreamsPerRes, excludeCam) {
        val parts = mutableListOf<String>()
        if (selectedTrackers.isNotEmpty()) {
            parts.add("providers=" + selectedTrackers.joinToString(","))
        }
        parts.add("sort=$sortOption")
        if (maxStreamsPerRes in 1..10) {
            parts.add("limit=$maxStreamsPerRes")
        }
        if (excludeCam) {
            parts.add("qualityfilter=cam,scr")
        }
        if (selectedDebrid != DebridService.NONE && debridKeyInput.isNotBlank()) {
            parts.add("${selectedDebrid.id}=${debridKeyInput.trim()}")
        }
        val optionsStr = if (parts.isNotEmpty()) parts.joinToString("|") + "/" else ""
        "https://torrentio.strem.fun/${optionsStr}manifest.json"
    }

    AlertDialog(
        onDismissRequest = { if (!isSaving) onDismiss() },
        containerColor = theme.surface,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("torrentio_setup_dialog"),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
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
                    Column {
                        Text(
                            text = "Torrentio Setup",
                            color = Color.White,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Aggregator & Debrid Configuration",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp
                        )
                    }
                }

                IconButton(
                    onClick = onDismiss,
                    enabled = !isSaving
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // Section 1: Debrid Provider Selection
                Text(
                    text = "DEBRID PROVIDER (FAST CACHED STREAMS)",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    color = theme.primary,
                    letterSpacing = 0.5.sp
                )
                Text(
                    text = "Debrid services instantly download and stream torrents through high-speed CDN without uploading.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 4.dp)
                )

                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    DebridService.entries.forEach { srv ->
                        val isSelected = selectedDebrid == srv
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) theme.primary else theme.surfaceVariant)
                                .clickable { selectedDebrid = srv }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = srv.title,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color.Black else Color.White
                            )
                        }
                    }
                }

                // Debrid API Key Field
                if (selectedDebrid != DebridService.NONE) {
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = debridKeyInput,
                        onValueChange = { debridKeyInput = it },
                        label = { Text("${selectedDebrid.title} API Key") },
                        placeholder = { Text("Paste token from account") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = theme.surfaceVariant,
                            unfocusedContainerColor = theme.surfaceVariant
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("dialog_debrid_key_input")
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (selectedDebrid.tokenUrl.isNotBlank()) {
                            Row(
                                modifier = Modifier
                                    .clickable {
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(selectedDebrid.tokenUrl))
                                        context.startActivity(intent)
                                    }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.OpenInNew,
                                    contentDescription = null,
                                    tint = theme.primary,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Get token",
                                    color = theme.primary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        androidx.compose.material3.TextButton(
                            onClick = {
                                if (debridKeyInput.isNotBlank()) {
                                    scope.launch {
                                        isValidatingKeyOnly = true
                                        validationError = null
                                        val res = streamResolverRepository.validateDebridKey(selectedDebrid.id, debridKeyInput.trim())
                                        isValidatingKeyOnly = false
                                        if (res.isSuccess) {
                                            verifiedDebridAccount = res.getOrThrow()
                                            Toast.makeText(context, "Verified ${selectedDebrid.title} account!", Toast.LENGTH_SHORT).show()
                                        } else {
                                            verifiedDebridAccount = null
                                            validationError = res.exceptionOrNull()?.message ?: "Invalid key"
                                        }
                                    }
                                }
                            },
                            enabled = !isValidatingKeyOnly && debridKeyInput.isNotBlank()
                        ) {
                            if (isValidatingKeyOnly) {
                                CircularProgressIndicator(modifier = Modifier.size(12.dp), color = theme.primary, strokeWidth = 1.5.dp)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Checking...", fontSize = 11.sp)
                            } else {
                                Text("Test Key", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = theme.primary)
                            }
                        }
                    }

                    if (verifiedDebridAccount != null) {
                        val acc = verifiedDebridAccount!!
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFF10B981).copy(alpha = 0.15f))
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Verified: ${acc.username} (${if (acc.isPremium) "Premium" else "Standard"})",
                                    fontSize = 11.sp,
                                    color = Color(0xFF10B981),
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Section 2: Torrent Providers & Trackers
                Text(
                    text = "INDEXED TORRENT TRACKERS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    color = theme.primary,
                    letterSpacing = 0.5.sp
                )
                Text(
                    text = "Select trackers to aggregate streams from:",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 2.dp)
                )

                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    allTrackers.forEach { (key, label) ->
                        val isSelected = selectedTrackers.contains(key)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) theme.primary.copy(alpha = 0.25f) else theme.surfaceVariant)
                                .clickable {
                                    selectedTrackers = if (isSelected) {
                                        if (selectedTrackers.size > 1) selectedTrackers - key else selectedTrackers
                                    } else {
                                        selectedTrackers + key
                                    }
                                }
                                .padding(horizontal = 8.dp, vertical = 5.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = theme.primary,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                }
                                Text(
                                    text = label,
                                    fontSize = 11.sp,
                                    color = if (isSelected) theme.primary else Color.White,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Section 3: Sorting & Filters
                Text(
                    text = "STREAM RANKING & FILTERS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    color = theme.primary,
                    letterSpacing = 0.5.sp
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        "qualitysize" to "Quality & Size",
                        "seeders" to "By Seeds",
                        "size" to "By Size"
                    ).forEach { (opt, label) ->
                        val isSelected = sortOption == opt
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) theme.primary else theme.surfaceVariant)
                                .clickable { sortOption = opt }
                                .padding(horizontal = 8.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = label,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) Color.Black else Color.White
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Exclude CAM / TS Releases", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        Text("Filter out low-quality cinema recordings", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                    }
                    Switch(
                        checked = excludeCam,
                        onCheckedChange = { excludeCam = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = theme.primary)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Toggle for Advanced custom manifest URL
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { useAdvancedCustomUrl = !useAdvancedCustomUrl }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (useAdvancedCustomUrl) "Hide Direct Manifest URL Field" else "Or Paste Custom Manifest URL directly",
                        fontSize = 11.sp,
                        color = theme.primary
                    )
                }

                if (useAdvancedCustomUrl) {
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = customUrlInput,
                        onValueChange = { customUrlInput = it },
                        label = { Text("Direct Manifest URL") },
                        placeholder = { Text(generatedUrl) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = theme.surfaceVariant,
                            unfocusedContainerColor = theme.surfaceVariant
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    // Preview box of generated URL
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.Black.copy(alpha = 0.4f))
                            .padding(8.dp)
                    ) {
                        Column {
                            Text(
                                text = "LIVE GENERATED MANIFEST URL:",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = generatedUrl,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                color = theme.primary
                            )
                        }
                    }
                }

                if (statusMessage != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(theme.surfaceVariant)
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = theme.primary, strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = statusMessage ?: "", fontSize = 11.sp, color = Color.White)
                    }
                }

                if (validationError != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFEF4444).copy(alpha = 0.15f))
                            .padding(10.dp)
                    ) {
                        Text(
                            text = validationError ?: "",
                            fontSize = 11.sp,
                            color = Color(0xFFEF4444),
                            lineHeight = 15.sp
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val targetUrl = if (useAdvancedCustomUrl && customUrlInput.isNotBlank()) {
                        customUrlInput.trim()
                    } else {
                        generatedUrl
                    }

                    scope.launch {
                        isSaving = true
                        validationError = null

                        // 1. Validate Debrid Key if selected
                        val debridKey = if (selectedDebrid != DebridService.NONE && debridKeyInput.isNotBlank()) debridKeyInput.trim() else null
                        var verifiedInfo: com.example.data.api.DebridAccountInfo? = verifiedDebridAccount

                        if (selectedDebrid != DebridService.NONE && !debridKey.isNullOrBlank() && verifiedInfo == null) {
                            statusMessage = "Validating ${selectedDebrid.title} API key with servers..."
                            val debridRes = streamResolverRepository.validateDebridKey(selectedDebrid.id, debridKey)
                            if (debridRes.isFailure) {
                                isSaving = false
                                statusMessage = null
                                validationError = debridRes.exceptionOrNull()?.message ?: "Invalid ${selectedDebrid.title} API key"
                                return@launch
                            }
                            verifiedInfo = debridRes.getOrNull()
                        }

                        // 2. Validate Torrentio manifest URL reachable
                        statusMessage = "Verifying Torrentio manifest response..."
                        val manifestRes = streamResolverRepository.validateTorrentioUrl(targetUrl)
                        if (manifestRes.isFailure) {
                            isSaving = false
                            statusMessage = null
                            validationError = manifestRes.exceptionOrNull()?.message ?: "Unable to connect to Torrentio at manifest URL"
                            return@launch
                        }

                        // 3. Both passed! Save configuration & link
                        statusMessage = "Connecting Torrentio..."
                        streamResolverRepository.linkTorrentio(
                            customUrl = targetUrl,
                            debridKey = debridKey,
                            debridService = selectedDebrid.id,
                            verifiedInfo = verifiedInfo
                        )
                        isSaving = false
                        statusMessage = null
                        Toast.makeText(context, "Verified & linked Torrentio successfully! Real streams ready.", Toast.LENGTH_SHORT).show()
                        onSaved()
                    }
                },
                enabled = !isSaving,
                colors = ButtonDefaults.buttonColors(
                    containerColor = theme.primary,
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.testTag("dialog_save_torrentio_button")
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = Color.Black,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Verifying...", fontWeight = FontWeight.Bold)
                } else {
                    Text("Verify & Connect Torrentio", fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                enabled = !isSaving,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Cancel")
            }
        }
    )
}
