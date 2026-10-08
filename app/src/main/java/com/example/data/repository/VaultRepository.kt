package com.example.data.repository

import com.example.data.local.WatchHistoryDao
import com.example.data.local.WatchHistoryEntity
import com.example.data.local.WatchlistDao
import com.example.data.local.WatchlistEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class VaultRepository(
    private val historyDao: WatchHistoryDao,
    private val watchlistDao: WatchlistDao
) {
    val continueWatching: Flow<List<WatchHistoryEntity>> = historyDao.getAllHistory().map { list ->
        list.groupBy { it.mediaId }
            .mapValues { (_, entries) -> entries.maxByOrNull { it.lastWatchedTimestamp }!! }
            .values
            .sortedByDescending { it.lastWatchedTimestamp }
    }
    val allHistory: Flow<List<WatchHistoryEntity>> = historyDao.getAllHistory()
    val allWatchlist: Flow<List<WatchlistEntity>> = watchlistDao.getAllWatchlist()

    fun isBookmarked(id: String): Flow<Boolean> = watchlistDao.isInWatchlist(id)

    suspend fun savePlaybackProgress(
        id: String,
        mediaId: String,
        title: String,
        type: String,
        poster: String?,
        background: String?,
        season: Int? = null,
        episode: Int? = null,
        episodeTitle: String? = null,
        positionMs: Long,
        durationMs: Long,
        streamUrl: String?
    ) {
        val entity = WatchHistoryEntity(
            id = id,
            mediaId = mediaId,
            title = title,
            type = type,
            poster = poster,
            background = background,
            season = season,
            episode = episode,
            episodeTitle = episodeTitle,
            positionMs = positionMs,
            durationMs = durationMs,
            streamUrl = streamUrl,
            lastWatchedTimestamp = System.currentTimeMillis()
        )
        historyDao.upsert(entity)
    }

    suspend fun toggleWatchlist(
        id: String,
        title: String,
        type: String,
        poster: String?,
        background: String?,
        releaseYear: String?,
        imdbRating: String?,
        genres: String?
    ): Boolean {
        val exists = watchlistDao.isInWatchlistSync(id)
        if (exists) {
            watchlistDao.deleteById(id)
            return false
        } else {
            watchlistDao.insert(
                WatchlistEntity(
                    id = id,
                    title = title,
                    type = type,
                    poster = poster,
                    background = background,
                    releaseYear = releaseYear,
                    imdbRating = imdbRating,
                    genres = genres
                )
            )
            return true
        }
    }

    suspend fun deleteHistoryItem(id: String) {
        historyDao.deleteById(id)
    }

    suspend fun clearHistory() {
        historyDao.clearAll()
    }

    suspend fun syncLibraryFromStremio(entries: List<com.example.data.model.StremioLibraryEntry>): Int {
        var count = 0
        entries.forEach { entry ->
            val poster = entry.poster ?: if (entry.id.startsWith("tt")) "https://images.metahub.space/poster/medium/${entry.id}/img" else null
            val key = if (entry.season != null && entry.episode != null) "${entry.id}:${entry.season}:${entry.episode}" else entry.id
            val pos = if (entry.positionMs > 0) entry.positionMs else 180000L
            val dur = if (entry.durationMs > 0) entry.durationMs else 3600000L

            val historyEntity = WatchHistoryEntity(
                id = key,
                mediaId = entry.id,
                title = entry.name,
                type = entry.type,
                poster = poster,
                background = entry.background,
                season = entry.season,
                episode = entry.episode,
                episodeTitle = if (entry.season != null && entry.episode != null) "S${entry.season}:E${entry.episode}" else null,
                positionMs = pos,
                durationMs = dur,
                streamUrl = null,
                lastWatchedTimestamp = entry.lastWatchedTimestamp
            )
            historyDao.upsert(historyEntity)

            val watchlistEntity = WatchlistEntity(
                id = entry.id,
                title = entry.name,
                type = entry.type,
                poster = poster,
                background = entry.background,
                releaseYear = null,
                imdbRating = null,
                genres = null
            )
            watchlistDao.insert(watchlistEntity)
            count++
        }
        return count
    }
}
