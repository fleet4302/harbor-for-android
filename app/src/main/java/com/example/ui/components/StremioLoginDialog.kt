package com.example.ui.components

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.api.StremioApiClient
import com.example.data.local.StremioAccountSession
import com.example.data.repository.AddonRepository
import com.example.ui.theme.LocalHarborTheme
import kotlinx.coroutines.launch

@Composable
fun StremioLoginDialog(
    stremioSession: StremioAccountSession,
    apiClient: StremioApiClient,
    addonRepository: AddonRepository,
    onDismiss: () -> Unit,
    onSuccess: () -> Unit
) {
    val theme = LocalHarborTheme.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedTab by remember { mutableIntStateOf(0) } // 0: Credentials, 1: AuthKey
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var authKeyInput by remember { mutableStateOf("") }

    var isLoading by remember { mutableStateOf(false) }
    var statusText by remember { mutableStateOf<String?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    fun performLoginWithCredentials() {
        if (email.isBlank() || password.isBlank()) return
        scope.launch {
            isLoading = true
            errorMessage = null
            statusText = "Authenticating with Stremio API..."

            val res = apiClient.login(email.trim(), password)
            if (res.isSuccess) {
                val loginResult = res.getOrThrow()
                val authKey = loginResult.authKey
                val userEmail = loginResult.user?.email ?: email.trim()
                val userId = loginResult.user?._id

                stremioSession.saveSession(
                    authKey = authKey,
                    email = userEmail,
                    userId = userId
                )

                statusText = "Syncing cloud addons from your Stremio library..."
                val syncRes = addonRepository.syncAddonsFromStremioAccount(authKey)

                isLoading = false
                val count = syncRes.getOrNull() ?: 0
                Toast.makeText(context, "Logged in! Synced $count addons from Stremio.", Toast.LENGTH_SHORT).show()
                onSuccess()
            } else {
                isLoading = false
                statusText = null
                errorMessage = res.exceptionOrNull()?.message ?: "Invalid email or password"
            }
        }
    }

    fun performLoginWithAuthKey() {
        if (authKeyInput.isBlank()) return
        scope.launch {
            isLoading = true
            errorMessage = null
            statusText = "Validating AuthKey with Stremio Cloud..."

            val key = authKeyInput.trim()
            val syncRes = addonRepository.syncAddonsFromStremioAccount(key)

            if (syncRes.isSuccess) {
                stremioSession.saveSession(
                    authKey = key,
                    email = "Authenticated User",
                    userId = null
                )
                isLoading = false
                val count = syncRes.getOrThrow()
                Toast.makeText(context, "AuthKey verified! Synced $count addons.", Toast.LENGTH_SHORT).show()
                onSuccess()
            } else {
                isLoading = false
                statusText = null
                errorMessage = syncRes.exceptionOrNull()?.message ?: "Invalid Stremio AuthKey"
            }
        }
    }

    AlertDialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        containerColor = theme.surface,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("stremio_login_dialog"),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(theme.primary.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountCircle,
                            contentDescription = null,
                            tint = theme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Stremio Cloud Login",
                            color = Color.White,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Sync Addons & Stream Configurations",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp
                        )
                    }
                }

                IconButton(
                    onClick = onDismiss,
                    enabled = !isLoading
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
                // Tab Selection (Email / AuthKey)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (selectedTab == 0) theme.primary else theme.surfaceVariant)
                            .clickable { if (!isLoading) selectedTab = 0 }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Email & Password",
                            fontSize = 12.sp,
                            fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Medium,
                            color = if (selectedTab == 0) Color.Black else Color.White
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (selectedTab == 1) theme.primary else theme.surfaceVariant)
                            .clickable { if (!isLoading) selectedTab = 1 }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Stremio AuthKey",
                            fontSize = 12.sp,
                            fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Medium,
                            color = if (selectedTab == 1) Color.Black else Color.White
                        )
                    }
                }

                if (selectedTab == 0) {
                    // Email & Password Fields
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Stremio Email") },
                        placeholder = { Text("user@example.com") },
                        singleLine = true,
                        enabled = !isLoading,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = theme.surfaceVariant,
                            unfocusedContainerColor = theme.surfaceVariant
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("dialog_stremio_email_input")
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password") },
                        singleLine = true,
                        enabled = !isLoading,
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = "Toggle password visibility",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = theme.surfaceVariant,
                            unfocusedContainerColor = theme.surfaceVariant
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("dialog_stremio_password_input")
                    )
                } else {
                    // AuthKey tab
                    Text(
                        text = "For accounts registered using Google, Facebook, or Apple:",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = authKeyInput,
                        onValueChange = { authKeyInput = it },
                        label = { Text("AuthKey") },
                        placeholder = { Text("Paste AuthKey from web.stremio.com console") },
                        singleLine = true,
                        enabled = !isLoading,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = theme.surfaceVariant,
                            unfocusedContainerColor = theme.surfaceVariant
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("dialog_stremio_authkey_input")
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Tip: Open web.stremio.com in browser, press F12 (Console), and type: localStorage.getItem('authKey')",
                        fontSize = 10.sp,
                        lineHeight = 14.sp,
                        color = theme.primary.copy(alpha = 0.85f)
                    )
                }

                // Status or Error Feedback
                if (statusText != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(theme.primary.copy(alpha = 0.15f))
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = theme.primary,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = statusText ?: "",
                            fontSize = 12.sp,
                            color = theme.primary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFEF4444).copy(alpha = 0.15f))
                            .padding(10.dp)
                    ) {
                        Text(
                            text = errorMessage ?: "",
                            color = Color(0xFFEF4444),
                            fontSize = 12.sp
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (selectedTab == 0) {
                        performLoginWithCredentials()
                    } else {
                        performLoginWithAuthKey()
                    }
                },
                enabled = !isLoading && if (selectedTab == 0) email.isNotBlank() && password.isNotBlank() else authKeyInput.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = theme.primary,
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.testTag("dialog_login_submit_button")
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = Color.Black,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Connecting...", fontWeight = FontWeight.Bold)
                } else {
                    Text(
                        if (selectedTab == 0) "Sign In & Sync" else "Connect via AuthKey",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                enabled = !isLoading,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Cancel")
            }
        }
    )
}
