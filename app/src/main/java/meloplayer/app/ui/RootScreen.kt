package meloplayer.app.ui

import androidx.compose.animation.togetherWith
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.ArtTrack
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.LineStyle
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PersonalInjury
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.olshevski.navigation.reimagined.AnimatedNavHost
import dev.olshevski.navigation.reimagined.NavAction
import dev.olshevski.navigation.reimagined.NavBackHandler
import dev.olshevski.navigation.reimagined.moveToTop
import dev.olshevski.navigation.reimagined.navigate
import dev.olshevski.navigation.reimagined.rememberNavController
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import meloplayer.app.playbackx.PlaybackCommand
import meloplayer.app.playbackx.PlaybackStateX
import meloplayer.app.playbackx.PlaybackTimeline
import meloplayer.app.playbackx.glue.PlaybackGlue
import meloplayer.app.ui.comps.nowplaying.MiniPlayer
import meloplayer.app.ui.comps.nowplaying.NowPlayingPanel
import meloplayer.app.ui.screen.AlbumListScreen
import meloplayer.app.ui.screen.ArtistListScreen
import meloplayer.app.ui.screen.SongListScreen
import meloplayer.core.store.repo.SongsRepository
import meloplayer.core.ui.components.nowplaying.PlayerSheetScaffold
import meloplayer.core.ui.components.nowplaying.rememberPlayerSheetState
import meloplayer.core.ui.materialSharedAxisZIn
import meloplayer.core.ui.materialSharedAxisZOut


enum class TabScreen {
    ForYou, Songs, Albums, Artists, Folders, Playlist
}

val playbackManager
    get() = PlaybackGlue.instance.playbackManagerX


@Composable
fun RootScreen(
) {
    val navController = rememberNavController(TabScreen.Songs)
    NavBackHandler(controller = navController)
    val sheetState = rememberPlayerSheetState()
    val coroutineScope = rememberCoroutineScope()

    val state by PlaybackGlue.instance.playbackManagerX.playbackStateX.map {
        when(it){
            PlaybackStateX.Empty -> null
            is PlaybackStateX.OnGoing -> it
        }
    }.collectAsStateWithLifecycle(initialValue = null)
    val currentSong = remember(state){ state?.currentMediaItemId }
    val playbackProgressPos = remember(state){ state?.timeline as? PlaybackTimeline.Prepared }
    val queue = remember(state){ state?.queue }
    val currentSongIndex = remember(state){ state?.currentItemIndex }
    val playbackProgress = remember(playbackProgressPos) {
        playbackProgressPos?.let {
            it.currentMillis * 1f / it.totalMills
        } ?: 0f
    }
    val isPlaying = remember(state) {
        state?.isPlaying ?: false
    }
    val currentSongDetails = remember(state) {
        state?.currentMediaItemId?.let {  id ->
            SongsRepository.instance.songById(id).getOrNull()
        }
    }

    PlayerSheetScaffold(
        sheetState = sheetState,
        fullPlayerContent = {
            NowPlayingPanel(
                playItem = { playbackManager.handleCommand(PlaybackCommand.SetCurrentQueueItemIndex(it)) },
                playingQueueAlbumArtUris = queue ?: listOf(),
                currentItemIndex = currentSongIndex,
                currentPlaybackProgress = playbackProgress,
                setPlaybackProgress = { floatVal ->
                    playbackProgressPos?.let { posCurr ->
                        playbackManager.handleCommand(PlaybackCommand.SetPosition(
                            (posCurr.totalMills * floatVal).toLong()
                        ))
                    }
                }
            )
        },
        miniPlayerContent = { applyNavBarPadding ->
            val currentSongThis = currentSongDetails
            if (currentSongThis != null) {
                MiniPlayer(
                    isPlaying = isPlaying,
                    currentSong = currentSongThis,
                    playbackProgress = playbackProgress,
                    onClick = {
                        coroutineScope.launch { sheetState.expandToFullPlayer() }
                    },
                    onSkipToPrevious = {
                        playbackManager.handleCommand(PlaybackCommand.SkipPrevious)
                    },
                    onSkipToNext = {
                        playbackManager.handleCommand(PlaybackCommand.SkipNext)
                    },
                    onSwitchPlayPause = {
                        playbackManager.handleCommand(PlaybackCommand.SwitchPlaying)
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