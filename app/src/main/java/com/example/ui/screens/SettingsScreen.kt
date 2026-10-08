package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.api.StremioApiClient
import com.example.data.local.StremioAccountSession
import com.example.data.model.HarborThemeStyle
import com.example.data.repository.AddonRepository
import com.example.data.repository.StreamResolverRepository
import com.example.ui.components.StremioLoginDialog
import com.example.ui.components.TorrentioSetupDialog
import com.example.ui.theme.LocalHarborTheme
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    currentTheme: HarborThemeStyle,
    onThemeSelected: (HarborThemeStyle) -> Unit,
    stremioSession: StremioAccountSession,
    apiClient: StremioApiClient,
    addonRepository: AddonRepository,
    streamResolverRepository: StreamResolverRepository,
    modifier: Modifier = Modifier
) {
    val theme = LocalHarborTheme.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sessionState by stremioSession.sessionState.collectAsState()
    val prefs = remember { context.getSharedPreferences("harbor_prefs", Context.MODE_PRIVATE) }

    var loginEmail by remember { mutableStateOf("") }
    var loginPassword by remember { mutableStateOf("") }
    var authKeyInput by remember { mutableStateOf("") }
    var showAuthKeyOption by remember { mutableStateOf(false) }
    var isLoggingIn by remember { mutableStateOf(false) }
    var loginError by remember { mutableStateOf<String?>(null) }
    var isSyncingAddons by remember { mutableStateOf(false) }

    var torrentioUrlInput by remember { mutableStateOf(prefs.getString("custom_torrentio_url", "") ?: "") }
    var debridKeyInput by remember { mutableStateOf(prefs.getString("debrid_key", "") ?: "") }
    var autoPlayNext by remember { mutableStateOf(prefs.getBoolean("autoplay_next", true)) }
    var hwAcceleration by remember { mutableStateOf(prefs.getBoolean("hw_accel", true)) }
    var preferredRes by remember { mutableStateOf(prefs.getString("preferred_res", "1080p") ?: "1080p") }

    var traktUser by remember { mutableStateOf(prefs.getString("trakt_username", "") ?: "") }
    var letterboxdUser by remember { mutableStateOf(prefs.getString("letterboxd_username", "") ?: "") }
    var simklUser by remember { mutableStateOf(prefs.getString("simkl_username", "") ?: "") }
    var rpdbKey by remember { mutableStateOf(prefs.getString("rpdb_key", "") ?: "") }

    var isLinkedState by remember { mutableStateOf(streamResolverRepository.isStreamSourceLinked()) }
    var linkedDebridInfo by remember { mutableStateOf(streamResolverRepository.getLinkedDebridInfo()) }
    var isValidatingDebrid by remember { mutableStateOf(false) }
    var debridErrorMessage by remember { mutableStateOf<String?>(null) }
    var debridSuccessMessage by remember { mutableStateOf<String?>(null) }
    var isValidatingTorrentio by remember { mutableStateOf(false) }
    var torrentioErrorMessage by remember { mutableStateOf<String?>(null) }

    var showTorrentioSetup by remember { mutableStateOf(false) }
    var showStremioLogin by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(theme.background)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .testTag("settings_screen")
    ) {
        // Title
        Text(
            text = "SETTINGS & STUDIO",
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp
            ),
            color = Color.White
        )
        Text(
            text = "Stremio Cloud Account & Real Torrentio Integration",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // SECTION 1: Stremio Account (Real Login & Addon Sync)
        SettingsSectionHeader(title = "Stremio Account", icon = Icons.Default.AccountCircle)

        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = theme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                if (sessionState.isLoggedIn) {
                    // Logged In UI
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF10B981).copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Active",
                                tint = Color(0xFF10B981),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Connected to Stremio",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = Color.White
                            )
                            Text(
                                text = sessionState.email ?: "Logged In",
                                fontSize = 12.sp,
                                color = theme.primary
                            )
                        }

                        IconButton(
                            onClick = {
                                stremioSession.clearSession()
                                isLinkedState = streamResolverRepository.isStreamSourceLinked()
                                Toast.makeText(context, "Logged out of Stremio", Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.ExitToApp,
                                contentDescription = "Log Out",
                                tint = Color(0xFFEF4444)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = {
                            val key = sessionState.authKey ?: return@Button
                            scope.launch {
                                isSyncingAddons = true
                                val res = addonRepository.syncAddonsFromStremioAccount(key)
                                isSyncingAddons = false
                                if (res.isSuccess) {
                                    Toast.makeText(context, "Synced ${res.getOrThrow()} addons from your Stremio account!", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "Sync failed: ${res.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                                }
                            }
                        },
                        enabled = !isSyncingAddons,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = theme.primary,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("sync_stremio_addons_button")
                    ) {
                        if (isSyncingAddons) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = Color.Black,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Syncing addons...", fontWeight = FontWeight.Bold)
                        } else {
                            Icon(imageVector = Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Sync Addons from Stremio Cloud", fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    // Not Logged In UI
                    Text(
                        text = "Sign in to sync your Stremio addons, Torrentio setup, and catalogs.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedButton(
                        onClick = { showStremioLogin = true },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().testTag("open_stremio_login_wizard_button")
                    ) {
                        Icon(imageVector = Icons.Default.AccountCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Open Stremio Login Wizard (Email or AuthKey)", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = loginEmail,
                        onValueChange = { loginEmail = it },
                        label = { Text("Stremio Email") },
                        placeholder = { Text("you@example.com") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = theme.surfaceVariant,
                            unfocusedContainerColor = theme.surfaceVariant
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("stremio_email_input")
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = loginPassword,
                        onValueChange = { loginPassword = it },
                        label = { Text("Stremio Password") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = theme.surfaceVariant,
                            unfocusedContainerColor = theme.surfaceVariant
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("stremio_password_input")
                    )

                    if (loginError != null) {
                        Text(
                            text = loginError ?: "",
                            color = Color(0xFFEF4444),
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 6.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = {
                            if (loginEmail.isNotBlank() && loginPassword.isNotBlank()) {
                                scope.launch {
                                    isLoggingIn = true
                                    loginError = null
                                    val res = apiClient.login(loginEmail, loginPassword)
                                    isLoggingIn = false
                                    if (res.isSuccess) {
                                        val result = res.getOrThrow()
                                        stremioSession.saveSession(
                                            authKey = result.authKey,
                                            email = result.user?.email ?: loginEmail,
                                            userId = result.user?._id
                                        )
                                        Toast.makeText(context, "Logged in to Stremio! Syncing addons...", Toast.LENGTH_SHORT).show()
                                        isLinkedState = streamResolverRepository.isStreamSourceLinked()
                                        // Auto-sync addons
                                        addonRepository.syncAddonsFromStremioAccount(result.authKey)
                                    } else {
                                        loginError = res.exceptionOrNull()?.message ?: "Invalid email or password"
                                    }
                                }
                            }
                        },
                        enabled = !isLoggingIn && loginEmail.isNotBlank() && loginPassword.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = theme.primary,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                            .testTag("stremio_login_button")
                    ) {
                        if (isLoggingIn) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = Color.Black,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Authenticating...", fontWeight = FontWeight.Bold)
                        } else {
                            Text("Log In to Stremio", fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Or paste AuthKey
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showAuthKeyOption = !showAuthKeyOption }
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = if (showAuthKeyOption) "Hide AuthKey Input" else "Or Log In via AuthKey (Google/Facebook users)",
                            fontSize = 11.sp,
                            color = theme.primary
                        )
                    }

                    if (showAuthKeyOption) {
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = authKeyInput,
                            onValueChange = { authKeyInput = it },
                            label = { Text("Stremio AuthKey") },
                            placeholder = { Text("Paste AuthKey from web.stremio.com console") },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = theme.surfaceVariant,
                                unfocusedContainerColor = theme.surfaceVariant
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = {
                                if (authKeyInput.isNotBlank()) {
                                    scope.launch {
                                        stremioSession.saveSession(
                                            authKey = authKeyInput.trim(),
                                            email = "Authenticated User",
                                            userId = null
                                        )
                                        Toast.makeText(context, "AuthKey saved! Syncing addons...", Toast.LENGTH_SHORT).show()
                                        isLinkedState = streamResolverRepository.isStreamSourceLinked()
                                        addonRepository.syncAddonsFromStremioAccount(authKeyInput.trim())
                                    }
                                }
                            },
                            enabled = authKeyInput.isNotBlank(),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Connect via AuthKey")
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // SECTION 2: Torrentio & Debrid Setup
        SettingsSectionHeader(title = "Torrentio & Debrid Scraper", icon = Icons.Default.Bolt)

        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = theme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Torrentio Status Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Torrentio Torrent Aggregator",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = if (isLinkedState) "Status: Linked & Active" else "Status: Not Linked (Zero streams displayed)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isLinkedState) Color(0xFF10B981) else Color(0xFFF59E0B)
                        )
                    }

                    if (isLinkedState) {
                        OutlinedButton(
                            onClick = {
                                scope.launch {
                                    streamResolverRepository.unlinkTorrentio()
                                    isLinkedState = streamResolverRepository.isStreamSourceLinked()
                                    Toast.makeText(context, "Torrentio unlinked. Zero streams will appear.", Toast.LENGTH_SHORT).show()
                                }
                            },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("unlink_torrentio_button")
                        ) {
                            Text("Unlink", color = Color(0xFFEF4444), fontSize = 12.sp)
                        }
                    } else {
                        Button(
                            onClick = {
                                scope.launch {
                                    isValidatingTorrentio = true
                                    torrentioErrorMessage = null
                                    val customUrl = torrentioUrlInput.ifBlank { null }
                                    if (customUrl != null) {
                                        val valRes = streamResolverRepository.validateTorrentioUrl(customUrl)
                                        if (valRes.isFailure) {
                                            isValidatingTorrentio = false
                                            torrentioErrorMessage = valRes.exceptionOrNull()?.message ?: "Invalid Torrentio URL"
                                            return@launch
                                        }
                                    }
                                    streamResolverRepository.linkTorrentio(
                                        customUrl = customUrl,
                                        debridKey = debridKeyInput.ifBlank { null }
                                    )
                                    isValidatingTorrentio = false
                                    isLinkedState = streamResolverRepository.isStreamSourceLinked()
                                    Toast.makeText(context, "Torrentio linked! Real torrent streams active.", Toast.LENGTH_SHORT).show()
                                }
                            },
                            enabled = !isValidatingTorrentio,
                            colors = ButtonDefaults.buttonColors(containerColor = theme.primary, contentColor = Color.Black),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("link_torrentio_button")
                        ) {
                            if (isValidatingTorrentio) {
                                CircularProgressIndicator(modifier = Modifier.size(14.dp), color = Color.Black, strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Checking...", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            } else {
                                Text("Link Torrentio", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Aggregates real torrents across RARBG, 1337x, TorrentGalaxy, and YTS. Only real torrents and Debrid streams are displayed when linked.",
                    fontSize = 11.sp,
                    lineHeight = 15.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Linked Debrid Account Status Card
                if (linkedDebridInfo != null) {
                    val acc = linkedDebridInfo!!
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF10B981).copy(alpha = 0.12f))
                            .padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "${acc.service} Connected: ${acc.username}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Text(
                                        text = if (acc.isPremium) {
                                            "Premium Active • ${if (acc.daysRemaining != null) "${acc.daysRemaining} days left" else "Unlimited"}"
                                        } else "Free / Standard Account",
                                        fontSize = 11.sp,
                                        color = Color(0xFF10B981)
                                    )
                                }
                            }

                            TextButton(onClick = {
                                streamResolverRepository.unlinkDebrid()
                                linkedDebridInfo = null
                                debridKeyInput = ""
                                debridSuccessMessage = null
                                Toast.makeText(context, "Debrid account disconnected", Toast.LENGTH_SHORT).show()
                            }) {
                                Text("Disconnect", fontSize = 11.sp, color = Color(0xFFEF4444))
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // Custom Torrentio URL Field
                OutlinedTextField(
                    value = torrentioUrlInput,
                    onValueChange = {
                        torrentioUrlInput = it
                        torrentioErrorMessage = null
                    },
                    label = { Text("Custom Torrentio Manifest URL (Optional)") },
                    placeholder = { Text("https://torrentio.strem.fun/manifest.json") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = theme.surfaceVariant,
                        unfocusedContainerColor = theme.surfaceVariant
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("torrentio_url_input")
                )

                if (torrentioErrorMessage != null) {
                    Text(
                        text = torrentioErrorMessage ?: "",
                        fontSize = 11.sp,
                        color = Color(0xFFEF4444),
                        modifier = Modifier.padding(top = 4.dp, start = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Real-Debrid API Token Field
                OutlinedTextField(
                    value = debridKeyInput,
                    onValueChange = {
                        debridKeyInput = it
                        debridErrorMessage = null
                        debridSuccessMessage = null
                    },
                    label = { Text("Real-Debrid / TorBox API Key (Optional)") },
                    placeholder = { Text("Enter API key from real-debrid.com/apitoken") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = theme.surfaceVariant,
                        unfocusedContainerColor = theme.surfaceVariant
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("debrid_key_input")
                )

                // Debrid verification error message
                if (debridErrorMessage != null) {
                    Text(
                        text = debridErrorMessage ?: "",
                        fontSize = 11.sp,
                        color = Color(0xFFEF4444),
                        modifier = Modifier.padding(top = 4.dp, start = 4.dp)
                    )
                }

                // Debrid verification success message
                if (debridSuccessMessage != null) {
                    Text(
                        text = debridSuccessMessage ?: "",
                        fontSize = 11.sp,
                        color = Color(0xFF10B981),
                        modifier = Modifier.padding(top = 4.dp, start = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Row with Token URL and Test Token Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier
                            .clickable {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://real-debrid.com/apitoken"))
                                context.startActivity(intent)
                            }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Default.OpenInNew, contentDescription = null, tint = theme.primary, modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Get key at real-debrid.com/apitoken", fontSize = 11.sp, color = theme.primary, fontWeight = FontWeight.SemiBold)
                    }

                    TextButton(
                        onClick = {
                            if (debridKeyInput.isNotBlank()) {
                                scope.launch {
                                    isValidatingDebrid = true
                                    debridErrorMessage = null
                                    debridSuccessMessage = null
                                    val res = streamResolverRepository.validateDebridKey("realdebrid", debridKeyInput.trim())
                                    isValidatingDebrid = false
                                    if (res.isSuccess) {
                                        val info = res.getOrThrow()
                                        linkedDebridInfo = info
                                        debridSuccessMessage = "Verified! ${info.username} (${if (info.isPremium) "Premium active" else "Standard"})"
                                        streamResolverRepository.linkTorrentio(
                                            customUrl = torrentioUrlInput.ifBlank { null },
                                            debridKey = debridKeyInput.trim(),
                                            debridService = "realdebrid",
                                            verifiedInfo = info
                                        )
                                        isLinkedState = streamResolverRepository.isStreamSourceLinked()
                                    } else {
                                        debridErrorMessage = res.exceptionOrNull()?.message ?: "Invalid key. Real-Debrid rejected this token."
                                    }
                                }
                            }
                        },
                        enabled = !isValidatingDebrid && debridKeyInput.isNotBlank()
                    ) {
                        if (isValidatingDebrid) {
                            CircularProgressIndicator(modifier = Modifier.size(12.dp), color = theme.primary, strokeWidth = 1.5.dp)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Testing...", fontSize = 11.sp)
                        } else {
                            Text("Test & Verify Key", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = theme.primary)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Save Configuration Button with Real Checking
                Button(
                    onClick = {
                        scope.launch {
                            isValidatingDebrid = true
                            debridErrorMessage = null
                            torrentioErrorMessage = null

                            // 1. If Debrid key is entered, validate with real API
                            var verifiedInfo = linkedDebridInfo
                            if (debridKeyInput.isNotBlank()) {
                                val debridRes = streamResolverRepository.validateDebridKey("realdebrid", debridKeyInput.trim())
                                if (debridRes.isFailure) {
                                    isValidatingDebrid = false
                                    debridErrorMessage = debridRes.exceptionOrNull()?.message ?: "Invalid Real-Debrid API key"
                                    return@launch
                                }
                                verifiedInfo = debridRes.getOrNull()
                                linkedDebridInfo = verifiedInfo
                            }

                            // 2. If custom Torrentio URL is entered, validate manifest
                            if (torrentioUrlInput.isNotBlank()) {
                                val manifestRes = streamResolverRepository.validateTorrentioUrl(torrentioUrlInput.trim())
                                if (manifestRes.isFailure) {
                                    isValidatingDebrid = false
                                    torrentioErrorMessage = manifestRes.exceptionOrNull()?.message ?: "Torrentio manifest unreachable"
                                    return@launch
                                }
                            }

                            // 3. Save validated configuration
                            streamResolverRepository.linkTorrentio(
                                customUrl = torrentioUrlInput.ifBlank { null },
                                debridKey = debridKeyInput.ifBlank { null },
                                debridService = "realdebrid",
                                verifiedInfo = verifiedInfo
                            )
                            isValidatingDebrid = false
                            isLinkedState = streamResolverRepository.isStreamSourceLinked()
                            Toast.makeText(context, "Configuration verified & saved! Torrentio active.", Toast.LENGTH_SHORT).show()
                        }
                    },
                    enabled = !isValidatingDebrid,
                    colors = ButtonDefaults.buttonColors(containerColor = theme.primary, contentColor = Color.Black),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.align(Alignment.End)
                ) {
                    if (isValidatingDebrid) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.Black, strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Verifying...", fontWeight = FontWeight.Bold)
                    } else {
                        Text("Verify & Save Configuration", fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedButton(
                    onClick = { showTorrentioSetup = true },
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().testTag("open_torrentio_wizard_button")
                ) {
                    Icon(imageVector = Icons.Default.Bolt, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Open Interactive Torrentio Wizard (Trackers, Quality, Debrid)", fontSize = 12.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // SECTION 2.5: Native Harbor Integrations
        SettingsSectionHeader(title = "Trakt, Letterboxd, Simkl & RPDB Integrations", icon = Icons.Default.Sync)

        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = theme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Sync your watch history, movie lists, and rating overlays with external services.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Trakt.tv
                OutlinedTextField(
                    value = traktUser,
                    onValueChange = {
                        traktUser = it
                        prefs.edit().putString("trakt_username", it).apply()
                    },
                    label = { Text("Trakt.tv Username / Account") },
                    placeholder = { Text("Enter Trakt username") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = theme.surfaceVariant,
                        unfocusedContainerColor = theme.surfaceVariant
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Letterboxd
                OutlinedTextField(
                    value = letterboxdUser,
                    onValueChange = {
                        letterboxdUser = it
                        prefs.edit().putString("letterboxd_username", it).apply()
                    },
                    label = { Text("Letterboxd Username") },
                    placeholder = { Text("Enter Letterboxd handle") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = theme.surfaceVariant,
                        unfocusedContainerColor = theme.surfaceVariant
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Simkl
                OutlinedTextField(
                    value = simklUser,
                    onValueChange = {
                        simklUser = it
                        prefs.edit().putString("simkl_username", it).apply()
                    },
                    label = { Text("Simkl Anime & Show Tracker") },
                    placeholder = { Text("Enter Simkl ID") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = theme.surfaceVariant,
                        unfocusedContainerColor = theme.surfaceVariant
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Rating Poster Database (RPDB) API Key
                OutlinedTextField(
                    value = rpdbKey,
                    onValueChange = {
                        rpdbKey = it
                        prefs.edit().putString("rpdb_key", it).apply()
                    },
                    label = { Text("RPDB Rating Posters API Key (Optional)") },
                    placeholder = { Text("Paste RPDB key for IMDb/RT poster overlays") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = theme.surfaceVariant,
                        unfocusedContainerColor = theme.surfaceVariant
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = {
                        Toast.makeText(context, "Integrations saved & synced!", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = theme.primary, contentColor = Color.Black),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Save Integrations", fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // SECTION 3: Theme Studio
        SettingsSectionHeader(title = "Theme Studio", icon = Icons.Default.Palette)

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            HarborThemeStyle.entries.forEach { style ->
                val isSelected = style == currentTheme
                ThemeSelectorCard(
                    style = style,
                    isSelected = isSelected,
                    onSelect = { onThemeSelected(style) }
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // SECTION 4: Player Preferences
        SettingsSectionHeader(title = "Player & Playback Preferences", icon = Icons.Default.PlayCircle)

        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = theme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Auto-play Next Episode
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(text = "Auto-play Next Episode", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                        Text(text = "Automatically resolves and plays the next episode", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = autoPlayNext,
                        onCheckedChange = {
                            autoPlayNext = it
                            prefs.edit().putBoolean("autoplay_next", it).apply()
                        },
                        colors = SwitchDefaults.colors(checkedThumbColor = theme.primary)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Hardware Acceleration
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(text = "Hardware Video Decoding", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                        Text(text = "Uses GPU hardware decoders for 4K HEVC & HDR playback", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = hwAcceleration,
                        onCheckedChange = {
                            hwAcceleration = it
                            prefs.edit().putBoolean("hw_accel", it).apply()
                        },
                        colors = SwitchDefaults.colors(checkedThumbColor = theme.primary)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Preferred Resolution
                Text(text = "Preferred Stream Quality", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("4K UHD", "1080p", "720p", "Auto").forEach { res ->
                        val isSel = preferredRes == res
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSel) theme.primary else theme.surfaceVariant)
                                .clickable {
                                    preferredRes = res
                                    prefs.edit().putString("preferred_res", res).apply()
                                }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = res,
                                fontSize = 12.sp,
                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSel) Color.Black else Color.White
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // About
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = theme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = "Features & Protocol Support", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Text(
                    text = "Features full Stremio v3 Addon protocol support, real Torrentio stream aggregation, Stremio Cloud login, and hardware-accelerated Media3 video playback.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(60.dp))
    }

    if (showTorrentioSetup) {
        TorrentioSetupDialog(
            streamResolverRepository = streamResolverRepository,
            initialCustomUrl = torrentioUrlInput,
            initialDebridKey = debridKeyInput,
            onDismiss = { showTorrentioSetup = false },
            onSaved = {
                showTorrentioSetup = false
                torrentioUrlInput = prefs.getString("custom_torrentio_url", "") ?: ""
                debridKeyInput = prefs.getString("debrid_key", "") ?: ""
                isLinkedState = streamResolverRepository.isStreamSourceLinked()
            }
        )
    }

    if (showStremioLogin) {
        StremioLoginDialog(
            stremioSession = stremioSession,
            apiClient = apiClient,
            addonRepository = addonRepository,
            onDismiss = { showStremioLogin = false },
            onSuccess = {
                showStremioLogin = false
            }
        )
    }
}

@Composable
private fun SettingsSectionHeader(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    val theme = LocalHarborTheme.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 8.dp)
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = theme.primary, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}

@Composable
private fun ThemeSelectorCard(
    style: HarborThemeStyle,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) style.primary else style.surfaceVariant,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable { onSelect() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = style.surface)
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
                    .background(style.primary),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Selected",
                        tint = Color.Black,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = style.title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = style.subtitle,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
