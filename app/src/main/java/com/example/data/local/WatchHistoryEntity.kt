package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "watch_history")
data class WatchHistoryEntity(
    @PrimaryKey
    val id: String, // e.g. tt12345 or tt12345:1:2
    val mediaId: String,
    val title: String,
    val type: String,
    val poster: String?,
    val background: String?,
    val season: Int? = null,
    val episode: Int? = null,
    val episodeTitle: String? = null,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val streamUrl: String? = null,
    val lastWatchedTimestamp: Long = System.currentTimeMillis()
)

@Dao
interface WatchHistoryDao {
    @Query("SELECT * FROM watch_history ORDER BY lastWatchedTimestamp DESC")
    fun getAllHistory(): Flow<List<WatchHistoryEntity>>

    @Query("SELECT * FROM watch_history ORDER BY lastWatchedTimestamp DESC LIMIT 20")
    fun getContinueWatching(): Flow<List<WatchHistoryEntity>>

    @Query("SELECT * FROM watch_history WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): WatchHistoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: WatchHistoryEntity)

    @Delete
    suspend fun delete(item: WatchHistoryEntity)

    @Query("DELETE FROM watch_history WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM watch_history")
    suspend fun clearAll()
}
