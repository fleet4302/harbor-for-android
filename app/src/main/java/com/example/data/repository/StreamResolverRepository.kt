package com.example.data.repository

import android.content.Context
import com.example.data.api.StremioApiClient
import com.example.data.local.AddonEntity
import com.example.data.model.HarborParsedStream
import com.example.data.model.HarborStreamParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext

class StreamResolverRepository(
    private val context: Context,
    private val addonRepository: AddonRepository,
    private val apiClient: StremioApiClient
) {

    fun isStreamSourceLinked(): Boolean {
        val prefs = context.getSharedPreferences("harbor_prefs", Context.MODE_PRIVATE)
        val accountPrefs = context.getSharedPreferences("harbor_stremio_account", Context.MODE_PRIVATE)
        val hasStremioLogin = !accountPrefs.getString("auth_key", null).isNullOrBlank()
        val hasCustomTorrentio = !prefs.getString("custom_torrentio_url", null).isNullOrBlank()
        val hasDebridKey = !prefs.getString("debrid_key", null).isNullOrBlank()
        val isTorrentioLinked = prefs.getBoolean("torrentio_linked", false)
        return hasStremioLogin || hasCustomTorrentio || hasDebridKey || isTorrentioLinked
    }

    suspend fun linkTorrentio(customUrl: String? = null, debridKey: String? = null) {
        val prefs = context.getSharedPreferences("harbor_prefs", Context.MODE_PRIVATE)
        val editor = prefs.edit().putBoolean("torrentio_linked", true)
        if (!customUrl.isNullOrBlank()) {
            editor.putString("custom_torrentio_url", customUrl.trim())
        }
        if (!debridKey.isNullOrBlank()) {
            editor.putString("debrid_key", debridKey.trim())
        }
        editor.apply()

        val manifestUrl = if (!customUrl.isNullOrBlank()) {
            customUrl.trim()
        } else {
            "https://torrentio.strem.fun/manifest.json"
        }

        addonRepository.installOrUpdateStreamAddon(
            id = "community.torrentio",
            name = "Torrentio",
            manifestUrl = manifestUrl,
            description = "Aggregates real torrent streams across trackers with resolution, seeds, and Debrid support."
        )
    }

    suspend fun unlinkTorrentio() {
        val prefs = context.getSharedPreferences("harbor_prefs", Context.MODE_PRIVATE)
        prefs.edit()
            .putBoolean("torrentio_linked", false)
            .remove("custom_torrentio_url")
            .apply()
        addonRepository.uninstallAddon("community.torrentio")
        addonRepository.uninstallAddon("custom.torrentio")
    }

    suspend fun resolveStreams(
        type: String,
        id: String,
        debridApiKey: String? = null
    ): List<HarborParsedStream> = withContext(Dispatchers.IO) {
        // STRICT USER REQUIREMENT:
        // Do NOT show streams before linking anything! If no torrent source or Stremio account
        // has been linked by the user, return empty list immediately.
        if (!isStreamSourceLinked()) {
            return@withContext emptyList()
        }

        val prefs = context.getSharedPreferences("harbor_prefs", Context.MODE_PRIVATE)
        val activeDebridKey = if (!debridApiKey.isNullOrBlank()) debridApiKey else prefs.getString("debrid_key", null)?.trim()
        val customTorrentio = prefs.getString("custom_torrentio_url", null)?.trim()
        val isTorrentioLinked = prefs.getBoolean("torrentio_linked", false)

        // Get currently active stream addons installed or synced by the user
        val enabledAddons = addonRepository.getEnabledAddons().filter { it.supportsStream }.toMutableList()

        // If Torrentio is linked or user set a custom Torrentio URL, ensure it's in the resolution pipeline
        if ((isTorrentioLinked || !customTorrentio.isNullOrBlank()) && enabledAddons.none { it.id == "community.torrentio" || it.manifestUrl == customTorrentio }) {
            val manifestUrl = if (!customTorrentio.isNullOrBlank()) customTorrentio else "https://torrentio.strem.fun/manifest.json"
            enabledAddons.add(
                AddonEntity(
                    id = "community.torrentio",
                    manifestUrl = manifestUrl,
                    name = "Torrentio",
                    supportsStream = true
                )
            )
        }

        if (enabledAddons.isEmpty()) {
            return@withContext emptyList()
        }

        val streamList = mutableListOf<HarborParsedStream>()

        coroutineScope {
            val deferreds = enabledAddons.map { addon ->
                async {
                    var addonUrl = addon.manifestUrl
                    if (!activeDebridKey.isNullOrBlank()) {
                        if (addonUrl.contains("torrentio.strem.fun") && !addonUrl.contains("realdebrid=")) {
                            addonUrl = addonUrl.replace("torrentio.strem.fun", "torrentio.strem.fun/realdebrid=$activeDebridKey")
                        }
                    }
                    val result = apiClient.fetchStreams(addonUrl, type, id)
                    if (result.isSuccess) {
                        result.getOrNull()?.mapNotNull { rawItem ->
                            // Discard YouTube previews/trailers
                            if (!rawItem.ytId.isNullOrBlank()) return@mapNotNull null

                            // Discard non-torrent web store or purchase redirect links
                            if (!rawItem.externalUrl.isNullOrBlank() && rawItem.infoHash.isNullOrBlank() && (rawItem.url == null || !rawItem.url.startsWith("magnet:"))) {
                                return@mapNotNull null
                            }

                            // Strict verification: Must link to a real torrent or Debrid-resolved stream
                            val hasInfoHash = !rawItem.infoHash.isNullOrBlank()
                            val hasMagnet = !rawItem.url.isNullOrBlank() && rawItem.url.startsWith("magnet:")
                            val hasDirectDebridStream = !rawItem.url.isNullOrBlank() && (rawItem.url.startsWith("http://") || rawItem.url.startsWith("https://"))

                            if (hasInfoHash || hasMagnet || hasDirectDebridStream) {
                                HarborStreamParser.parse(rawItem, addon.name)
                            } else {
                                null
                            }
                        } ?: emptyList()
                    } else {
                        emptyList()
                    }
                }
            }

            val results = deferreds.awaitAll()
            results.forEach { streamList.addAll(it) }
        }

        // Sort using Harbor's ranking algorithm (Debrid/4K/Seeders)
        streamList.sortedByDescending { it.harborScore }
    }
}
