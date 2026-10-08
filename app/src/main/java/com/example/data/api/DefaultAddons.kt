package com.example.data.api

import com.example.data.local.AddonEntity
import com.example.data.model.StremioMetaDetail
import com.example.data.model.StremioMetaSummary
import com.example.data.model.StremioStreamItem
import com.example.data.model.StremioVideo

data class CommunityAddonListing(
    val id: String,
    val name: String,
    val manifestUrl: String,
    val description: String,
    val category: String,
    val iconUrl: String?,
    val isDebridSupported: Boolean = false,
    val stars: Double = 4.8
)

object DefaultAddons {

    val INITIAL_ADDONS = listOf(
        AddonEntity(
            id = "community.cinemeta",
            manifestUrl = "https://v3-cinemeta.strem.io/manifest.json",
            name = "Cinemeta",
            version = "3.0.12",
            description = "Official Stremio movie & TV series metadata, IMDb ratings, and catalogs.",
            iconUrl = "https://v3-cinemeta.strem.io/favicon.ico",
            isEnabled = true,
            isOfficial = true,
            orderIndex = 0,
            supportsCatalog = true,
            supportsStream = false,
            supportsSubtitles = false
        ),
        AddonEntity(
            id = "org.stremio.opensubtitles",
            manifestUrl = "https://opensubtitles-v3.strem.io/manifest.json",
            name = "OpenSubtitles v3",
            version = "1.0.0",
            description = "Multi-language subtitles from OpenSubtitles.org for movies and TV episodes.",
            iconUrl = null,
            isEnabled = true,
            isOfficial = true,
            orderIndex = 1,
            supportsCatalog = false,
            supportsStream = false,
            supportsSubtitles = true
        ),
        AddonEntity(
            id = "community.cyberflix",
            manifestUrl = "https://cyberflix.elfhosted.com/manifest.json",
            name = "CyberFlix Catalog",
            version = "1.5.0",
            description = "Catalog organizer for Netflix, Apple TV+, HBO, Amazon Prime, and Hulu.",
            iconUrl = null,
            isEnabled = true,
            isOfficial = false,
            orderIndex = 2,
            supportsCatalog = true,
            supportsStream = false,
            supportsSubtitles = false
        )
    )

    val COMMUNITY_STORE = listOf(
        CommunityAddonListing(
            id = "community.torrentio",
            name = "Torrentio",
            manifestUrl = "https://torrentio.strem.fun/manifest.json",
            description = "Torrent and Debrid streams provider for Stremio. Supports Real-Debrid, AllDebrid, Premiumize, and TorBox.",
            category = "Streams",
            iconUrl = null,
            isDebridSupported = true,
            stars = 4.9
        ),
        CommunityAddonListing(
            id = "community.tmdb",
            name = "The Movie Database (TMDB)",
            manifestUrl = "https://tmdb-addon.elfhosted.com/manifest.json",
            description = "Rich metadata, posters, backdrops, cast, crew, and localized language translations.",
            category = "Catalogs",
            iconUrl = null,
            stars = 4.8
        ),
        CommunityAddonListing(
            id = "community.animekitsu",
            name = "Anime Kitsu",
            manifestUrl = "https://anime-kitsu.strem.fun/manifest.json",
            description = "Anime discovery, trending seasons, and episode tracking from Kitsu API.",
            category = "Anime",
            iconUrl = null,
            stars = 4.7
        ),
        CommunityAddonListing(
            id = "community.cyberflix",
            name = "CyberFlix Catalog",
            manifestUrl = "https://cyberflix.elfhosted.com/manifest.json",
            description = "Curated VOD streaming service carousels (Netflix, Disney+, Max, Hulu, Paramount+).",
            category = "Catalogs",
            iconUrl = null,
            stars = 4.8
        ),
        CommunityAddonListing(
            id = "org.stremio.opensubtitles",
            name = "OpenSubtitles v3",
            manifestUrl = "https://opensubtitles-v3.strem.io/manifest.json",
            description = "Find subtitles in over 75 languages synced to your exact video releases.",
            category = "Subtitles",
            iconUrl = null,
            stars = 4.9
        ),
        CommunityAddonListing(
            id = "community.publicdomain",
            name = "Public Domain Movies",
            manifestUrl = "https://watchhub.strem.io/manifest.json",
            description = "Classic cinema, vintage horror, noir, and public domain masterworks with instant streaming.",
            category = "Free Cinema",
            iconUrl = null,
            stars = 4.6
        )
    )

