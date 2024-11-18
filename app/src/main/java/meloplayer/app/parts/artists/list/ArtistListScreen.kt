package meloplayer.app.parts.artists.list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import meloplayer.app.R
import meloplayer.app.db.entities.Artist
import meloplayer.app.parts.songs.list.SongSortOrder
import meloplayer.core.ui.components.MediaItemListCard
import meloplayer.core.ui.components.base.PopupMenu
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArtistListScreen(
    viewModel: ArtistListViewModel = koinViewModel()
) {
    val context = LocalContext.current
    val artists by viewModel.artists.collectAsState(listOf())
    var sortOrder by remember {
        mutableStateOf(SongSortOrder.Name(isAscending = true))
    }
    val onSongClick = remember {
        { song: Artist ->
//        if (!songs.isNullOrEmpty()) {
//            playbackManager.startPlayingWithQueueInit(songs?.map { it.id } ?: listOf())
//        }
//        playbackManager.playWithId(song.id)

        }
    }
    val onShuffleSongs = remember {
        {
//        if (!songs.isNullOrEmpty()) {
//            playbackManager.startPlayingWithQueueInit(songs?.map { it.id } ?: listOf())
//        }
        }
    }


    val scrollBehavior =
        TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    var showOverflowMenu by remember {
        mutableStateOf(false)
    }
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(
                        text = stringResource(id = R.string.songs),
                    )
                },
                scrollBehavior = scrollBehavior,
                actions = {
                    IconButton(onClick = { showOverflowMenu = true }) {
                        Icon(imageVector = Icons.Default.MoreVert, contentDescription = "TODO")
                    }
                    if(showOverflowMenu)
                    PopupMenu(
                        onDismissRequest = { showOverflowMenu = false }
                    ) {

                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onShuffleSongs) {
                Icon(
                    imageVector = Icons.Default.Shuffle,
                    contentDescription = stringResource(R.string.shuffle_songs_and_play)
                )
            }
        }
    ) { innerPadding ->

        val state= rememberLazyListState()
        val itemToScrollTo = remember {
            mutableStateOf(10)
        }
        LaunchedEffect(key1 = Unit) {
            state.animateScrollToItem(itemToScrollTo.value)
            itemToScrollTo.value += 1
            delay(1000)
        }
        LazyColumn(
            state = state,
            modifier = Modifier.padding(innerPadding),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(artists.size) { index ->
                val artist = artists[index]
                MediaItemListCard(
                    imageModel = null,
                    title = artist.name,
                    subtitle = "",
                    onClick = { onSongClick(artist) }
                )
            }
        }
    }
}