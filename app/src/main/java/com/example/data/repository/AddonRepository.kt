package com.example.data.repository

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
    private val addonDao: AddonDao,
    private val apiClient: StremioApiClient
) {
    val allAddons: Flow<List<AddonEntity>> = addonDao.getAllAddons()

    init {
        // Initialize defaults if database is empty
        CoroutineScope(Dispatchers.IO).launch {
            val existing = addonDao.getAllAddons().firstOrNull()
            if (existing.isNullOrEmpty()) {
                addonDao.insertAll(DefaultAddons.INITIAL_ADDONS)
            }
        }
    }

    suspend fun getEnabledAddons(): List<AddonEntity> {
        val enabled = addonDao.getEnabledAddonsSync()
        return if (enabled.isEmpty()) DefaultAddons.INITIAL_ADDONS else enabled
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
}
