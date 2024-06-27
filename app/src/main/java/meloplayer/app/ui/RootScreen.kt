package meloplayer.app.ui

import android.content.ContentUris
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.core.net.toUri
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
import meloplayer.app.ui.comps.nowplaying.MiniPlayer
import meloplayer.app.ui.comps.nowplaying.NowPlayingPanel
import meloplayer.app.ui.screen.AlbumListScreen
import meloplayer.app.ui.screen.ArtistListScreen
import meloplayer.app.ui.screen.SongListScreen
import meloplayer.core.store.MediaStoreUtils.getArtworkUriForSong
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
    val playbackProgressPos = playbackManager?.player?.playbackPosition?.collectAsStateWithLifecycle()
    val queue by playbackManager.queueManager.currentQueue.collectAsStateWithLifecycle(initialValue = listOf())
    val currentSongIndex by playbackManager.queueManager.currentSongIndex.collectAsStateWithLifecycle()
    val playbackProgress = playbackProgressPos?.value?.let {
        it.currentDurationMillis * 1f / it.totalDurationMillis
    } ?: 0f
    PlayerSheetScaffold(
        sheetState = sheetState,
        fullPlayerContent = {
            NowPlayingPanel(
                playItem = { playbackManager.playWithId(it) },
                playingQueueAlbumArtUris = queue,
                currentItemIndex = currentSongIndex,
                currentPlaybackProgress = playbackProgress,
                setPlaybackProgress = { floatVal ->
                    playbackProgressPos?.value?.let { posCurr ->
                        playbackManager.player.seekTo((posCurr.totalDurationMillis * floatVal).toLong())
                    }
                }
            )
        },
        miniPlayerContent = { applyNavBarPadding ->
            val currentSongThis = currentSong?.value
            if (currentSongThis != null) {
                MiniPlayer(
                    isPlaying = playbackManager.player.isPlaying.collectAsStateWithLifecycle().value,
                    currentSong = currentSongThis,
                    playbackProgress = playbackProgress,
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