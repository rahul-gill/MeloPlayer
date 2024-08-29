package meloplayer.app.ui

import androidx.compose.animation.togetherWith
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.LineStyle
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PersonalInjury
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastDistinctBy
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
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
import meloplayer.app.ui.screen.songs.SongListScreen
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
                                TabScreen.Artists -> Icons.Default.Hub
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
//                    val songsNew = remember {
//                        db.schemaQueries.songsList().executeAsList()
//                            .sortedBy { it.song_title }
//                            .groupBy { it.song_id }
//                    }.toList()
//                    LazyColumn {
//                        items(songsNew){ songHybridItem ->
//                            Card(Modifier.padding(8.dp)) {
//                                Column(Modifier.padding(8.dp)) {
//                                    Row(Modifier.padding(end = 8.dp)) {
//                                        AsyncImage(
//                                            songHybridItem.second.first().cover_image_uri,
//                                            null,
//                                            contentScale = ContentScale.Crop,
//                                            modifier = Modifier
//                                                .height(52.dp)
//                                                .aspectRatio(1f)
//                                                .clip(RoundedCornerShape(12))
//                                        )
//                                    }
//                                    Column {
//                                        Text(text = "Song name: ${songHybridItem.second.first().song_title}")
//                                        Text(text = "Album name: ${songHybridItem.second.first().album_id}")
////                                        Text(text = "Artist name: ${songHybridItem.second.map { it. }.distinct()}")
////                                        Text(text = "Genre name: ${songHybridItem.second.map { it.genre_name }.distinct()}")
//                                    }
//                                }
//                            }
//                        }
//                    }
                }

                TabScreen.Songs -> {
                    SongListScreen(

                    )
                }

                TabScreen.Albums -> {
                    AlbumListScreen()
                }

                TabScreen.Artists -> {
//                    val withSongs = remember {
//                        db.schemaQueries.artistListWithSongs().executeAsList()
//                            .sortedBy { it.artist }
//                            .fastDistinctBy { it.song }
//                            .groupBy { it.artist }
//
//                    }.toList()
//                    val withAlbums = remember {
//                        db.schemaQueries.artistListWithAlbums().executeAsList()
//                            .sortedBy { it.artist_name }
//                            .fastDistinctBy { it.album_name }
//                            .groupBy { it.artist_name }
//                    }
//                    LazyColumn {
//                        items(withSongs){ songHybridItem ->
//                            Card(Modifier.padding(8.dp)) {
//                                Text(text = songHybridItem.first, style = MaterialTheme.typography.titleLarge)
//                                Row(Modifier.padding(8.dp)) {
//                                    Column(Modifier.weight(1f)) {
//                                        songHybridItem.second.forEachIndexed { index, item ->
//                                            Text(text = "${index + 1} ${item.song}")
//                                        }
//                                    }
//                                    Spacer(Modifier.width(8.dp))
//                                    Column(Modifier.weight(1f)) {
//                                        withAlbums.getOrDefault(songHybridItem.first, listOf()).forEachIndexed { index, item ->
//                                            Text(text = "${index+1} ${item.album_name}")
//                                        }
//                                    }
//                                }
//                            }
//                        }
//                    }
                }

                TabScreen.Folders -> {}
                TabScreen.Playlist -> {}
            }
        }
    }

}