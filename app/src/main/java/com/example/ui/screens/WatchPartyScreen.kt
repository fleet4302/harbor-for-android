package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import com.example.data.repository.WatchPartyRepository
import com.example.ui.theme.LocalHarborTheme

@Composable
fun WatchPartyScreen(
    watchPartyRepository: WatchPartyRepository,
    onLaunchSyncedMedia: (title: String, url: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val theme = LocalHarborTheme.current
    val context = LocalContext.current
    val room by watchPartyRepository.currentRoom.collectAsState()

    var userNameInput by remember { mutableStateOf("Sailor_1") }
    var joinCodeInput by remember { mutableStateOf("") }
    var chatInput by remember { mutableStateOf("") }

    val reactions = listOf("🍿", "🔥", "❤️", "😂", "😮", "🚀")

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(theme.background)
            .statusBarsPadding()
            .testTag("watch_party_screen")
    ) {
        if (room == null) {
            // Join or Host initial screen
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(theme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Group,
                        contentDescription = "Watch Party",
                        tint = theme.primary,
                        modifier = Modifier.size(36.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "HARBOR WATCH PARTY",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp,
                    color = Color.White
                )

                Text(
                    text = "Watch together with friends with synchronized playback & live chat.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)
                )

                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = theme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Your Display Name",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = userNameInput,
                            onValueChange = { userNameInput = it },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("party_username_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = theme.surfaceVariant,
                                unfocusedContainerColor = theme.surfaceVariant
                            )
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Host party button
                        Button(
                            onClick = {
                                watchPartyRepository.createRoom(
                                    hostName = userNameInput.ifBlank { "Host" },
                                    mediaTitle = "Dune: Part Two",
                                    mediaType = "movie",
                                    streamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4"
                                )
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = theme.primary,
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("host_party_button")
                        ) {
                            Text("Host New Party", fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(1.dp)
                                    .background(theme.surfaceVariant)
                            )
                            Text(
                                text = "  OR JOIN  ",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(1.dp)
                                    .background(theme.surfaceVariant)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        OutlinedTextField(
                            value = joinCodeInput,
                            onValueChange = { joinCodeInput = it.uppercase() },
                            placeholder = { Text("Enter Room Code (e.g. HBR-8492)") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("join_code_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = theme.surfaceVariant,
                                unfocusedContainerColor = theme.surfaceVariant
                            )
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedButton(
                            onClick = {
                                if (joinCodeInput.isNotBlank()) {
                                    watchPartyRepository.joinRoom(
                                        roomCode = joinCodeInput,
                                        userName = userNameInput.ifBlank { "Guest" }
                                    )
                                }
                            },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("join_party_button")
                        ) {
                            Text("Join Room Code", color = theme.primary, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        } else {
            val activeRoom = room!!
            Column(modifier = Modifier.fillMaxSize()) {
                // Room Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "ROOM: ${activeRoom.roomCode}",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            IconButton(
                                onClick = {
                                    val clip = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clip.setPrimaryClip(ClipData.newPlainText("Harbor Code", activeRoom.roomCode))
                                    Toast.makeText(context, "Room code copied!", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "Copy Code",
                                    tint = theme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        Text(
                            text = "Now Playing: ${activeRoom.mediaTitle}",
                            fontSize = 12.sp,
                            color = theme.primary
                        )
                    }

                    Row {
                        IconButton(
                            onClick = {
                                val url = activeRoom.currentStreamUrl ?: "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4"
                                onLaunchSyncedMedia(activeRoom.mediaTitle, url)
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Watch Now",
                                tint = theme.primary
                            )
                        }

                        IconButton(
                            onClick = { watchPartyRepository.leaveRoom() }
                        ) {
                            Icon(
                                imageVector = Icons.Default.ExitToApp,
                                contentDescription = "Leave",
                                tint = Color(0xFFEF4444)
                            )
                        }
                    }
                }

                // Participants Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    activeRoom.participants.forEach { user ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(theme.surfaceVariant)
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(Color(user.avatarColorHex))
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = user.name + if (user.isHost) " (Host)" else "",
                                    fontSize = 11.sp,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }

                // Chat & Messages Stream
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(activeRoom.chatMessages) { msg ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (msg.isReaction) Color.Transparent else theme.surface)
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            if (msg.isReaction) {
                                Text(
                                    text = "${msg.senderName}: ${msg.text}",
                                    fontSize = 20.sp
                                )
                            } else {
                                Column {
                                    Text(
                                        text = msg.senderName,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = theme.primary
                                    )
                                    Text(
                                        text = msg.text,
                                        fontSize = 13.sp,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                }

                // Quick Reactions Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    reactions.forEach { emoji ->
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(theme.surfaceVariant)
                                .clickable {
                                    watchPartyRepository.sendMessage(
                                        text = emoji,
                                        senderName = userNameInput,
                                        isReaction = true
                                    )
                                }
                                .padding(8.dp)
                        ) {
                            Text(text = emoji, fontSize = 18.sp)
                        }
                    }
                }

                // Chat Input bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = chatInput,
                        onValueChange = { chatInput = it },
                        placeholder = { Text("Message room...") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = theme.surface,
                            unfocusedContainerColor = theme.surface
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(
                        onClick = {
                            if (chatInput.isNotBlank()) {
                                watchPartyRepository.sendMessage(chatInput, userNameInput)
                                chatInput = ""
                            }
                        },
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(theme.primary)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = "Send",
                            tint = Color.Black
                        )
                    }
                }
            }
        }
    }
}
