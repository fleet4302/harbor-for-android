package com.example.data.repository

import com.example.data.api.DefaultAddons
import com.example.data.api.StremioApiClient
import com.example.data.model.StremioMetaDetail
import com.example.data.model.StremioMetaSummary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CatalogRepository(
    private val addonRepository: AddonRepository,
    private val apiClient: StremioApiClient
) {

    suspend fun getPopularMovies(genre: String? = null): List<StremioMetaSummary> = withContext(Dispatchers.IO) {
        val extra = mutableMapOf<String, String>()
        if (!genre.isNullOrBlank() && genre != "All") {
            extra["genre"] = genre
        }

        val cinemetaUrl = "https://v3-cinemeta.strem.io"
        val result = apiClient.fetchCatalog(cinemetaUrl, "movie", "top", extra)
        if (result.isSuccess && result.getOrNull()?.isNotEmpty() == true) {
            result.getOrThrow()
        } else {
            DefaultAddons.FALLBACK_CATALOG.filter { it.type == "movie" }
        }
    }

    suspend fun getPopularSeries(genre: String? = null): List<StremioMetaSummary> = withContext(Dispatchers.IO) {
        val extra = mutableMapOf<String, String>()
        if (!genre.isNullOrBlank() && genre != "All") {
            extra["genre"] = genre
        }

        val cinemetaUrl = "https://v3-cinemeta.strem.io"
        val result = apiClient.fetchCatalog(cinemetaUrl, "series", "top", extra)
        if (result.isSuccess && result.getOrNull()?.isNotEmpty() == true) {
            result.getOrThrow()
        } else {
            DefaultAddons.FALLBACK_CATALOG.filter { it.type == "series" }
        }
    }

    suspend fun search(query: String, type: String? = null): List<StremioMetaSummary> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()

        val cinemetaUrl = "https://v3-cinemeta.strem.io"
        val targetType = type ?: "movie"
        val extra = mapOf("search" to query)

        val result = apiClient.fetchCatalog(cinemetaUrl, targetType, "top", extra)
        if (result.isSuccess && result.getOrNull()?.isNotEmpty() == true) {
            result.getOrThrow()
        } else {
            // Filter local fallback
            DefaultAddons.FALLBACK_CATALOG.filter {
                it.name.contains(query, ignoreCase = true) ||
                (it.genres?.any { g -> g.contains(query, ignoreCase = true) } == true)
            }
        }
    }

    suspend fun getMetaDetail(type: String, id: String): StremioMetaDetail = withContext(Dispatchers.IO) {
        val cinemetaUrl = "https://v3-cinemeta.strem.io"
        val result = apiClient.fetchMetaDetail(cinemetaUrl, type, id)
        if (result.isSuccess && result.getOrNull() != null) {
            result.getOrThrow()
        } else {
            DefaultAddons.getFallbackDetail(id)
        }
    }
}
