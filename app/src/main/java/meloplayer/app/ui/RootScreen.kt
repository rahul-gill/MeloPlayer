package meloplayer.app.ui

import android.content.ContentUris
import android.widget.Toast
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonPin
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.util.fastFirst
import androidx.core.net.toUri
import dev.olshevski.navigation.reimagined.AnimatedNavHost
import dev.olshevski.navigation.reimagined.NavAction
import dev.olshevski.navigation.reimagined.NavBackHandler
import dev.olshevski.navigation.reimagined.moveToTop
import dev.olshevski.navigation.reimagined.navigate
import dev.olshevski.navigation.reimagined.rememberNavController
import kotlinx.coroutines.launch
import meloplayer.app.R
import meloplayer.app.ui.screen.SongListScreen
import meloplayer.app.playback.PlaybackManger
import meloplayer.app.ui.comps.nowplaying.NowPlayingPanel
import meloplayer.core.store.MediaStoreSongsFetcher
import meloplayer.core.store.repo.SongsRepository
import meloplayer.core.ui.components.base.NavigationBarTabs
import meloplayer.core.ui.components.base.TabItem
import meloplayer.core.ui.components.base.sheet.NavigationSuiteScaffold
import meloplayer.core.ui.components.base.sheet.PlayerSheetScaffold
import meloplayer.core.ui.components.base.sheet.PlayerSheetStateType
import meloplayer.core.ui.components.base.sheet.rememberPlayerSheetState
import meloplayer.core.ui.materialSharedAxisZIn
import meloplayer.core.ui.materialSharedAxisZOut


