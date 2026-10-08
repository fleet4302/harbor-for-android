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

    suspend fun getPopularMovies(genre: String? = null, skip: Int = 0): List<StremioMetaSummary> = withContext(Dispatchers.IO) {
        val extra = mutableMapOf<String, String>()
        if (!genre.isNullOrBlank() && genre != "All") {
            extra["genre"] = genre
        }
        if (skip > 0) {
            extra["skip"] = skip.toString()
        }

        val cinemetaUrl = "https://v3-cinemeta.strem.io"
        val result = apiClient.fetchCatalog(cinemetaUrl, "movie", "top", extra)
        if (result.isSuccess && result.getOrNull()?.isNotEmpty() == true) {
            result.getOrThrow()
        } else {
            DefaultAddons.FALLBACK_CATALOG.filter { it.type == "movie" }
        }
    }

    suspend fun getPopularSeries(genre: String? = null, skip: Int = 0): List<StremioMetaSummary> = withContext(Dispatchers.IO) {
        val extra = mutableMapOf<String, String>()
        if (!genre.isNullOrBlank() && genre != "All") {
            extra["genre"] = genre
        }
        if (skip > 0) {
            extra["skip"] = skip.toString()
        }

        val cinemetaUrl = "https://v3-cinemeta.strem.io"
        val result = apiClient.fetchCatalog(cinemetaUrl, "series", "top", extra)
        if (result.isSuccess && result.getOrNull()?.isNotEmpty() == true) {
            result.getOrThrow()
        } else {
            DefaultAddons.FALLBACK_CATALOG.filter { it.type == "series" }
        }
    }

    suspend fun getCatalogPage(type: String, genre: String? = null, skip: Int = 0): List<StremioMetaSummary> = withContext(Dispatchers.IO) {
        if (type == "movie") {
            getPopularMovies(genre, skip)
        } else {
            getPopularSeries(genre, skip)
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
