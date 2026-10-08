package com.example.data.repository

import com.example.data.model.WatchPartyMessage
import com.example.data.model.WatchPartyRoom
import com.example.data.model.WatchPartyUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID
import kotlin.random.Random

class WatchPartyRepository {

    private val _currentRoom = MutableStateFlow<WatchPartyRoom?>(null)
    val currentRoom: StateFlow<WatchPartyRoom?> = _currentRoom.asStateFlow()

    fun createRoom(
        hostName: String,
        mediaTitle: String,
        mediaType: String,
        streamUrl: String?
    ): WatchPartyRoom {
        val randomNum = Random.nextInt(1000, 9999)
        val roomCode = "HBR-$randomNum"
        val hostUser = WatchPartyUser(
            id = UUID.randomUUID().toString(),
            name = hostName,
            isHost = true,
            avatarColorHex = 0xFF00E5FF
        )

        val room = WatchPartyRoom(
            roomId = UUID.randomUUID().toString(),
            roomCode = roomCode,
            hostName = hostName,
            mediaTitle = mediaTitle,
            mediaType = mediaType,
            currentPositionMs = 0L,
            isPlaying = true,
            currentStreamUrl = streamUrl,
            participants = listOf(hostUser),
            chatMessages = listOf(
                WatchPartyMessage(
                    id = UUID.randomUUID().toString(),
                    senderName = "Harbor Relay",
                    text = "Room $roomCode created! Share code with friends to sync playback.",
                    timestampMs = System.currentTimeMillis()
                )
            )
        )
        _currentRoom.value = room
        return room
    }

    fun joinRoom(roomCode: String, userName: String): Boolean {
        val formattedCode = roomCode.trim().uppercase()
        val current = _currentRoom.value

        val room = if (current != null && current.roomCode.equals(formattedCode, ignoreCase = true)) {
            val newUser = WatchPartyUser(
                id = UUID.randomUUID().toString(),
                name = userName,
                isHost = false,
                avatarColorHex = 0xFFA855F7
            )
            val updatedParticipants = current.participants + newUser
            val welcomeMsg = WatchPartyMessage(
                id = UUID.randomUUID().toString(),
                senderName = "Harbor Relay",
                text = "$userName joined the watch party!",
                timestampMs = System.currentTimeMillis()
            )
            current.copy(
                participants = updatedParticipants,
                chatMessages = current.chatMessages + welcomeMsg
            )
        } else {
            // Demo join simulated room
            val host = WatchPartyUser(id = "host_1", name = "Captain_Alex", isHost = true, avatarColorHex = 0xFF00E5FF)
            val me = WatchPartyUser(id = "user_me", name = userName, isHost = false, avatarColorHex = 0xFF10B981)
            WatchPartyRoom(
                roomId = UUID.randomUUID().toString(),
                roomCode = formattedCode,
                hostName = "Captain_Alex",
                mediaTitle = "Dune: Part Two",
                mediaType = "movie",
                currentPositionMs = 125000L,
                isPlaying = true,
                currentStreamUrl = null,
                participants = listOf(host, me),
                chatMessages = listOf(
                    WatchPartyMessage("1", "Harbor Relay", "Connected to room $formattedCode", System.currentTimeMillis() - 60000),
                    WatchPartyMessage("2", "Captain_Alex", "Welcome! We just started 2 minutes ago 🍿", System.currentTimeMillis() - 30000)
                )
            )
        }

        _currentRoom.value = room
        return true
    }

    fun sendMessage(text: String, senderName: String, isReaction: Boolean = false) {
        val room = _currentRoom.value ?: return
        val msg = WatchPartyMessage(
            id = UUID.randomUUID().toString(),
            senderName = senderName,
            text = text,
            timestampMs = System.currentTimeMillis(),
            isReaction = isReaction
        )
        _currentRoom.value = room.copy(
            chatMessages = room.chatMessages + msg
        )
    }

    fun updatePlaybackState(positionMs: Long, isPlaying: Boolean) {
        val room = _currentRoom.value ?: return
        _currentRoom.value = room.copy(
            currentPositionMs = positionMs,
            isPlaying = isPlaying
        )
    }

    fun leaveRoom() {
        _currentRoom.value = null
    }
}