enum class TabScreen {
    ForYou, Songs, Albums, Artists, Folders, Playlist
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RootScreen(
) {
    val navController = rememberNavController(TabScreen.ForYou)
    val scope = rememberCoroutineScope()
    NavBackHandler(controller = navController)
    val sheetState = rememberPlayerSheetState()
    val context = LocalContext.current
    val songs = remember {
        SongsRepository(fetcher = MediaStoreSongsFetcher.getImpl(context))
            .allSongs.getOrNull().also {
                Toast.makeText(context, "Got ${it?.size} items", Toast.LENGTH_SHORT).show()
            }
    }
    val playbackManager = remember {
        PlaybackManger(
            context = context
        )
    }
    val currentSongIndex by playbackManager.queueManager.currentSongIndex.collectAsState()
    val currentQueue by playbackManager.queueManager.currentQueue.collectAsState()
    val currentSong = remember(currentSongIndex, currentQueue) {
        val songId =  currentSongIndex?.let { s ->
            currentQueue.getOrNull(s)
        }
        songs?.firstOrNull { it.id == songId }
    }

    val selectedItem = navController.backstack.entries.last().destination
    val tabs = remember {
        TabScreen.entries.toList()
    }
    val intr = remember {
        MutableInteractionSource()
    }

    val alpha = remember(sheetState.sheetExpansionRatio) {
        derivedStateOf {
            if (sheetState.sheetExpansionRatio >= 0.5f) {
                0f
            } else {
                1 - sheetState.sheetExpansionRatio * 2f
            }
        }
    }
    NavigationSuiteScaffold(
                sheetState = sheetState,
        fullPlayerContent = fullContent@{
            if(currentSong == null){
                return@fullContent
            }
            Column(
                modifier = Modifier
                    .graphicsLayer {
                        this.alpha =
                            if (sheetState.targetValue == PlayerSheetStateType.FullPlayer) 1f
                            else (0.5f - alpha.value) * 2
                    }
                    .weight(1f)
                    .fillMaxWidth()
                    .systemBarsPadding()
                    .background(color = MaterialTheme.colorScheme.background)
            ) {
                val queue = remember {
                    listOf(
                        R.drawable.app_icon,
                        R.drawable.app_icon_monochrome,
                        R.drawable.app_icon,
                        R.drawable.app_icon_monochrome,
                        R.drawable.app_icon,
                        R.drawable.app_icon_monochrome,
                        R.drawable.app_icon,
                        R.drawable.app_icon_monochrome,
                    )
                }
                val itemIndex = remember {
                    mutableIntStateOf(0)
                }
                NowPlayingPanel(
                    playItemAtIndex = { index ->
                        if(songs != null){
                            playbackManager.playWithId(songs[index].id)
                        }

                    },
                    playingQueueAlbumArtUris = songs?.map {
                        ContentUris.withAppendedId(
                            "content://media/external/audio/albumart".toUri(),
                            it.albumId
                        )
                    } ?: listOf(),
                    currentItemIndex = songs?.indexOf(currentSong) ?: 0,
                    currentPlaybackProgress = kotlin.run {
                        playbackManager.player.playbackPosition.collectAsState().value?.let {
                            it.currentDurationMillis * 1f / it.totalDurationMillis
                        } ?: 0f
                    },
                    setPlaybackProgress = { newFrac ->
                        playbackManager.player.playbackPosition.value?.let {
                            playbackManager.player.seekTo((it.totalDurationMillis * newFrac).toLong())
                        }
                    }
                )
            }
        },
        miniPlayerContent = {
            if (currentSong != null) {
                MiniPlayer(
                    modifier = Modifier
                        .graphicsLayer {
                            this.alpha = alpha.value
                        }
                     ,
                    onClick = { scope.launch { sheetState.expandToFullPlayer() } },
                    currentSong = currentSong,
                    isPlaying = playbackManager.player.isPlaying.collectAsState().value,
                    playbackProgress = 0.5f,
                    showTrackControls = true,
                    onSkipToNext = { playbackManager.goToNextSong() }
                )
            }
        },


        navigationSuiteItems = {
            tabs.forEach {  tab ->
                item(
                    selected = selectedItem == tab,
                    onClick = {
                        if (!navController.moveToTop { it == tab }) {
                                navController.navigate(tab)
                            }
                    },
                    icon = {
                        Icon(imageVector = when(tab){
                            TabScreen.ForYou -> Icons.Default.Person
                            TabScreen.Songs -> Icons.Default.MusicNote
                            TabScreen.Albums -> Icons.Default.Album
                            TabScreen.Artists -> Icons.Default.PersonPin
                            TabScreen.Folders -> Icons.Default.Folder
                            TabScreen.Playlist -> Icons.Default.FormatListNumbered
                        },
                        contentDescription = null
                        )
                    },
                    interactionSource = intr
                )
            }

        }
    ){
        AnimatedNavHost(
            controller = navController,
            transitionSpec = { action, _, _ ->
                materialSharedAxisZIn(forward = action != NavAction.Pop) togetherWith
                        materialSharedAxisZOut(forward = action == NavAction.Pop)
            }
        ) { tab ->
            SongListScreen(
                songs = songs,
                onSongClick = { song ->
                    if (!songs.isNullOrEmpty()) {
                        playbackManager.startPlayingWithQueueInit(songs.map { it.id })
                    }
                    playbackManager.playWithId(song.id)
                }
                ,
                onShuffleSongs = {
                    if (!songs.isNullOrEmpty()) {
                        playbackManager.startPlayingWithQueueInit(songs.map { it.id })
                    }
                }
            )
            when (tab) {
                TabScreen.ForYou -> {

                }

                TabScreen.Songs -> {

                }

                TabScreen.Albums -> {}
                TabScreen.Artists -> {}
                TabScreen.Folders -> {}
                TabScreen.Playlist -> {}
            }
        }
    }

//    PlayerSheetScaffold(
//        sheetState = sheetState,
//        fullPlayerContent = {
//            Column(
//                modifier = Modifier
//                    .weight(1f)
//                    .fillMaxWidth()
//                    .systemBarsPadding()
//                    .background(color = MaterialTheme.colorScheme.background)
//            ) {
//                val queue = remember {
//                    listOf(
//                        R.drawable.app_icon,
//                        R.drawable.app_icon_monochrome,
//                        R.drawable.app_icon,
//                        R.drawable.app_icon_monochrome,
//                        R.drawable.app_icon,
//                        R.drawable.app_icon_monochrome,
//                        R.drawable.app_icon,
//                        R.drawable.app_icon_monochrome,
//                    )
//                }
//                val itemIndex = remember {
//                    mutableIntStateOf(0)
//                }
//                NowPlayingPanel(
//                    playItemAtIndex = { index ->
//                        if(songs != null){
//                            playbackManager.playWithId(songs[index].id)
//                        }
//
//                    },
//                    playingQueueAlbumArtUris = songs?.map {
//                        ContentUris.withAppendedId(
//                            "content://media/external/audio/albumart".toUri(),
//                            it.albumId
//                        )
//                    } ?: listOf(),
//                    currentItemIndex = songs?.indexOf(currentSong) ?: 0,
//                    currentPlaybackProgress = kotlin.run {
//                        playbackManager.player.playbackPosition.collectAsState().value?.let {
//                            it.currentDurationMillis * 1f / it.totalDurationMillis
//                        } ?: 0f
//                    },
//                    setPlaybackProgress = { newFrac ->
//                        playbackManager.player.playbackPosition.value?.let {
//                            playbackManager.player.seekTo((it.totalDurationMillis * newFrac).toLong())
//                        }
//                    }
//                )
//            }
//        },
//        miniPlayerContent = {
//            if (currentSong != null) {
//                MiniPlayer(
//                    onClick = { scope.launch { sheetState.expandToFullPlayer() } },
//                    currentSong = currentSong,
//                    isPlaying = playbackManager.player.isPlaying.collectAsState().value,
//                    playbackProgress = 0.5f,
//                    showTrackControls = true,
//                    onSkipToNext = { playbackManager.goToNextSong() }
//                )
//            }
//        },
//        navBarContent = {
//            val selectedItem = navController.backstack.entries.last().destination
//            val tabs = remember {
//                TabScreen.entries.toList()
//            }
//            NavigationBarTabs(
//                modifier = Modifier
//                    .selectableGroup()
//                    .navigationBarsPadding()
//                    .horizontalScroll(rememberScrollState())
//            ) {
//                tabs.forEach { tabScreen ->
//                    TabItem(
//                        modifier = Modifier.background(MaterialTheme.colorScheme.surface),
//                        onClick = {
//                            if (!navController.moveToTop { it == tabScreen }) {
//                                navController.navigate(tabScreen)
//                            }
//                        },
//                        selected = selectedItem == tabScreen,
//                        text = when (tabScreen) {
//                            TabScreen.ForYou -> "For you"
//                            TabScreen.Songs -> "Songs"
//                            TabScreen.Albums -> "Albums"
//                            TabScreen.Artists -> "Artists"
//                            TabScreen.Folders -> "Folders"
//                            TabScreen.Playlist -> "Playlists"
//                        }
//                    )
//                }
//            }
//        },
//        content = {
//            AnimatedNavHost(
//                controller = navController,
//                transitionSpec = { action, _, _ ->
//                    materialSharedAxisZIn(forward = action != NavAction.Pop) togetherWith
//                            materialSharedAxisZOut(forward = action == NavAction.Pop)
//                }
//            ) { tab ->
//                SongListScreen(
//                    songs = songs,
//                    onSongClick = { song ->
//                        if (!songs.isNullOrEmpty()) {
//                            playbackManager.startPlayingWithQueueInit(songs.map { it.id })
//                        }
//                        playbackManager.playWithId(song.id)
//                    }
//                    ,
//                    onShuffleSongs = {
//                        if (!songs.isNullOrEmpty()) {
//                            playbackManager.startPlayingWithQueueInit(songs.map { it.id })
//                        }
//                    }
//                )
//                when (tab) {
//                    TabScreen.ForYou -> {
//
//                    }
//
//                    TabScreen.Songs -> {
//
//                    }
//
//                    TabScreen.Albums -> {}
//                    TabScreen.Artists -> {}
//                    TabScreen.Folders -> {}
//                    TabScreen.Playlist -> {}
//                }
//            }
//        }
//    )
}