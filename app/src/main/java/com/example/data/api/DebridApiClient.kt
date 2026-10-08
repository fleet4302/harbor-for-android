package com.example.data.api

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class DebridAccountInfo(
    val service: String,
    val username: String,
    val email: String?,
    val isPremium: Boolean,
    val expiration: String?,
    val daysRemaining: Int?
)

class DebridApiClient {

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    /**
     * Validates a Real-Debrid API key by calling /rest/1.0/user
     */
    suspend fun validateRealDebridKey(token: String): Result<DebridAccountInfo> = withContext(Dispatchers.IO) {
        val cleanToken = token.trim()
        if (cleanToken.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Real-Debrid API token cannot be empty"))
        }

        try {
            val request = Request.Builder()
                .url("https://api.real-debrid.com/rest/1.0/user")
                .header("Authorization", "Bearer $cleanToken")
                .header("Accept", "application/json")
                .build()

            client.newCall(request).execute().use { response ->
                val body = response.body?.string() ?: ""
                if (response.code == 401 || response.code == 403) {
                    val errMsg = try {
                        val json = JSONObject(body)
                        json.optString("error", "Invalid or expired token")
                    } catch (e: Exception) {
                        "Invalid API token (HTTP ${response.code})"
                    }
                    return@withContext Result.failure(Exception("Real-Debrid rejected key: $errMsg. Verify at real-debrid.com/apitoken"))
                }

                if (!response.isSuccessful) {
                    return@withContext Result.failure(Exception("Real-Debrid API returned HTTP ${response.code}: $body"))
                }

                val json = JSONObject(body)
                val username = json.optString("username", "Real-Debrid User")
                val email = if (json.has("email") && !json.isNull("email")) json.optString("email") else null
                val type = json.optString("type", "standard")
                val premiumSeconds = json.optLong("premium", 0L)
                val expiration = if (json.has("expiration") && !json.isNull("expiration")) json.optString("expiration") else null
                val isPremium = type.equals("premium", ignoreCase = true) || premiumSeconds > 0
                val daysRemaining = if (premiumSeconds > 0) (premiumSeconds / 86400).toInt() else null

                Result.success(
                    DebridAccountInfo(
                        service = "Real-Debrid",
                        username = username,
                        email = email,
                        isPremium = isPremium,
                        expiration = expiration,
                        daysRemaining = daysRemaining
                    )
                )
            }
        } catch (e: Exception) {
            Log.e("DebridApiClient", "Error validating Real-Debrid token", e)
            Result.failure(Exception("Connection to Real-Debrid failed: ${e.message}"))
        }
    }

    /**
     * Validates a TorBox API key by calling /v1/api/user/me
     */
    suspend fun validateTorBoxKey(token: String): Result<DebridAccountInfo> = withContext(Dispatchers.IO) {
        val cleanToken = token.trim()
        if (cleanToken.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("TorBox API token cannot be empty"))
        }

        try {
            val request = Request.Builder()
                .url("https://api.torbox.app/v1/api/user/me")
                .header("Authorization", "Bearer $cleanToken")
                .header("Accept", "application/json")
                .build()

            client.newCall(request).execute().use { response ->
                val body = response.body?.string() ?: ""
                if (!response.isSuccessful) {
                    return@withContext Result.failure(Exception("TorBox key validation failed (HTTP ${response.code})"))
                }

                val json = JSONObject(body)
                val data = json.optJSONObject("data") ?: json
                val email = data.optString("email", "TorBox User")
                val plan = data.optInt("plan", 0)

                Result.success(
                    DebridAccountInfo(
                        service = "TorBox",
                        username = email.substringBefore("@"),
                        email = email,
                        isPremium = plan > 0,
                        expiration = null,
                        daysRemaining = null
                    )
                )
            }
        } catch (e: Exception) {
            Result.failure(Exception("Connection to TorBox failed: ${e.message}"))
        }
    }

    /**
     * General validator based on service name
     */
    suspend fun validateKey(service: String, token: String): Result<DebridAccountInfo> {
        return when (service.lowercase()) {
            "torbox" -> validateTorBoxKey(token)
            else -> validateRealDebridKey(token)
        }
    }

    /**
     * Resolves a magnet or torrent infoHash to a direct HTTP stream URL via Real-Debrid API.
     */
    suspend fun resolveMagnetViaRealDebrid(token: String, magnetUri: String): Result<String> = withContext(Dispatchers.IO) {
        val cleanToken = token.trim()
        try {
            // 1. Add magnet
            val addBody = FormBody.Builder()
                .add("magnet", magnetUri)
                .build()

            val addReq = Request.Builder()
                .url("https://api.real-debrid.com/rest/1.0/torrents/addMagnet")
                .header("Authorization", "Bearer $cleanToken")
                .post(addBody)
                .build()

            val torrentId = client.newCall(addReq).execute().use { resp ->
                val body = resp.body?.string() ?: ""
                if (!resp.isSuccessful) {
                    return@withContext Result.failure(Exception("Failed to add magnet to Real-Debrid (HTTP ${resp.code})"))
                }
                val json = JSONObject(body)
                json.optString("id", "")
            }

            if (torrentId.isBlank()) {
                return@withContext Result.failure(Exception("Empty torrent ID returned from Real-Debrid"))
            }

            // 2. Select files (all files)
            val selBody = FormBody.Builder()
                .add("files", "all")
                .build()

            val selReq = Request.Builder()
                .url("https://api.real-debrid.com/rest/1.0/torrents/selectFiles/$torrentId")
                .header("Authorization", "Bearer $cleanToken")
                .post(selBody)
                .build()

            client.newCall(selReq).execute().close()

            // 3. Check status & links
            val infoReq = Request.Builder()
                .url("https://api.real-debrid.com/rest/1.0/torrents/info/$torrentId")
                .header("Authorization", "Bearer $cleanToken")
                .build()

            val debridLink = client.newCall(infoReq).execute().use { resp ->
                val body = resp.body?.string() ?: ""
                val json = JSONObject(body)
                val status = json.optString("status", "")
                val linksArr = json.optJSONArray("links")
                if (linksArr != null && linksArr.length() > 0) {
                    linksArr.getString(0)
                } else {
                    if (status == "downloading") {
                        val progress = json.optDouble("progress", 0.0)
                        return@withContext Result.failure(Exception("Torrent downloading to Real-Debrid cache (${progress.toInt()}%)."))
                    }
                    return@withContext Result.failure(Exception("No links ready in Real-Debrid (status: $status)"))
                }
            }

            // 4. Unrestrict link
            val unresBody = FormBody.Builder()
                .add("link", debridLink)
                .build()

            val unresReq = Request.Builder()
                .url("https://api.real-debrid.com/rest/1.0/unrestrict/link")
                .header("Authorization", "Bearer $cleanToken")
                .post(unresBody)
                .build()

            client.newCall(unresReq).execute().use { resp ->
                val body = resp.body?.string() ?: ""
                if (!resp.isSuccessful) {
                    return@withContext Result.failure(Exception("Failed to unrestrict link (HTTP ${resp.code})"))
                }
                val json = JSONObject(body)
                val downloadUrl = json.optString("download", "")
                if (downloadUrl.isNotBlank()) {
                    Result.success(downloadUrl)
                } else {
                    Result.failure(Exception("No direct download link returned"))
                }
            }
        } catch (e: Exception) {
            Log.e("DebridApiClient", "Error resolving magnet on Real-Debrid", e)
            Result.failure(e)
        }
    }
}
