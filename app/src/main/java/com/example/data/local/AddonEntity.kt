package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "installed_addons")
data class AddonEntity(
    @PrimaryKey
    val id: String,
    val manifestUrl: String,
    val name: String,
    val version: String = "1.0.0",
    val description: String = "",
    val iconUrl: String? = null,
    val isEnabled: Boolean = true,
    val isOfficial: Boolean = false,
    val orderIndex: Int = 0,
    val supportsCatalog: Boolean = false,
    val supportsStream: Boolean = false,
    val supportsSubtitles: Boolean = false
)

@Dao
interface AddonDao {
    @Query("SELECT * FROM installed_addons ORDER BY orderIndex ASC")
    fun getAllAddons(): Flow<List<AddonEntity>>

    @Query("SELECT * FROM installed_addons WHERE isEnabled = 1 ORDER BY orderIndex ASC")
    suspend fun getEnabledAddonsSync(): List<AddonEntity>

    @Query("SELECT * FROM installed_addons WHERE id = :id LIMIT 1")
    suspend fun getAddonById(id: String): AddonEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(addons: List<AddonEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(addon: AddonEntity)

    @Query("UPDATE installed_addons SET isEnabled = :isEnabled WHERE id = :id")
    suspend fun updateEnabled(id: String, isEnabled: Boolean)

    @Delete
    suspend fun delete(addon: AddonEntity)

    @Query("DELETE FROM installed_addons WHERE id = :id")
    suspend fun deleteById(id: String)
}
