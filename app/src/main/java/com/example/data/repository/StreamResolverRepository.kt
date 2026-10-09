package com.example.data.repository

import android.content.Context
import com.example.data.api.DebridAccountInfo
import com.example.data.api.DebridApiClient
import com.example.data.api.StremioApiClient
import com.example.data.local.AddonEntity
import com.example.data.model.HarborParsedStream
import com.example.data.model.HarborStreamParser
import com.example.data.model.StremioManifest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext

class StreamResolverRepository(
    private val context: Context,
    private val addonRepository: AddonRepository,
    private val apiClient: StremioApiClient,
    private val debridApiClient: DebridApiClient = DebridApiClient()
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

    fun getLinkedDebridInfo(): DebridAccountInfo? {
        val prefs = context.getSharedPreferences("harbor_prefs", Context.MODE_PRIVATE)
        val key = prefs.getString("debrid_key", null)
        if (key.isNullOrBlank()) return null
        val service = prefs.getString("debrid_service", "Real-Debrid") ?: "Real-Debrid"
        val username = prefs.getString("debrid_username", "Connected Account") ?: "Connected Account"
        val email = prefs.getString("debrid_email", null)
        val isPremium = prefs.getBoolean("debrid_is_premium", true)
        val expiration = prefs.getString("debrid_expiration", null)
        val days = prefs.getInt("debrid_days", -1).let { if (it >= 0) it else null }
        return DebridAccountInfo(
            service = service,
            username = username,
            email = email,
            isPremium = isPremium,
            expiration = expiration,
            daysRemaining = days
        )
    }

    suspend fun validateDebridKey(service: String, token: String): Result<DebridAccountInfo> {
        return debridApiClient.validateKey(service, token)
    }

    suspend fun validateTorrentioUrl(url: String): Result<StremioManifest> {
        return apiClient.validateTorrentioManifest(url)
    }

    suspend fun resolveMagnetViaDebrid(magnetUri: String): Result<String> {
        val prefs = context.getSharedPreferences("harbor_prefs", Context.MODE_PRIVATE)
        val key = prefs.getString("debrid_key", null)?.trim()
            ?: return Result.failure(IllegalStateException("No Debrid API key configured"))
        return debridApiClient.resolveMagnetViaRealDebrid(key, magnetUri)
    }

    suspend fun linkTorrentio(
        customUrl: String? = null,
        debridKey: String? = null,
        debridService: String = "realdebrid",
        verifiedInfo: DebridAccountInfo? = null
    ) {
        val prefs = context.getSharedPreferences("harbor_prefs", Context.MODE_PRIVATE)
        val editor = prefs.edit().putBoolean("torrentio_linked", true)
        if (!customUrl.isNullOrBlank()) {
            editor.putString("custom_torrentio_url", customUrl.trim())
        }
        if (!debridKey.isNullOrBlank()) {
            editor.putString("debrid_key", debridKey.trim())
            editor.putString("debrid_service", debridService)
            if (verifiedInfo != null) {
                editor.putString("debrid_username", verifiedInfo.username)
                editor.putString("debrid_email", verifiedInfo.email)
                editor.putBoolean("debrid_is_premium", verifiedInfo.isPremium)
                editor.putString("debrid_expiration", verifiedInfo.expiration)
                editor.putInt("debrid_days", verifiedInfo.daysRemaining ?: -1)
            }
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

    fun unlinkDebrid() {
        val prefs = context.getSharedPreferences("harbor_prefs", Context.MODE_PRIVATE)
        prefs.edit()
            .remove("debrid_key")
            .remove("debrid_service")
            .remove("debrid_username")
            .remove("debrid_email")
            .remove("debrid_is_premium")
            .remove("debrid_expiration")
            .remove("debrid_days")
            .apply()
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

        val defaultScraperUrl = if (!activeDebridKey.isNullOrBlank()) {
            "https://torrentio.strem.fun/realdebrid=$activeDebridKey|sort=qualityseeders|limit=25/manifest.json"
        } else {
            "https://torrentio.strem.fun/sort=qualityseeders|limit=25/manifest.json"
        }

        // If Torrentio is linked or user set a custom Torrentio URL, ensure it's in the resolution pipeline
        if (isTorrentioLinked || !customTorrentio.isNullOrBlank()) {
            val manifestUrl = if (!customTorrentio.isNullOrBlank()) customTorrentio else defaultScraperUrl
            if (enabledAddons.none { it.id == "community.torrentio" || it.manifestUrl == customTorrentio }) {
                enabledAddons.add(
                    AddonEntity(
                        id = "community.torrentio",
                        manifestUrl = manifestUrl,
                        name = "Torrentio",
                        supportsStream = true
                    )
                )
            }
        }

        // Add KnightCrawler multi-source stream aggregator if fewer than 2 stream addons exist
        if (enabledAddons.size <= 1 && enabledAddons.none { it.manifestUrl.contains("knightcrawler") }) {
            enabledAddons.add(
                AddonEntity(
                    id = "community.knightcrawler",
                    manifestUrl = "https://knightcrawler.elfhosted.com/manifest.json",
                    name = "KnightCrawler",
                    supportsStream = true
                )
            )
        }

        if (enabledAddons.isEmpty()) {
            return@withContext emptyList()
        }

        val streamList = mutableListOf<HarborParsedStream>()
        val resolvedType = if (type == "tv") "series" else type

        coroutineScope {
            val deferreds = enabledAddons.map { addon ->
                async {
                    kotlinx.coroutines.withTimeoutOrNull(4500L) {
                        var addonUrl = addon.manifestUrl
                        // If bare Torrentio manifest, upgrade to optimized scraper url
                        if (addonUrl == "https://torrentio.strem.fun/manifest.json") {
                            addonUrl = defaultScraperUrl
                        }
                        val result = apiClient.fetchStreams(addonUrl, resolvedType, id)
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
                    } ?: emptyList()
                }
            }

            val results = deferreds.awaitAll()
            results.forEach { streamList.addAll(it) }
        }

        // Sort using Harbor's ranking algorithm (Debrid/4K/Seeders)
        streamList.sortedByDescending { it.harborScore }
    }
}
