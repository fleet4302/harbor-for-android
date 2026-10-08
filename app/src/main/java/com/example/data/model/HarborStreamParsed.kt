package com.example.data.model

import java.util.Locale
import java.util.regex.Pattern

enum class StreamResolution(val label: String, val rank: Int) {
    RES_4K("4K UHD", 100),
    RES_1080P("1080p", 80),
    RES_720P("720p", 60),
    RES_480P("480p", 40),
    RES_CAM("CAM / TS", 10),
    RES_UNKNOWN("HD", 50)
}

data class HarborParsedStream(
    val rawStream: StremioStreamItem,
    val addonName: String,
    val resolution: StreamResolution,
    val resolutionBadge: String,
    val isHdr: Boolean,
    val isDolbyVision: Boolean,
    val isDebrid: Boolean,
    val debridProvider: String?,
    val codec: String?,
    val audioChannels: String?,
    val seeders: Int?,
    val fileSizeFormatted: String?,
    val releaseGroup: String?,
    val displayTitle: String,
    val displaySubtitle: String,
    val harborScore: Int
)

object HarborStreamParser {

    private val SEEDERS_PATTERN = Pattern.compile("(?:👤|seeds|seeders|S:)[\\s:]*(\\d+)", Pattern.CASE_INSENSITIVE)
    private val SIZE_PATTERN = Pattern.compile("([0-9]+(?:\\.[0-9]+)?\\s*(?:GB|MB|GiB|MiB))", Pattern.CASE_INSENSITIVE)

    fun parse(stream: StremioStreamItem, addonName: String): HarborParsedStream {
        val name = stream.name ?: ""
        val title = stream.title ?: stream.description ?: ""
        val combined = "$name $title"
        val lower = combined.lowercase(Locale.ROOT)

        // Resolution detection
        val (res, resBadge) = when {
            lower.contains("4k") || lower.contains("2160p") || lower.contains("uhd") ->
                Pair(StreamResolution.RES_4K, "4K UHD")
            lower.contains("1080p") || lower.contains("fhd") ->
                Pair(StreamResolution.RES_1080P, "1080p")
            lower.contains("720p") || lower.contains("hd") && !lower.contains("fullhd") ->
                Pair(StreamResolution.RES_720P, "720p")
            lower.contains("480p") || lower.contains("sd") ->
                Pair(StreamResolution.RES_480P, "480p")
            lower.contains("cam") || lower.contains("telesync") || lower.contains("hdcam") ->
                Pair(StreamResolution.RES_CAM, "CAM")
            else -> Pair(StreamResolution.RES_1080P, "1080p")
        }

        // HDR & Dolby Vision
        val isDv = lower.contains("dv") || lower.contains("dovi") || lower.contains("dolby vision")
        val isHdr = lower.contains("hdr") || lower.contains("hdr10") || lower.contains("hdr10+") || isDv

        // Debrid detection
        val (isDebrid, debridProvider) = when {
            lower.contains("[rd+]") || lower.contains("realdebrid") || lower.contains("rd+") ->
                Pair(true, "Real-Debrid")
            lower.contains("[ad+]") || lower.contains("alldebrid") || lower.contains("ad+") ->
                Pair(true, "AllDebrid")
            lower.contains("[tb+]") || lower.contains("torbox") || lower.contains("tb+") ->
                Pair(true, "TorBox")
            lower.contains("[pm+]") || lower.contains("premiumize") ->
                Pair(true, "Premiumize")
            lower.contains("[debrid]") ->
                Pair(true, "Debrid")
            else -> Pair(false, null)
        }

        // Codec
        val codec = when {
            lower.contains("hevc") || lower.contains("x265") || lower.contains("h.265") || lower.contains("h265") -> "HEVC (x265)"
            lower.contains("av1") -> "AV1"
            lower.contains("x264") || lower.contains("h.264") || lower.contains("h264") || lower.contains("avc") -> "AVC (x264)"
            else -> "x264"
        }

        // Audio
        val audio = when {
            lower.contains("atmos") -> "Dolby Atmos"
            lower.contains("truehd") -> "Dolby TrueHD"
            lower.contains("dts-hd") || lower.contains("dts-x") -> "DTS-HD"
            lower.contains("ddp5.1") || lower.contains("dovi 5.1") || lower.contains("5.1") -> "5.1 Surround"
            lower.contains("7.1") -> "7.1 Surround"
            else -> "Stereo"
        }

        // Seeders
        var seeders: Int? = null
        val seedMatcher = SEEDERS_PATTERN.matcher(combined)
        if (seedMatcher.find()) {
            seeders = seedMatcher.group(1)?.toIntOrNull()
        }

        // File size
        var fileSizeFormatted: String? = null
        val sizeMatcher = SIZE_PATTERN.matcher(combined)
        if (sizeMatcher.find()) {
            fileSizeFormatted = sizeMatcher.group(1)
        }

        // Display labels
        val displayTitle = if (name.isNotBlank()) {
            name.lines().firstOrNull()?.trim() ?: "Stream"
        } else {
            "Stream ($resBadge)"
        }

        val displaySubtitle = if (title.isNotBlank()) {
            title.lines().firstOrNull()?.trim() ?: title
        } else {
            "$resBadge • $codec • $audio"
        }

        // Harbor Score ranking algorithm
        var score = res.rank
        if (isDebrid) score += 50
        if (isHdr) score += 15
        if (isDv) score += 10
        if (audio.contains("Atmos") || audio.contains("TrueHD")) score += 10
        if (seeders != null) score += (seeders / 10).coerceAtMost(30)

        return HarborParsedStream(
            rawStream = stream,
            addonName = addonName,
            resolution = res,
            resolutionBadge = resBadge,
            isHdr = isHdr,
            isDolbyVision = isDv,
            isDebrid = isDebrid,
            debridProvider = debridProvider,
            codec = codec,
            audioChannels = audio,
            seeders = seeders,
            fileSizeFormatted = fileSizeFormatted,
            releaseGroup = null,
            displayTitle = displayTitle,
            displaySubtitle = displaySubtitle,
            harborScore = score
        )
    }
}
