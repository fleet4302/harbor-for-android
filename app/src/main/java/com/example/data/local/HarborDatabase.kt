package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        WatchHistoryEntity::class,
        WatchlistEntity::class,
        AddonEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class HarborDatabase : RoomDatabase() {
    abstract fun watchHistoryDao(): WatchHistoryDao
    abstract fun watchlistDao(): WatchlistDao
    abstract fun addonDao(): AddonDao

    companion object {
        @Volatile
        private var INSTANCE: HarborDatabase? = null

        fun getInstance(context: Context): HarborDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    HarborDatabase::class.java,
                    "harbor_stremio.db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