    // Curated fallback showcase catalog with real IMDb IDs matching Cinemeta & Torrentio
    val FALLBACK_CATALOG = listOf(
        StremioMetaSummary(
            id = "tt15239678",
            type = "movie",
            name = "Dune: Part Two",
            poster = "https://images.unsplash.com/photo-1534447677768-be436bb09401?w=800&q=80",
            background = "https://images.unsplash.com/photo-1509198397868-475647b2a1e5?w=1600&q=80",
            genres = listOf("Sci-Fi", "Adventure", "Drama"),
            releaseInfo = "2024",
            imdbRating = "8.6",
            description = "Paul Atreides unites with Chani and the Fremen while seeking revenge against the conspirators who destroyed his family."
        ),
        StremioMetaSummary(
            id = "tt11126994",
            type = "series",
            name = "Arcane",
            poster = "https://images.unsplash.com/photo-1578632767115-351597cf2477?w=800&q=80",
            background = "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?w=1600&q=80",
            genres = listOf("Animation", "Action", "Sci-Fi"),
            releaseInfo = "2021-2024",
            imdbRating = "9.0",
            description = "Set in the utopian region of Piltover and the oppressed underground of Zaun, the story follows the origins of two iconic champions."
        ),
        StremioMetaSummary(
            id = "tt15398776",
            type = "movie",
            name = "Oppenheimer",
            poster = "https://images.unsplash.com/photo-1440404653325-ab127d49abc1?w=800&q=80",
            background = "https://images.unsplash.com/photo-1451187580459-43490279c0fa?w=1600&q=80",
            genres = listOf("Biography", "Drama", "History"),
            releaseInfo = "2023",
            imdbRating = "8.9",
            description = "The story of American scientist J. Robert Oppenheimer and his role in the development of the atomic bomb."
        ),
        StremioMetaSummary(
            id = "tt12590266",
            type = "series",
            name = "Cyberpunk: Edgerunners",
            poster = "https://images.unsplash.com/photo-1563089145-599997674d42?w=800&q=80",
            background = "https://images.unsplash.com/photo-1508739773434-c26b3d09e071?w=1600&q=80",
            genres = listOf("Animation", "Action", "Cyberpunk"),
            releaseInfo = "2022",
            imdbRating = "8.3",
            description = "A street kid trying to survive in a technology and body modification-obsessed city of the future decides to stay alive by becoming an edgerunner."
        ),
        StremioMetaSummary(
            id = "tt0816692",
            type = "movie",
            name = "Interstellar",
            poster = "https://images.unsplash.com/photo-1451187580459-43490279c0fa?w=800&q=80",
            background = "https://images.unsplash.com/photo-1506703719100-a0f3a48c0f86?w=1600&q=80",
            genres = listOf("Sci-Fi", "Adventure", "Drama"),
            releaseInfo = "2014",
            imdbRating = "8.7",
            description = "When Earth becomes uninhabitable in the future, a farmer and ex-NASA pilot is tasked to pilot a spacecraft along with a team of researchers."
        ),
        StremioMetaSummary(
            id = "tt0903747",
            type = "series",
            name = "Breaking Bad",
            poster = "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?w=800&q=80",
            background = "https://images.unsplash.com/photo-1470240731273-7821a6eeb6bd?w=1600&q=80",
            genres = listOf("Crime", "Drama", "Thriller"),
            releaseInfo = "2008-2013",
            imdbRating = "9.5",
            description = "A chemistry teacher diagnosed with inoperable lung cancer turns to manufacturing and selling methamphetamine."
        )
    )

    fun getFallbackDetail(id: String): StremioMetaDetail {
        val summary = FALLBACK_CATALOG.find { it.id == id } ?: FALLBACK_CATALOG.first()
        val isSeries = summary.type == "series"

        val episodes = if (isSeries) {
            listOf(
                StremioVideo(
                    id = "$id:1:1",
                    title = "Episode 1",
                    season = 1,
                    episode = 1,
                    released = "2024-01-10",
                    thumbnail = summary.poster,
                    overview = "The introductory episode setting the stage for the journey."
                ),
                StremioVideo(
                    id = "$id:1:2",
                    title = "Episode 2",
                    season = 1,
                    episode = 2,
                    released = "2024-01-17",
                    thumbnail = summary.background,
                    overview = "The storyline deepens as challenges intensify."
                ),
                StremioVideo(
                    id = "$id:1:3",
                    title = "Episode 3",
                    season = 1,
                    episode = 3,
                    released = "2024-01-24",
                    thumbnail = summary.poster,
                    overview = "Climactic revelations unfold across the horizon."
                )
            )
        } else null

        return StremioMetaDetail(
            id = summary.id,
            type = summary.type,
            name = summary.name,
            poster = summary.poster,
            background = summary.background,
            description = summary.description,
            releaseInfo = summary.releaseInfo,
            imdbRating = summary.imdbRating,
            runtime = if (isSeries) "45 min/ep" else "2h 15m",
            genres = summary.genres,
            cast = listOf("Timothée Chalamet", "Zendaya", "Rebecca Ferguson", "Javier Bardem"),
            director = listOf("Denis Villeneuve"),
            videos = episodes
        )
    }
}
