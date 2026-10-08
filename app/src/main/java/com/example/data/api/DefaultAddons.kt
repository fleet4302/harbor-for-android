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
            id = "community.torrentio",
            manifestUrl = "https://torrentio.strem.fun/manifest.json",
            name = "Torrentio",
            version = "1.0.13",
            description = "High-speed streams scraper with Real-Debrid, TorBox, and AllDebrid support.",
            iconUrl = null,
            isEnabled = true,
            isOfficial = false,
            orderIndex = 2,
            supportsCatalog = false,
            supportsStream = true,
            supportsSubtitles = false
        ),
        AddonEntity(
            id = "community.watchhub",
            manifestUrl = "https://watchhub.strem.io/manifest.json",
            name = "WatchHub",
            version = "1.0.3",
            description = "Official streams and VOD services (Netflix, Prime, Disney+, HBO Max, Apple TV).",
            iconUrl = null,
            isEnabled = true,
            isOfficial = true,
            orderIndex = 3,
            supportsCatalog = false,
            supportsStream = true,
            supportsSubtitles = false
        ),
        AddonEntity(
            id = "community.animekitsu",
            manifestUrl = "https://anime-kitsu.strem.fun/manifest.json",
            name = "Anime Kitsu",
            version = "2.0.4",
            description = "Anime series, movies, and trending catalogs powered by Kitsu.io.",
            iconUrl = null,
            isEnabled = true,
            isOfficial = false,
            orderIndex = 4,
            supportsCatalog = true,
            supportsStream = true,
            supportsSubtitles = false
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
            orderIndex = 5,
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

    // Curated fallback showcase catalog (high quality metadata + playable test stream links)
    val FALLBACK_CATALOG = listOf(
        StremioMetaSummary(
            id = "harbor_dune2",
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
            id = "harbor_arcane",
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
            id = "harbor_oppenheimer",
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
            id = "harbor_cyberpunk",
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
            id = "harbor_interstellar",
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
            id = "harbor_bbb",
            type = "movie",
            name = "Big Buck Bunny (4K Remaster)",
            poster = "https://images.unsplash.com/photo-1579783900882-c0d3dad7b119?w=800&q=80",
            background = "https://images.unsplash.com/photo-1470240731273-7821a6eeb6bd?w=1600&q=80",
            genres = listOf("Animation", "Comedy", "Short"),
            releaseInfo = "2008",
            imdbRating = "7.8",
            description = "A large and lovable rabbit deals with bullying forest creatures in this iconic open-source benchmark animated film."
        ),
        StremioMetaSummary(
            id = "harbor_tears_of_steel",
            type = "movie",
            name = "Tears of Steel (Sci-Fi VFX)",
            poster = "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?w=800&q=80",
            background = "https://images.unsplash.com/photo-1534447677768-be436bb09401?w=1600&q=80",
            genres = listOf("Sci-Fi", "Action", "Short"),
            releaseInfo = "2012",
            imdbRating = "7.2",
            description = "Set in a dystopian future in Amsterdam, a group of scientists and warriors attempt to save the earth from destructive robotic giants."
        ),
        StremioMetaSummary(
            id = "harbor_sintel",
            type = "movie",
            name = "Sintel",
            poster = "https://images.unsplash.com/photo-1514565131-fce0801e5785?w=800&q=80",
            background = "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?w=1600&q=80",
            genres = listOf("Animation", "Fantasy", "Short"),
            releaseInfo = "2010",
            imdbRating = "7.5",
            description = "A lonely young woman searches the lands for a baby dragon she nursed to health, only to discover a heartbreaking truth."
        )
    )

    // Playable demo streams
    val SAMPLE_STREAMS = listOf(
        StremioStreamItem(
            name = "[RD+] Real-Debrid 4K",
            title = "Harbor Direct 4K UHD Remux | ⚙️ 2160p HDR10+ | 💾 18.4 GB | 👤 320",
            url = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4"
        ),
        StremioStreamItem(
            name = "[RD+] Real-Debrid 1080p",
            title = "Torrentio 1080p BluRay x265 | ⚙️ 1080p | 💾 4.2 GB | Dolby Atmos | 👤 145",
            url = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4"
        ),
        StremioStreamItem(
            name = "Harbor Fast Stream 1080p",
            title = "WEB-DL 1080p H264 AAC 5.1 | 💾 2.1 GB | 👤 89",
            url = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4"
        ),
        StremioStreamItem(
            name = "Harbor Mobile 720p",
            title = "720p HD Optimized Fast Stream | 💾 950 MB | 👤 54",
            url = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4"
        )
    )

    fun getFallbackDetail(id: String): StremioMetaDetail {
        val summary = FALLBACK_CATALOG.find { it.id == id } ?: FALLBACK_CATALOG.first()
        val isSeries = summary.type == "series"

        val episodes = if (isSeries) {
            listOf(
                StremioVideo(
                    id = "$id:1:1",
                    title = "Episode 1: The Golden Harbor",
                    season = 1,
                    episode = 1,
                    released = "2024-01-10",
                    thumbnail = summary.poster,
                    overview = "An unexpected voyage begins across the neon-lit horizon as tensions flare."
                ),
                StremioVideo(
                    id = "$id:1:2",
                    title = "Episode 2: Into the Depths",
                    season = 1,
                    episode = 2,
                    released = "2024-01-17",
                    thumbnail = summary.background,
                    overview = "The crew navigates perilous ocean currents and uncovers an ancient signal."
                ),
                StremioVideo(
                    id = "$id:1:3",
                    title = "Episode 3: The Beacon",
                    season = 1,
                    episode = 3,
                    released = "2024-01-24",
                    thumbnail = summary.poster,
                    overview = "Allies reunite at the harbor beacon as the storm reaches peak fury."
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
