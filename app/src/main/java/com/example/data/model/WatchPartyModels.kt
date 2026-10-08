package com.example.data.model

data class WatchPartyRoom(
    val roomId: String,
    val roomCode: String,
    val hostName: String,
    val mediaTitle: String,
    val mediaType: String,
    val currentPositionMs: Long,
    val isPlaying: Boolean,
    val currentStreamUrl: String?,
    val participants: List<WatchPartyUser>,
    val chatMessages: List<WatchPartyMessage>
)

data class WatchPartyUser(
    val id: String,
    val name: String,
    val isHost: Boolean,
    val avatarColorHex: Long = 0xFF00E5FF
)

data class WatchPartyMessage(
    val id: String,
    val senderName: String,
    val text: String,
    val timestampMs: Long,
    val isReaction: Boolean = false
)
