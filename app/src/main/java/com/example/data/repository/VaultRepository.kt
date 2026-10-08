package com.example.data.repository

import com.example.data.local.WatchHistoryDao
import com.example.data.local.WatchHistoryEntity
import com.example.data.local.WatchlistDao
import com.example.data.local.WatchlistEntity
import kotlinx.coroutines.flow.Flow

class VaultRepository(
    private val historyDao: WatchHistoryDao,
    private val watchlistDao: WatchlistDao
) {
    val continueWatching: Flow<List<WatchHistoryEntity>> = historyDao.getContinueWatching()
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
}
