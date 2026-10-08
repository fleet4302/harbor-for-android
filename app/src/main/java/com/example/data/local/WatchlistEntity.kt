package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "watchlist")
data class WatchlistEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val type: String,
    val poster: String?,
    val background: String?,
    val releaseYear: String?,
    val imdbRating: String?,
    val genres: String? = null,
    val addedTimestamp: Long = System.currentTimeMillis()
)

@Dao
interface WatchlistDao {
    @Query("SELECT * FROM watchlist ORDER BY addedTimestamp DESC")
    fun getAllWatchlist(): Flow<List<WatchlistEntity>>

    @Query("SELECT * FROM watchlist WHERE type = :type ORDER BY addedTimestamp DESC")
    fun getWatchlistByType(type: String): Flow<List<WatchlistEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM watchlist WHERE id = :id)")
    fun isInWatchlist(id: String): Flow<Boolean>

    @Query("SELECT EXISTS(SELECT 1 FROM watchlist WHERE id = :id)")
    suspend fun isInWatchlistSync(id: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: WatchlistEntity)

    @Delete
    suspend fun delete(item: WatchlistEntity)

    @Query("DELETE FROM watchlist WHERE id = :id")
    suspend fun deleteById(id: String)
}
