package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class StremioManifest(
    val id: String,
    val name: String,
    val version: String? = "1.0.0",
    val description: String? = null,
    val logo: String? = null,
    val icon: String? = null,
    val background: String? = null,
    val types: List<String>? = listOf("movie", "series"),
    val resources: List<Any>? = null,
    val catalogs: List<StremioCatalogDesc>? = emptyList(),
    val idPrefixes: List<String>? = null,
    val behaviorHints: Map<String, Any>? = null
)

@JsonClass(generateAdapter = true)
data class StremioCatalogDesc(
    val type: String,
    val id: String,
    val name: String,
    val pageSize: Int? = 20,
    val extra: List<StremioCatalogExtra>? = null,
    val extraSupported: List<String>? = null,
    val extraRequired: List<String>? = null
)

@JsonClass(generateAdapter = true)
data class StremioCatalogExtra(
    val name: String,
    val isRequired: Boolean? = false,
    val options: List<String>? = null
)

@JsonClass(generateAdapter = true)
data class StremioCatalogResponse(
    val metas: List<StremioMetaSummary> = emptyList()
)

@JsonClass(generateAdapter = true)
data class StremioMetaSummary(
    val id: String,
    val type: String,
    val name: String,
    val poster: String? = null,
    val banner: String? = null,
    val background: String? = null,
    val logo: String? = null,
    val genres: List<String>? = emptyList(),
    val releaseInfo: String? = null,
    val imdbRating: String? = null,
    val description: String? = null
)

@JsonClass(generateAdapter = true)
data class StremioMetaDetailResponse(
    val meta: StremioMetaDetail? = null
)

@JsonClass(generateAdapter = true)
data class StremioMetaDetail(
    val id: String,
    val type: String,
    val name: String,
    val poster: String? = null,
    val background: String? = null,
    val logo: String? = null,
    val description: String? = null,
    val releaseInfo: String? = null,
    val imdbRating: String? = null,
    val runtime: String? = null,
    val genres: List<String>? = emptyList(),
    val cast: List<String>? = emptyList(),
    val director: List<String>? = emptyList(),
    val trailer: String? = null,
    val videos: List<StremioVideo>? = emptyList()
)

@JsonClass(generateAdapter = true)
data class StremioVideo(
    val id: String,
    val title: String? = null,
    val season: Int? = null,
    val episode: Int? = null,
    val released: String? = null,
    val thumbnail: String? = null,
    val overview: String? = null,
    val stream: StremioStreamItem? = null
)

@JsonClass(generateAdapter = true)
data class StremioStreamResponse(
    val streams: List<StremioStreamItem> = emptyList()
)

@JsonClass(generateAdapter = true)
data class StremioStreamItem(
    val name: String? = null,
    val title: String? = null,
    val description: String? = null,
    val url: String? = null,
    val infoHash: String? = null,
    val fileIdx: Int? = null,
    val externalUrl: String? = null,
    val ytId: String? = null,
    val behaviorHints: Map<String, Any>? = null
)

@JsonClass(generateAdapter = true)
data class StremioSubtitlesResponse(
    val subtitles: List<StremioSubtitleItem> = emptyList()
)

@JsonClass(generateAdapter = true)
data class StremioSubtitleItem(
    val id: String,
    val url: String,
    val lang: String
)
