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
                val loginResp = loginResponseAdapter.fromJson(body)
                if (loginResp?.result != null) {
                    Result.success(loginResp.result)
                } else {
                    val errorMsg = loginResp?.error ?: "Login failed (HTTP ${response.code})"
                    Result.failure(Exception(errorMsg))
                }
            }
        } catch (e: Exception) {
            Log.e("StremioApiClient", "Login error: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun getAddonCollection(authKey: String): Result<List<StremioSyncedAddon>> = withContext(Dispatchers.IO) {
        try {
            val jsonBody = addonCollectionRequestAdapter.toJson(StremioAddonCollectionRequest(authKey = authKey.trim()))
            val requestBody = jsonBody.toRequestBody("application/json; charset=utf-8".toMediaType())
            val request = Request.Builder()
                .url("https://api.strem.io/api/addonCollectionGet")
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                val body = response.body?.string() ?: ""
                val collectionResp = addonCollectionResponseAdapter.fromJson(body)
                val addons = collectionResp?.result?.addons ?: emptyList()
                Result.success(addons)
            }
        } catch (e: Exception) {
            Log.e("StremioApiClient", "AddonCollection error: ${e.message}", e)
            Result.failure(e)
        }
    }
}
