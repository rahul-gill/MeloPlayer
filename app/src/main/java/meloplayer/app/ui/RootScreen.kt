package meloplayer.app.ui

import android.content.ContentUris
import android.provider.MediaStore
import android.widget.Toast
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Face
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.media3.common.C.TIME_UNSET
import dev.olshevski.navigation.reimagined.AnimatedNavHost
import dev.olshevski.navigation.reimagined.NavAction
import dev.olshevski.navigation.reimagined.NavBackHandler
import dev.olshevski.navigation.reimagined.moveToTop
import dev.olshevski.navigation.reimagined.navigate
import dev.olshevski.navigation.reimagined.rememberNavController
import kotlinx.coroutines.launch
import meloplayer.app.R
import meloplayer.app.ui.screen.SongListScreen
import meloplayer.core.playback.CrossFadePlayerWrapper
import meloplayer.core.store.MediaStoreSong
import meloplayer.core.store.MediaStoreSongsFetcher
import meloplayer.core.store.repo.SongsRepository
import meloplayer.core.ui.components.base.NavigationBarTabs
import meloplayer.core.ui.components.base.TabItem
import meloplayer.core.ui.components.base.sheet.PlayerSheetScaffold
import meloplayer.core.ui.components.base.sheet.rememberPlayerSheetState
import meloplayer.core.ui.materialSharedAxisZIn
import meloplayer.core.ui.materialSharedAxisZOut


enum class TabScreen {
    ForYou, Songs, Albums, Artists, Folders, Playlist
}

@Composable
fun RootScreen(
) {
    val navController = rememberNavController(TabScreen.ForYou)
    val scope = rememberCoroutineScope()
    NavBackHandler(controller = navController)
    val sheetState = rememberPlayerSheetState()
    val context = LocalContext.current
    val player = remember {
        CrossFadePlayerWrapper(context)
    }
    var nowPlayingSong: MediaStoreSong? by remember {
        mutableStateOf(null)
    }
    val songs = remember {
        SongsRepository(fetcher = MediaStoreSongsFetcher.getImpl(context))
            .allSongs.getOrNull().also {
                Toast.makeText(context, "Got ${it?.size} items", Toast.LENGTH_SHORT).show()
            }
    }
    PlayerSheetScaffold(
        sheetState = sheetState,
        fullPlayerContent = {
            Column(
                modifier = Modifier
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
                    albumId = nowPlayingSong?.albumId,
                    playItemAtIndex = { itemIndex.intValue = it },
                    playingQueue = queue.map { painterResource(id = it) },
                    currentItemIndex = itemIndex.intValue
                )
            }
        },
        miniPlayerContent = {
            val song = nowPlayingSong
            if (song != null) {
                MiniPlayer(
                    onClick = { scope.launch { sheetState.expandToFullPlayer() } },
                    currentSong = song,
                    isPlaying = player.player.player.isPlaying,
                    playbackProgress = 0.5f,
                )
            }
        },
        navBarContent = {
            val selectedItem = navController.backstack.entries.last().destination
            val tabs = remember {
                TabScreen.entries.toList()
            }
            NavigationBarTabs(
                modifier = Modifier
                    .selectableGroup()
                    .navigationBarsPadding()
                    .horizontalScroll(rememberScrollState())
            ) {
                tabs.forEach { tabScreen ->
                    TabItem(
                        modifier = Modifier.background(MaterialTheme.colorScheme.surface),
                        onClick = {
                            if (!navController.moveToTop { it == tabScreen }) {
                                navController.navigate(tabScreen)
                            }
                        },
                        selected = selectedItem == tabScreen,
                        text = when (tabScreen) {
                            TabScreen.ForYou -> "For you"
                            TabScreen.Songs -> "Songs"
                            TabScreen.Albums -> "Albums"
                            TabScreen.Artists -> "Artists"
                            TabScreen.Folders -> "Folders"
                            TabScreen.Playlist -> "Playlists"
                        }
                    )
                }
            }
        },
        content = {
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
                        player.startPlaying(
                            ContentUris.withAppendedId(
                                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                                song.id
                            )
                        )
                        nowPlayingSong = song
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
    )
}