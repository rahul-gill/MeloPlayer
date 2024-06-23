package meloplayer.app.ui

import androidx.compose.animation.togetherWith
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.ArtTrack
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.LineStyle
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PersonalInjury
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.olshevski.navigation.reimagined.AnimatedNavHost
import dev.olshevski.navigation.reimagined.NavAction
import dev.olshevski.navigation.reimagined.NavBackHandler
import dev.olshevski.navigation.reimagined.moveToTop
import dev.olshevski.navigation.reimagined.navigate
import dev.olshevski.navigation.reimagined.rememberNavController
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import meloplayer.app.playback.PlaybackManger
import meloplayer.app.playback.session.PlaybackService
import meloplayer.app.ui.screen.AlbumListScreen
import meloplayer.app.ui.screen.ArtistListScreen
import meloplayer.app.ui.screen.SongListScreen
import meloplayer.core.startup.applicationContextGlobal
import meloplayer.core.store.repo.SongsRepository
import meloplayer.core.ui.components.nowplaying.PlayerSheetScaffold
import meloplayer.core.ui.components.nowplaying.rememberPlayerSheetState
import meloplayer.core.ui.materialSharedAxisZIn
import meloplayer.core.ui.materialSharedAxisZOut


enum class TabScreen {
    ForYou, Songs, Albums, Artists, Folders, Playlist
}

val playbackManager
    get() = PlaybackManger.instance

@Composable
fun RootScreen(
) {
    val navController = rememberNavController(TabScreen.Songs)
    NavBackHandler(controller = navController)
    val sheetState = rememberPlayerSheetState()
    val coroutineScope = rememberCoroutineScope()

    val currentSong = playbackManager.queueManager.currentItem
        .map { if (it == null) null else SongsRepository.instance.songById(it).getOrNull() }
        .collectAsStateWithLifecycle(
            initialValue = null
        )
    val playbackProgress = playbackManager?.player?.playbackPosition?.collectAsStateWithLifecycle()

    PlayerSheetScaffold(
        sheetState = sheetState,
        fullPlayerContent = {
            Surface(
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.fillMaxSize()
            ) {

            }
        },
        miniPlayerContent = { applyNavBarPadding ->
            val currentSongThis = currentSong?.value
            if (currentSongThis != null) {
                MiniPlayer(
                    currentSong = currentSongThis,
                    playbackProgress = playbackProgress?.value?.let {
                        it.currentDurationMillis * 1f / it.totalDurationMillis
                    } ?: 0f,
                    onClick = {
                        coroutineScope.launch { sheetState.expandToFullPlayer() }
                    },
                    onSkipToPrevious = {
                        playbackManager?.skipToPrevious()
                    },
                    onSkipToNext = {
                        playbackManager?.goToNextSong()
                    },
                    onSwitchPlayPause = {
                        playbackManager?.player?.switchIsPlaying()
                    },
                    insetPaddings = if (applyNavBarPadding) WindowInsets.navigationBars else null
                )
            }
        },
        navigationSuiteItems = {
            TabScreen.entries.forEach { tabHere ->
                item(
                    selected = navController.backstack.entries.lastOrNull()?.destination == tabHere,
                    icon = {
                        Icon(
                            imageVector = when (tabHere) {
                                TabScreen.ForYou -> Icons.Default.PersonalInjury
                                TabScreen.Songs -> Icons.Default.MusicNote
                                TabScreen.Albums -> Icons.Default.Album
                                TabScreen.Artists -> Icons.Default.ArtTrack
                                TabScreen.Folders -> Icons.Default.Folder
                                TabScreen.Playlist -> Icons.Default.LineStyle
                            }, contentDescription = null
                        )
                    },
                    interactionSource = MutableInteractionSource(),
                    onClick = {
                        if (!navController.moveToTop { it == tabHere }) {
                            navController.navigate(tabHere)
                        }
                    }
                )
            }
        }
    ) {
        AnimatedNavHost(
            controller = navController,
            transitionSpec = { action, _, _ ->
                materialSharedAxisZIn(forward = action != NavAction.Pop) togetherWith
                        materialSharedAxisZOut(forward = action == NavAction.Pop)
            }
        ) { tab ->

            when (tab) {
                TabScreen.ForYou -> {

                }

                TabScreen.Songs -> {
                    SongListScreen(

                    )
                }

                TabScreen.Albums -> {
                    AlbumListScreen()
                }

                TabScreen.Artists -> {
                    ArtistListScreen()
                }

                TabScreen.Folders -> {}
                TabScreen.Playlist -> {}
            }
        }
    }

}