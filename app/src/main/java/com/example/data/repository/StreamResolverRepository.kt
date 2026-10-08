package com.example.data.repository

import com.example.data.api.DefaultAddons
import com.example.data.api.StremioApiClient
import com.example.data.model.HarborParsedStream
import com.example.data.model.HarborStreamParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext

class StreamResolverRepository(
    private val addonRepository: AddonRepository,
    private val apiClient: StremioApiClient
) {

    suspend fun resolveStreams(
        type: String,
        id: String,
        debridApiKey: String? = null
    ): List<HarborParsedStream> = withContext(Dispatchers.IO) {
        val enabledAddons = addonRepository.getEnabledAddons().filter { it.supportsStream }
        val streamList = mutableListOf<HarborParsedStream>()

        coroutineScope {
            val deferreds = enabledAddons.map { addon ->
                async {
                    var addonUrl = addon.manifestUrl
                    if (!debridApiKey.isNullOrBlank() && addonUrl.contains("torrentio.strem.fun")) {
                        // Torrentio allows realdebrid=APIKEY in url path
                        addonUrl = addonUrl.replace("torrentio.strem.fun", "torrentio.strem.fun/realdebrid=$debridApiKey")
                    }
                    val result = apiClient.fetchStreams(addonUrl, type, id)
                    if (result.isSuccess) {
                        result.getOrNull()?.map { rawItem ->
                            HarborStreamParser.parse(rawItem, addon.name)
                        } ?: emptyList()
                    } else {
                        emptyList()
                    }
                }
            }

            val results = deferreds.awaitAll()
            results.forEach { streamList.addAll(it) }
        }

        // If no streams were found (e.g. offline, mock ID, or addon rate limit), supply Harbor demo streams
        if (streamList.isEmpty()) {
            val sampleItems = DefaultAddons.SAMPLE_STREAMS.map {
                HarborStreamParser.parse(it, "Harbor Direct")
            }
            streamList.addAll(sampleItems)
        }

        // Sort using Harbor's ranking algorithm
        streamList.sortedByDescending { it.harborScore }
    }
}
