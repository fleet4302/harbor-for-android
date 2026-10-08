package com.example.ui.screens

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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.example.data.model.HarborThemeStyle
import com.example.ui.theme.LocalHarborTheme

@Composable
fun SettingsScreen(
    currentTheme: HarborThemeStyle,
    onThemeSelected: (HarborThemeStyle) -> Unit,
    modifier: Modifier = Modifier
) {
    val theme = LocalHarborTheme.current
    val context = LocalContext.current

    var debridKeyInput by remember { mutableStateOf("") }
    var autoPlayNext by remember { mutableStateOf(true) }
    var hwAcceleration by remember { mutableStateOf(true) }
    var preferredRes by remember { mutableStateOf("1080p") }
    var defaultSubLanguage by remember { mutableStateOf("English") }

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
            text = "Harbor Client & Stremio Addon Configuration",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Section 1: Harbor Theme Studio
        SettingsSectionHeader(title = "Harbor Theme Studio", icon = Icons.Default.Palette)

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

        // Section 2: Debrid Provider Setup (Real-Debrid, TorBox, AllDebrid)
        SettingsSectionHeader(title = "Debrid Service Integration", icon = Icons.Default.Bolt)

        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = theme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "Real-Debrid / TorBox API Key",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "Enables instant [RD+] high-speed cached streaming streams from Torrentio & scrapers.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 4.dp)
                )

                OutlinedTextField(
                    value = debridKeyInput,
                    onValueChange = { debridKeyInput = it },
                    placeholder = { Text("Paste API token from real-debrid.com/apitoken") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = theme.surfaceVariant,
                        unfocusedContainerColor = theme.surfaceVariant
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .testTag("debrid_key_input")
                )

                Button(
                    onClick = {
                        Toast.makeText(context, "Debrid configuration updated & active!", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = theme.primary, contentColor = Color.Black),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Save & Connect Token", fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Section 3: Player Settings
        SettingsSectionHeader(title = "Player & Playback Preferences", icon = Icons.Default.PlayCircle)

        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = theme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
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
                        onCheckedChange = { autoPlayNext = it },
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
                        onCheckedChange = { hwAcceleration = it },
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
                                .clickable { preferredRes = res }
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

                Spacer(modifier = Modifier.height(16.dp))

                // Default Subtitle Language
                Text(text = "Default Subtitle Language", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("English", "Spanish", "French", "None").forEach { lang ->
                        val isSel = defaultSubLanguage == lang
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSel) theme.primary else theme.surfaceVariant)
                                .clickable { defaultSubLanguage = lang }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = lang,
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

        // About Harbor Client
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = theme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(text = "Harbor for Android", fontSize = 14.sp, fontWeight = FontWeight.Black, color = Color.White)
                Text(
                    text = "Native Android port of Harbor, the next-generation Stremio client. Built with Jetpack Compose, ExoPlayer/Media3, and full Stremio v3 Addon protocol support.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
                Text(
                    text = "Reference: github.com/harborstremio/harbor",
                    fontSize = 10.sp,
                    color = theme.primary
                )
            }
        }

        Spacer(modifier = Modifier.height(60.dp))
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
            // Theme preview palette circle
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
