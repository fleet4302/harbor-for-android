package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.data.api.DefaultAddons
import com.example.data.api.StremioApiClient
import com.example.data.local.AddonDao
import com.example.data.local.AddonEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

class AddonRepository(
    private val context: Context,
    private val addonDao: AddonDao,
    private val apiClient: StremioApiClient
) {
    val allAddons: Flow<List<AddonEntity>> = addonDao.getAllAddons()

    init {
        CoroutineScope(Dispatchers.IO).launch {
            val prefs = context.getSharedPreferences("harbor_prefs", Context.MODE_PRIVATE)
            val accountPrefs = context.getSharedPreferences("harbor_stremio_account", Context.MODE_PRIVATE)
            val hasStremioLogin = !accountPrefs.getString("auth_key", null).isNullOrBlank()
            val hasCustomTorrentio = !prefs.getString("custom_torrentio_url", null).isNullOrBlank()
            val hasDebridKey = !prefs.getString("debrid_key", null).isNullOrBlank()
            val isTorrentioLinked = prefs.getBoolean("torrentio_linked", false)
            val isLinked = hasStremioLogin || hasCustomTorrentio || hasDebridKey || isTorrentioLinked

            val existing = addonDao.getAllAddons().firstOrNull()
            if (existing.isNullOrEmpty()) {
                addonDao.insertAll(DefaultAddons.INITIAL_ADDONS)
            } else {
                // Remove legacy WatchHub and AnimeKitsu
                addonDao.deleteById("community.watchhub")
                addonDao.deleteById("community.animekitsu")

                // If user has NOT explicitly linked a stream source yet, purge any leftover default stream addons
                if (!isLinked) {
                    addonDao.deleteById("community.torrentio")
                    addonDao.deleteById("custom.torrentio")
                }
            }
        }
    }

    suspend fun getEnabledAddons(): List<AddonEntity> {
        val enabled = addonDao.getEnabledAddonsSync()
        return if (enabled.isEmpty()) DefaultAddons.INITIAL_ADDONS else enabled
    }

    suspend fun installOrUpdateStreamAddon(
        id: String,
        name: String,
        manifestUrl: String,
        description: String
    ) {
        val entity = AddonEntity(
            id = id,
            manifestUrl = manifestUrl.trim(),
            name = name,
            version = "1.0.0",
            description = description,
            iconUrl = null,
            isEnabled = true,
            isOfficial = false,
            orderIndex = 1,
            supportsCatalog = false,
            supportsStream = true,
            supportsSubtitles = false
        )
        addonDao.insert(entity)
    }

    suspend fun toggleAddon(id: String, isEnabled: Boolean) {
        addonDao.updateEnabled(id, isEnabled)
    }

    suspend fun installAddonFromUrl(url: String): Result<AddonEntity> {
        val manifestResult = apiClient.fetchManifest(url)
        if (manifestResult.isFailure) {
            return Result.failure(manifestResult.exceptionOrNull() ?: Exception("Failed to load manifest"))
        }

        val manifest = manifestResult.getOrThrow()
        val supportsCatalog = !manifest.catalogs.isNullOrEmpty()
        val supportsStream = manifest.resources?.any {
            it.toString().contains("stream")
        } ?: true
        val supportsSubtitles = manifest.resources?.any {
            it.toString().contains("subtitles")
        } ?: false

        val entity = AddonEntity(
            id = manifest.id,
            manifestUrl = url.trim(),
            name = manifest.name,
            version = manifest.version ?: "1.0.0",
            description = manifest.description ?: "",
            iconUrl = manifest.logo ?: manifest.icon,
            isEnabled = true,
            isOfficial = false,
            orderIndex = 10,
            supportsCatalog = supportsCatalog,
            supportsStream = supportsStream,
            supportsSubtitles = supportsSubtitles
        )

        addonDao.insert(entity)
        return Result.success(entity)
    }

    suspend fun uninstallAddon(id: String) {
        addonDao.deleteById(id)
    }

    suspend fun resetToDefaults() {
        DefaultAddons.INITIAL_ADDONS.forEach {
            addonDao.insert(it)
        }
    }

    suspend fun syncAddonsFromStremioAccount(authKey: String): Result<Int> {
        Log.d("AddonRepository", "Syncing addons with authKey: ${authKey.take(5)}...")
        val result = apiClient.getAddonCollection(authKey)
        if (result.isFailure) {
            Log.e("AddonRepository", "Failed to fetch addons", result.exceptionOrNull())
            return Result.failure(result.exceptionOrNull() ?: Exception("Failed to fetch addons from Stremio"))
        }

        val addons = result.getOrThrow()
        Log.d("AddonRepository", "Fetched ${addons.size} addons")
        var count = 0
        addons.forEachIndexed { index, item ->
            val manifest = item.manifest
            Log.d("AddonRepository", "Processing addon: ${manifest.name} (${manifest.id})")
            val supportsCatalog = !manifest.catalogs.isNullOrEmpty()
            val supportsStream = manifest.resources?.any {
                it.toString().contains("stream", ignoreCase = true)
            } ?: true
            val supportsSubtitles = manifest.resources?.any {
                it.toString().contains("subtitles", ignoreCase = true)
            } ?: false

            val entity = AddonEntity(
                id = manifest.id,
                manifestUrl = item.transportUrl,
                name = manifest.name,
                version = manifest.version ?: "1.0.0",
                description = manifest.description ?: "",
                iconUrl = manifest.logo ?: manifest.icon,
                isEnabled = true,
                isOfficial = manifest.id.startsWith("community.cinemeta") || manifest.id.startsWith("org.stremio"),
                orderIndex = index,
                supportsCatalog = supportsCatalog,
                supportsStream = supportsStream,
                supportsSubtitles = supportsSubtitles
            )
            addonDao.insert(entity)
            count++
        }
        return Result.success(count)
    }
}
