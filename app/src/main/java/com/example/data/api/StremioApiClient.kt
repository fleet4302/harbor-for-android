package com.example.data.api

import android.util.Log
import com.example.data.model.StremioAddonCollectionRequest
import com.example.data.model.StremioAddonCollectionResponse
import com.example.data.model.StremioCatalogResponse
import com.example.data.model.StremioLoginRequest
import com.example.data.model.StremioLoginResponse
import com.example.data.model.StremioLoginResult
import com.example.data.model.StremioManifest
import com.example.data.model.StremioMetaDetail
import com.example.data.model.StremioMetaDetailResponse
import com.example.data.model.StremioMetaSummary
import com.example.data.model.StremioStreamItem
import com.example.data.model.StremioStreamResponse
import com.example.data.model.StremioSubtitlesResponse
import com.example.data.model.StremioSyncedAddon
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

class StremioApiClient {

    private val client = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .followRedirects(true)
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .header("User-Agent", "Harbor-Stremio-Android/1.0.0")
                .header("Accept", "application/json")
                .build()
            chain.proceed(request)
        }
        .build()

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val manifestAdapter = moshi.adapter(StremioManifest::class.java)
    private val catalogAdapter = moshi.adapter(StremioCatalogResponse::class.java)
    private val metaAdapter = moshi.adapter(StremioMetaDetailResponse::class.java)
    private val streamAdapter = moshi.adapter(StremioStreamResponse::class.java)
    private val subtitlesAdapter = moshi.adapter(StremioSubtitlesResponse::class.java)
    private val loginRequestAdapter = moshi.adapter(StremioLoginRequest::class.java)
    private val loginResponseAdapter = moshi.adapter(StremioLoginResponse::class.java)
    private val addonCollectionRequestAdapter = moshi.adapter(StremioAddonCollectionRequest::class.java)
    private val addonCollectionResponseAdapter = moshi.adapter(StremioAddonCollectionResponse::class.java)

    /**
     * Sanitizes addon URL to base URL without /manifest.json
     */
    private fun sanitizeBaseUrl(url: String): String {
        var clean = url.trim()
        if (clean.startsWith("stremio://")) {
            clean = "https://" + clean.removePrefix("stremio://")
        }
        return clean.removeSuffix("/manifest.json").removeSuffix("/")
    }

    suspend fun fetchManifest(url: String): Result<StremioManifest> = withContext(Dispatchers.IO) {
        try {
            val base = sanitizeBaseUrl(url)
            val manifestUrl = "$base/manifest.json"
            val request = Request.Builder().url(manifestUrl).build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.failure(Exception("HTTP ${response.code} fetching manifest"))
                }
                val body = response.body?.string() ?: return@withContext Result.failure(Exception("Empty body"))
                val manifest = manifestAdapter.fromJson(body)
                    ?: return@withContext Result.failure(Exception("Could not parse manifest"))
                Result.success(manifest)
            }
        } catch (e: Exception) {
            Log.w("StremioApiClient", "Error fetching manifest from $url", e)
            Result.failure(e)
        }
    }

    suspend fun fetchCatalog(
        addonUrl: String,
        type: String,
        id: String,
        extraParams: Map<String, String> = emptyMap()
    ): Result<List<StremioMetaSummary>> = withContext(Dispatchers.IO) {
        try {
            val base = sanitizeBaseUrl(addonUrl)
            val extraPath = if (extraParams.isNotEmpty()) {
                val formatted = extraParams.entries.joinToString("&") { "${it.key}=${it.value}" }
                "/$formatted"
            } else ""

            val requestUrl = "$base/catalog/$type/$id$extraPath.json"
            val request = Request.Builder().url(requestUrl).build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.failure(Exception("HTTP ${response.code}"))
                }
                val body = response.body?.string() ?: ""
                val catalogResp = catalogAdapter.fromJson(body)
                val metas = catalogResp?.metas ?: emptyList()
                Result.success(metas)
            }
        } catch (e: Exception) {
            Log.w("StremioApiClient", "Error fetching catalog from $addonUrl: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun fetchMetaDetail(
        addonUrl: String,
        type: String,
        id: String
    ): Result<StremioMetaDetail> = withContext(Dispatchers.IO) {
        try {
            val base = sanitizeBaseUrl(addonUrl)
            val requestUrl = "$base/meta/$type/$id.json"
            val request = Request.Builder().url(requestUrl).build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.failure(Exception("HTTP ${response.code}"))
                }
                val body = response.body?.string() ?: ""
                val metaResp = metaAdapter.fromJson(body)
                if (metaResp?.meta != null) {
                    Result.success(metaResp.meta)
                } else {
                    Result.failure(Exception("Meta detail null in response"))
                }
            }
        } catch (e: Exception) {
            Log.w("StremioApiClient", "Error fetching meta detail for $id: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun fetchStreams(
        addonUrl: String,
        type: String,
        id: String
    ): Result<List<StremioStreamItem>> = withContext(Dispatchers.IO) {
        try {
            val base = sanitizeBaseUrl(addonUrl)
            val requestUrl = "$base/stream/$type/$id.json"
            val request = Request.Builder().url(requestUrl).build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.failure(Exception("HTTP ${response.code}"))
                }
                val body = response.body?.string() ?: ""
                val streamResp = streamAdapter.fromJson(body)
                val rawStreams = streamResp?.streams ?: emptyList()

                // Convert infoHash torrents without direct HTTP URL to magnet URIs
                val processed = rawStreams.map { item ->
                    if (item.url.isNullOrBlank() && !item.infoHash.isNullOrBlank()) {
                        val titleEnc = try {
                            URLEncoder.encode(item.title ?: item.name ?: "Torrent", "UTF-8")
                        } catch (e: Exception) {
                            "Torrent"
                        }
                        item.copy(url = "magnet:?xt=urn:btih:${item.infoHash}&dn=$titleEnc")
                    } else {
                        item
                    }
                }
                Result.success(processed)
            }
        } catch (e: Exception) {
            Log.w("StremioApiClient", "Error fetching streams from $addonUrl: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun login(email: String, password: String): Result<StremioLoginResult> = withContext(Dispatchers.IO) {
        try {
            val jsonBody = loginRequestAdapter.toJson(StremioLoginRequest(email = email.trim(), password = password))
            val requestBody = jsonBody.toRequestBody("application/json; charset=utf-8".toMediaType())
            val request = Request.Builder()
                .url("https://api.strem.io/api/login")
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                val body = response.body?.string() ?: ""
                if (body.isBlank()) {
                    return@withContext Result.failure(Exception("Empty response from Stremio (HTTP ${response.code})"))
                }

                val json = try {
                    org.json.JSONObject(body)
                } catch (e: Exception) {
                    return@withContext Result.failure(Exception("Failed to parse Stremio response: ${e.message}"))
                }

                // Handle error field dynamically (can be String or JSONObject)
                if (json.has("error") && !json.isNull("error")) {
                    val errorMsg = when (val err = json.opt("error")) {
                        is org.json.JSONObject -> err.optString("message", err.optString("description", err.toString()))
                        is String -> err
                        else -> err?.toString() ?: "Unknown error"
                    }
                    return@withContext Result.failure(Exception(errorMsg))
                }

                // Handle result field dynamically (can be JSONObject with authKey/user or authKey string)
                if (json.has("result") && !json.isNull("result")) {
                    when (val res = json.opt("result")) {
                        is org.json.JSONObject -> {
                            val authKey = res.optString("authKey", "")
                            if (authKey.isBlank()) {
                                return@withContext Result.failure(Exception("No authKey in Stremio result"))
                            }
                            val userObj = res.optJSONObject("user")
                            val user = if (userObj != null) {
                                com.example.data.model.StremioUser(
                                    _id = userObj.optString("_id", userObj.optString("id", null)),
                                    email = userObj.optString("email", email.trim())
                                )
                            } else {
                                com.example.data.model.StremioUser(email = email.trim())
                            }
                            Result.success(StremioLoginResult(authKey = authKey, user = user))
                        }
                        is String -> {
                            Result.success(StremioLoginResult(authKey = res, user = com.example.data.model.StremioUser(email = email.trim())))
                        }
                        else -> {
                            Result.failure(Exception("Unexpected login result structure"))
                        }
                    }
                } else {
                    Result.failure(Exception("Login failed (HTTP ${response.code})"))
                }
            }
        } catch (e: Exception) {
            Log.e("StremioApiClient", "Login error: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun getAddonCollection(authKey: String): Result<List<StremioSyncedAddon>> = withContext(Dispatchers.IO) {
        try {
            val jsonBody = org.json.JSONObject().apply {
                put("type", "AddonCollectionGet")
                put("authKey", authKey.trim())
            }.toString()
            val requestBody = jsonBody.toRequestBody("application/json; charset=utf-8".toMediaType())
            val request = Request.Builder()
                .url("https://api.strem.io/api/addonCollectionGet")
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                val body = response.body?.string() ?: ""
                if (body.isBlank()) {
                    return@withContext Result.failure(Exception("Empty response from Stremio (HTTP ${response.code})"))
                }

                val json = try {
                    org.json.JSONObject(body)
                } catch (e: Exception) {
                    return@withContext Result.failure(Exception("Could not parse JSON response from Stremio"))
                }

                if (json.has("error") && !json.isNull("error")) {
                    val errorMsg = when (val err = json.opt("error")) {
                        is org.json.JSONObject -> err.optString("message", err.toString())
                        is String -> err
                        else -> err?.toString() ?: "AuthKey error"
                    }
                    return@withContext Result.failure(Exception(errorMsg))
                }

                // Resilient parsing: handles both nested result.addons and direct array
                val addonsArray = when {
                    json.has("result") && json.optJSONObject("result")?.has("addons") == true -> {
                        json.optJSONObject("result")?.optJSONArray("addons")
                    }
                    json.has("result") && json.optJSONArray("result") != null -> {
                        json.optJSONArray("result")
                    }
                    json.has("addons") -> {
                        json.optJSONArray("addons")
                    }
                    else -> null
                }

                val list = mutableListOf<StremioSyncedAddon>()
                if (addonsArray != null) {
                    for (i in 0 until addonsArray.length()) {
                        val itemObj = addonsArray.optJSONObject(i) ?: continue
                        val manifestObj = itemObj.optJSONObject("manifest") ?: itemObj
                        val id = manifestObj.optString("id", itemObj.optString("id", ""))
                        val name = manifestObj.optString("name", itemObj.optString("name", id))
                        var transportUrl = itemObj.optString("transportUrl",
                            itemObj.optString("manifestUrl",
                                itemObj.optString("url",
                                    manifestObj.optString("transportUrl",
                                        manifestObj.optString("manifestUrl", "")
                                    )
                                )
                            )
                        )
                        if (transportUrl.isBlank() && id.isNotBlank()) {
                            // Known defaults
                            transportUrl = when (id) {
                                "community.torrentio" -> "https://torrentio.strem.fun/manifest.json"
                                "community.cinemeta" -> "https://v3-cinemeta.strem.io/manifest.json"
                                "org.stremio.opensubtitles" -> "https://opensubtitles-v3.strem.io/manifest.json"
                                else -> ""
                            }
                        }

                        if (id.isNotBlank() && transportUrl.isNotBlank()) {
                            val desc = manifestObj.optString("description", "")
                            val version = manifestObj.optString("version", "1.0.0")
                            val logo = if (manifestObj.has("logo")) manifestObj.optString("logo") else null
                            val icon = if (manifestObj.has("icon")) manifestObj.optString("icon") else null

                            val manifest = StremioManifest(
                                id = id,
                                name = name,
                                version = version,
                                description = desc,
                                logo = logo,
                                icon = icon
                            )
                            list.add(StremioSyncedAddon(transportUrl = transportUrl, manifest = manifest))
                        }
                    }
                }
                Result.success(list)
            }
        } catch (e: Exception) {
            Log.e("StremioApiClient", "AddonCollection error: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Syncs user's library and watch state from Stremio datastore
     */
    suspend fun getLibraryItems(authKey: String): Result<List<com.example.data.model.StremioLibraryEntry>> = withContext(Dispatchers.IO) {
        try {
            val collectionsToFetch = listOf("libraryItem", "watchState", "watched")
            val resultMap = mutableMapOf<String, com.example.data.model.StremioLibraryEntry>()

            for (col in collectionsToFetch) {
                try {
                    val jsonBody = org.json.JSONObject().apply {
                        put("authKey", authKey.trim())
                        put("collection", col)
                    }.toString()
                    val requestBody = jsonBody.toRequestBody("application/json; charset=utf-8".toMediaType())
                    val request = Request.Builder()
                        .url("https://api.strem.io/api/datastoreGet")
                        .post(requestBody)
                        .build()

                    client.newCall(request).execute().use { response ->
                        val body = response.body?.string() ?: ""
                        if (body.isBlank()) return@use

                        val json = try {
                            org.json.JSONObject(body)
                        } catch (e: Exception) {
                            return@use
                        }

                        val rawItemsList = mutableListOf<org.json.JSONObject>()

                        if (json.has("result") && !json.isNull("result")) {
                            val resVal = json.opt("result")
                            if (resVal is org.json.JSONArray) {
                                for (i in 0 until resVal.length()) {
                                    val obj = resVal.optJSONObject(i)
                                    if (obj != null) rawItemsList.add(obj)
                                }
                            } else if (resVal is org.json.JSONObject) {
                                if (resVal.has("items") && resVal.optJSONArray("items") != null) {
                                    val arr = resVal.optJSONArray("items")!!
                                    for (i in 0 until arr.length()) {
                                        val obj = arr.optJSONObject(i)
                                        if (obj != null) rawItemsList.add(obj)
                                    }
                                } else {
                                    val keys = resVal.keys()
                                    while (keys.hasNext()) {
                                        val k = keys.next()
                                        val obj = resVal.optJSONObject(k)
                                        if (obj != null) {
                                            if (!obj.has("_id") && !obj.has("id")) {
                                                obj.put("_id", k)
                                            }
                                            rawItemsList.add(obj)
                                        }
                                    }
                                }
                            }
                        }

                        for (itemObj in rawItemsList) {
                            val rawId = itemObj.optString("_id", itemObj.optString("id", itemObj.optString("key", "")))
                            if (rawId.isBlank()) continue

                            val baseId = if (rawId.startsWith("tt") && rawId.contains(":")) rawId.substringBefore(":") else rawId
                            val name = itemObj.optString("name", itemObj.optString("title", "Untitled"))
                            val type = itemObj.optString("type", if (rawId.contains(":")) "series" else "movie")
                            var poster = if (itemObj.has("poster")) itemObj.optString("poster") else null
                            val background = if (itemObj.has("background")) itemObj.optString("background") else null

                            if (poster.isNullOrBlank() && baseId.startsWith("tt")) {
                                poster = "https://images.metahub.space/poster/medium/$baseId/img"
                            }

                            var season: Int? = null
                            var episode: Int? = null
                            var posMs = 0L
                            var durMs = 0L
                            var lastWatched = System.currentTimeMillis()

                            if (itemObj.has("mtime")) {
                                val mtime = itemObj.optLong("mtime", 0L)
                                if (mtime > 0) lastWatched = mtime
                            }

                            val stateObj = itemObj.optJSONObject("state") ?: itemObj.optJSONObject("value")
                            if (stateObj != null) {
                                val rawPos = stateObj.optLong("timeOffset", stateObj.optLong("time", stateObj.optLong("position", 0L)))
                                val rawDur = stateObj.optLong("duration", 0L)
                                posMs = if (rawPos in 1..99999) rawPos * 1000L else rawPos
                                durMs = if (rawDur in 1..99999) rawDur * 1000L else if (rawDur == 0L && posMs > 0) 3600000L else rawDur

                                val videoId = stateObj.optString("video_id", stateObj.optString("videoId", stateObj.optString("video", "")))
                                if (videoId.contains(":")) {
                                    val parts = videoId.split(":")
                                    if (parts.size >= 3) {
                                        season = parts[1].toIntOrNull()
                                        episode = parts[2].toIntOrNull()
                                    }
                                }
                                if (season == null && stateObj.has("season")) {
                                    season = stateObj.optInt("season")
                                }
                                if (episode == null && stateObj.has("episode")) {
                                    episode = stateObj.optInt("episode")
                                }
                            }

                            val entryKey = if (season != null && episode != null) "$baseId:$season:$episode" else baseId
                            val existing = resultMap[entryKey]
                            val mergedPos = if (posMs > 0) posMs else (existing?.positionMs ?: 0L)
                            val mergedDur = if (durMs > 0) durMs else (existing?.durationMs ?: 0L)

                            resultMap[entryKey] = com.example.data.model.StremioLibraryEntry(
                                id = baseId,
                                name = if (name != "Untitled") name else (existing?.name ?: "Untitled"),
                                type = type,
                                poster = poster ?: existing?.poster,
                                background = background ?: existing?.background,
                                season = season ?: existing?.season,
                                episode = episode ?: existing?.episode,
                                positionMs = mergedPos,
                                durationMs = mergedDur,
                                lastWatchedTimestamp = lastWatched
                            )
                        }
                    }
                } catch (e: Exception) {
                    Log.e("StremioApiClient", "Error fetching datastore $col: ${e.message}")
                }
            }

            Result.success(resultMap.values.toList())
        } catch (e: Exception) {
            Log.e("StremioApiClient", "Error fetching library items: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Validates that a Torrentio manifest URL is reachable and well-formed
     */
    suspend fun validateTorrentioManifest(url: String): Result<StremioManifest> = withContext(Dispatchers.IO) {
        val clean = url.trim()
        if (clean.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Torrentio URL cannot be empty"))
        }
        val res = fetchManifest(clean)
        if (res.isSuccess) {
            val manifest = res.getOrThrow()
            if (manifest.id.contains("torrentio", ignoreCase = true) || manifest.name.contains("torrentio", ignoreCase = true)) {
                Result.success(manifest)
            } else {
                // Still allow other stream providers if they provide streams
                val supportsStreams = manifest.resources?.any { res ->
                    when (res) {
                        is String -> res == "stream"
                        is Map<*, *> -> res["name"] == "stream"
                        else -> false
                    }
                } ?: false
                if (supportsStreams) {
                    Result.success(manifest)
                } else {
                    Result.failure(Exception("Addon '${manifest.name}' does not provide video streams"))
                }
            }
        } else {
            Result.failure(res.exceptionOrNull() ?: Exception("Cannot connect to Torrentio manifest at $clean"))
        }
    }
}
