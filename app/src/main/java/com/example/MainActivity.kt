package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.example.data.api.StremioApiClient
import com.example.data.local.HarborDatabase
import com.example.data.model.HarborThemeStyle
import com.example.data.repository.AddonRepository
import com.example.data.repository.CatalogRepository
import com.example.data.repository.StreamResolverRepository
import com.example.data.repository.VaultRepository
import com.example.ui.components.HarborBottomNav
import com.example.ui.components.HarborNavTab
import com.example.ui.components.HarborTopBar
import com.example.ui.screens.AddonsScreen
import com.example.ui.screens.DiscoverScreen
import com.example.ui.screens.HarborPlayerScreen
import com.example.ui.screens.MediaDetailScreen
import com.example.ui.screens.SearchScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.VaultScreen
import com.example.ui.theme.HarborTheme

sealed class HarborScreen {
    object Main : HarborScreen()
    data class MediaDetail(val type: String, val id: String) : HarborScreen()
    data class Player(
        val title: String,
        val streamUrl: String,
        val mediaId: String,
        val season: Int? = null,
        val episode: Int? = null,
        val episodeTitle: String? = null,
        val poster: String? = null,
        val background: String? = null
    ) : HarborScreen()
}

class MainActivity : ComponentActivity() {
    var isPlayingVideo: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            HarborApp()
        }
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (isPlayingVideo && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            try {
                val params = android.app.PictureInPictureParams.Builder()
                    .setAspectRatio(android.util.Rational(16, 9))
                    .build()
                enterPictureInPictureMode(params)
            } catch (e: Exception) {
                // Ignore if PiP failed
            }
        }
    }
}

@Composable
fun HarborApp() {
    val context = LocalContext.current
    var currentThemeStyle by remember { mutableStateOf(HarborThemeStyle.DEEP_HARBOR) }
    var currentNavTab by remember { mutableStateOf(HarborNavTab.DISCOVER) }
    var currentScreen by remember { mutableStateOf<HarborScreen>(HarborScreen.Main) }

    // Repositories initialization
    val database = remember { HarborDatabase.getInstance(context) }
    val apiClient = remember { StremioApiClient() }
    val stremioSession = remember { com.example.data.local.StremioAccountSession(context) }
    val addonRepository = remember { AddonRepository(context, database.addonDao(), apiClient) }
    val catalogRepository = remember { CatalogRepository(addonRepository, apiClient) }
    val streamResolver = remember { StreamResolverRepository(context, addonRepository, apiClient) }
    val vaultRepository = remember { VaultRepository(database.watchHistoryDao(), database.watchlistDao()) }

    HarborTheme(themeStyle = currentThemeStyle) {
        when (val screen = currentScreen) {
            is HarborScreen.Player -> {
                HarborPlayerScreen(
                    title = screen.title,
                    streamUrl = screen.streamUrl,
                    mediaId = screen.mediaId,
                    season = screen.season,
                    episode = screen.episode,
                    episodeTitle = screen.episodeTitle,
                    poster = screen.poster,
                    background = screen.background,
                    vaultRepository = vaultRepository,
                    catalogRepository = catalogRepository,
                    streamResolverRepository = streamResolver,
                    onBack = { currentScreen = HarborScreen.Main },
                    onSwitchStream = { newTitle, newUrl, newMediaId, newS, newE, newEpTitle, newPoster, newBg ->
                        currentScreen = HarborScreen.Player(
                            title = newTitle,
                            streamUrl = newUrl,
                            mediaId = newMediaId,
                            season = newS,
                            episode = newE,
                            episodeTitle = newEpTitle,
                            poster = newPoster,
                            background = newBg
                        )
                    }
                )
            }
            is HarborScreen.MediaDetail -> {
                MediaDetailScreen(
                    mediaType = screen.type,
                    mediaId = screen.id,
                    catalogRepository = catalogRepository,
                    streamResolverRepository = streamResolver,
                    vaultRepository = vaultRepository,
                    stremioSession = stremioSession,
                    apiClient = apiClient,
                    addonRepository = addonRepository,
                    onBack = { currentScreen = HarborScreen.Main },
                    onOpenSettings = {
                        currentNavTab = HarborNavTab.SETTINGS
                        currentScreen = HarborScreen.Main
                    },
                    onPlayStream = { title, streamUrl, mediaId, season, episode, epTitle, poster, background ->
                        currentScreen = HarborScreen.Player(
                            title = title,
                            streamUrl = streamUrl,
                            mediaId = mediaId,
                            season = season,
                            episode = episode,
                            episodeTitle = epTitle,
                            poster = poster,
                            background = background
                        )
                    }
                )
            }
            is HarborScreen.Main -> {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = {
                        if (currentNavTab != HarborNavTab.SEARCH) {
                            HarborTopBar(
                                title = "",
                                onSearchClick = { currentNavTab = HarborNavTab.SEARCH },
                                onSettingsClick = { currentNavTab = HarborNavTab.SETTINGS }
                            )
                        }
                    },
                    bottomBar = {
                        HarborBottomNav(
                            currentTab = currentNavTab,
                            onTabSelected = { currentNavTab = it }
                        )
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        when (currentNavTab) {
                            HarborNavTab.DISCOVER -> {
                                DiscoverScreen(
                                    catalogRepository = catalogRepository,
                                    vaultRepository = vaultRepository,
                                    onMediaSelected = { type, id ->
                                        currentScreen = HarborScreen.MediaDetail(type, id)
                                    },
                                    onPlayDirect = { title, url ->
                                        currentScreen = HarborScreen.Player(
                                            title = title,
                                            streamUrl = url,
                                            mediaId = "direct_play"
                                        )
                                    },
                                    onOpenSearch = {
                                        currentNavTab = HarborNavTab.SEARCH
                                    },
                                    onOpenSettings = {
                                        currentNavTab = HarborNavTab.SETTINGS
                                    }
                                )
                            }
                            HarborNavTab.SEARCH -> {
                                SearchScreen(
                                    catalogRepository = catalogRepository,
                                    onMediaSelected = { type, id ->
                                        currentScreen = HarborScreen.MediaDetail(type, id)
                                    }
                                )
                            }
                            HarborNavTab.VAULT -> {
                                VaultScreen(
                                    vaultRepository = vaultRepository,
                                    onMediaSelected = { type, id ->
                                        currentScreen = HarborScreen.MediaDetail(type, id)
                                    },
                                    onPlayDirect = { title, url ->
                                        currentScreen = HarborScreen.Player(
                                            title = title,
                                            streamUrl = url,
                                            mediaId = "vault_play"
                                        )
                                    }
                                )
                            }
                            HarborNavTab.SETTINGS -> {
                                SettingsScreen(
                                    currentTheme = currentThemeStyle,
                                    onThemeSelected = { currentThemeStyle = it },
                                    stremioSession = stremioSession,
                                    apiClient = apiClient,
                                    addonRepository = addonRepository,
                                    streamResolverRepository = streamResolver
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
