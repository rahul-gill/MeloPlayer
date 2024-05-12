package meloplayer.app.ui.screen

import android.content.ContentUris
import android.provider.MediaStore
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import meloplayer.app.R
import meloplayer.core.store.MediaStoreSong
import meloplayer.core.ui.components.MediaItemListCard


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongListScreen(
    songs: List<MediaStoreSong>?,
    onSongClick: (MediaStoreSong) -> Unit,
    onShuffleSongs: () -> Unit = {}
) {
    val scrollBehavior =
        TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(
                        text = stringResource(id = R.string.songs),
                    )
                },
                scrollBehavior = scrollBehavior
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onShuffleSongs) {
                Icon(imageVector = Icons.Default.Shuffle, contentDescription = stringResource(R.string.shuffle_songs_and_play))
            }
        }
    ) { innerPadding ->

        if (songs == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "Error")
            }
        } else {
            LazyColumn(
                modifier = Modifier.padding(innerPadding),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(songs) { song ->
                    MediaItemListCard(
                        imageModel = ContentUris.withAppendedId(
                            "content://media/external/audio/albumart".toUri(),
                            song.albumId
                        ),
                        title = song.title,
                        subtitle = song.artistName,
                        onClick = { onSongClick(song) }
                    )
                }
            }
        }
    }
}
