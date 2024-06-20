package meloplayer.app.ui.screen

import android.content.ContentUris
import android.content.res.Configuration
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import meloplayer.app.R
import meloplayer.app.ui.playbackManager
import meloplayer.core.store.model.MediaStoreSong
import meloplayer.core.store.model.SongSortOrder
import meloplayer.core.store.model.toStringResource
import meloplayer.core.store.repo.SongsRepository
import meloplayer.core.ui.AppTheme
import meloplayer.core.ui.components.MediaItemListCard
import ua.hospes.lazygrid.GridCells
import ua.hospes.lazygrid.LazyVerticalGrid
import ua.hospes.lazygrid.items
import java.time.format.DateTimeFormatter
import kotlin.reflect.KClass


private val monthFormatter by lazy {
    DateTimeFormatter.ofPattern("MMMM yyyy")
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun SongListScreen(
) {
    var songSortOrder: SongSortOrder by remember {
        mutableStateOf(SongSortOrder.DateModified())
    }
    var songsDir by remember {
        mutableStateOf<List<MediaStoreSong>>(listOf())
    }
    var songs by remember {
        mutableStateOf<Map<String, List<MediaStoreSong>>?>(
            mapOf()
        )
    }

    val onSongClick = { song: MediaStoreSong ->
        if (!songs.isNullOrEmpty()) {
            playbackManager.startPlayingWithQueueInit(songsDir.map { it.id })
        }
        playbackManager.playWithId(song.id)

    }
    LaunchedEffect(key1 = songSortOrder) {
        withContext(Dispatchers.IO) {
            val songRes = SongsRepository.instance
                .songs().getOrNull() ?: listOf()
            songsDir = songRes
            withContext(Dispatchers.Main) {
                val sortKeyGetter = { it: MediaStoreSong ->
                    when (songSortOrder) {
                        is SongSortOrder.Name -> it.title.firstOrNull()?.toString() ?: ""
                        is SongSortOrder.Album -> it.albumName
                        is SongSortOrder.AlbumArtist -> it.albumArtist ?: ""
                        is SongSortOrder.Composer -> it.composer ?: ""
                        is SongSortOrder.DateModified -> it.dateModified.format(monthFormatter)
                        is SongSortOrder.Duration -> when {
                            it.duration < 1000_000 -> "Less than a minute"
                            it.duration in 1000_000..2000_000 -> "1 to 2 minutes"
                            it.duration in 2000_000..3000_000 -> "2 to 3 minutes"
                            it.duration in 3000_000..4000_000 -> "3 to 4 minutes"
                            it.duration in 4000_000..5000_000 -> "4 to 5 minutes"
                            it.duration in 5000_000..10000_000 -> "5 to 10 minutes"
                            else -> "More than 10 minutes"
                        } //TODO: localization
                        is SongSortOrder.SongArtist -> it.artistNames.firstOrNull() ?: ""
                        is SongSortOrder.Year -> "${it.year}"
                    }
                }
                songs = songRes
                    .sortedWith { a, b ->
                        when {
                            (songSortOrder is SongSortOrder.Duration) -> {
                                if (songSortOrder.isAscending) a.duration.compareTo(b.duration)
                                else b.duration.compareTo(a.duration)
                            }

                            (songSortOrder is SongSortOrder.DateModified) -> {
                                if (songSortOrder.isAscending) a.dateModified.compareTo(b.dateModified)
                                else b.dateModified.compareTo(a.dateModified)
                            }

                            (songSortOrder is SongSortOrder.Year) -> {
                                if (songSortOrder.isAscending) a.year.compareTo(b.year)
                                else b.year.compareTo(a.year)
                            }

                            else -> {
                                if (songSortOrder.isAscending) sortKeyGetter(a).compareTo(
                                    sortKeyGetter(b)
                                )
                                else sortKeyGetter(b).compareTo(sortKeyGetter(a))
                            }
                        }
                    }
                    .groupBy { sortKeyGetter(it) }
            }
        }
    }
    var sortOrder by remember {
        mutableStateOf(SongSortOrder.Name(isAscending = true))
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
                    if (showOverflowMenu) {
                        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
                        ModalBottomSheet(
                            sheetState = sheetState,
                            onDismissRequest = { showOverflowMenu = true }) {
                            var newSortOrder by remember {
                                mutableStateOf(songSortOrder)
                            }
                            Text(
                                modifier = Modifier.padding(horizontal = 8.dp),
                                text = stringResource(id = R.string.sort_songs_by),
                                style = MaterialTheme.typography.titleLarge
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            SongSortOrder.allTypes.forEachIndexed { index, choice ->
                                Row(
                                    Modifier
                                        .fillMaxWidth()
                                        .height(56.dp)
                                        .selectable(
                                            selected = (choice::class == newSortOrder::class),
                                            onClick = {
                                                newSortOrder = when (choice) {
                                                    is SongSortOrder.Album -> choice.copy(
                                                        songSortOrder.isAscending
                                                    )

                                                    is SongSortOrder.AlbumArtist -> choice.copy(
                                                        songSortOrder.isAscending
                                                    )

                                                    is SongSortOrder.Composer -> choice.copy(
                                                        songSortOrder.isAscending
                                                    )

                                                    is SongSortOrder.DateModified -> choice.copy(
                                                        songSortOrder.isAscending
                                                    )

                                                    is SongSortOrder.Duration -> choice.copy(
                                                        songSortOrder.isAscending
                                                    )

                                                    is SongSortOrder.Name -> choice.copy(
                                                        songSortOrder.isAscending
                                                    )

                                                    is SongSortOrder.SongArtist -> choice.copy(
                                                        songSortOrder.isAscending
                                                    )

                                                    is SongSortOrder.Year -> choice.copy(
                                                        songSortOrder.isAscending
                                                    )
                                                }
                                            },
                                            role = Role.RadioButton
                                        )
                                        .padding(horizontal = 16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = (choice::class == newSortOrder::class),
                                        onClick = null
                                    )
                                    Text(
                                        text = stringResource(id = choice.toStringResource()),
                                        style = MaterialTheme.typography.bodyLarge,
                                        modifier = Modifier.padding(start = 16.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                                val orderChanger =
                                    { choice: SongSortOrder, newIsAscending: Boolean ->
                                        when (choice) {
                                            is SongSortOrder.Album -> choice.copy(
                                                newIsAscending
                                            )

                                            is SongSortOrder.AlbumArtist -> choice.copy(
                                                newIsAscending
                                            )

                                            is SongSortOrder.Composer -> choice.copy(
                                                newIsAscending
                                            )

                                            is SongSortOrder.DateModified -> choice.copy(
                                                newIsAscending
                                            )

                                            is SongSortOrder.Duration -> choice.copy(
                                                newIsAscending
                                            )

                                            is SongSortOrder.Name -> choice.copy(
                                                newIsAscending
                                            )

                                            is SongSortOrder.SongArtist -> choice.copy(
                                                newIsAscending
                                            )

                                            is SongSortOrder.Year -> choice.copy(
                                                newIsAscending
                                            )
                                        }
                                    }
                                SegmentedButton(
                                    onClick = { newSortOrder = orderChanger(songSortOrder, true) },
                                    selected = songSortOrder.isAscending,
                                    shape = SegmentedButtonDefaults.itemShape(
                                        index = 0,
                                        count = 2
                                    )
                                ) {
                                    Text(
                                        text = "Ascending"
                                    )
                                }
                                SegmentedButton(
                                    onClick = {
                                        newSortOrder = orderChanger(songSortOrder, false)
                                    },
                                    selected = !songSortOrder.isAscending,
                                    shape = SegmentedButtonDefaults.itemShape(
                                        index = 1,
                                        count = 2
                                    )
                                ) {
                                    Text(
                                        text = "Descending"
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                TextButton(onClick = { showOverflowMenu = false }) {
                                    Text(text = "Cancel")
                                }
                                TextButton(onClick = {
                                    songSortOrder = newSortOrder; showOverflowMenu = false
                                }) {
                                    Text(text = "OK")
                                }
                            }
                        }
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
        songs?.let { songsNotNull ->
            if (LocalConfiguration.current.orientation == Configuration.ORIENTATION_PORTRAIT)
                LazyColumn(
                    modifier = Modifier.padding(innerPadding),
                    verticalArrangement = Arrangement.spacedBy(4.dp)

                ) {
                    songsNotNull.forEach { (header, itemList) ->
                        stickyHeader {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        TopAppBarDefaults.topAppBarColors().scrolledContainerColor//.copy(alpha = 0.8f)
                                    )
                                    .padding(8.dp)

                            ) {
                                Text(text = header)
                            }
                        }
                        items(itemList) { song ->
                            MediaItemListCard(
                                imageModel = ContentUris.withAppendedId(
                                    "content://media/external/audio/albumart".toUri(),
                                    song.albumId
                                ),
                                title = song.title,
                                subtitle = song.artistNames.joinToString(", "),
                                onClick = { onSongClick(song) }
                            )
                        }
                    }
                }
            else
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    songsNotNull.forEach { (header, itemList) ->
                        stickyHeader {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        TopAppBarDefaults.topAppBarColors().scrolledContainerColor//.copy(alpha = 0.8f)
                                    )
                                    .padding(8.dp)

                            ) {
                                Text(text = header)
                            }
                        }
                        items(itemList) { song ->
                            MediaItemListCard(
                                imageModel = ContentUris.withAppendedId(
                                    "content://media/external/audio/albumart".toUri(),
                                    song.albumId
                                ),
                                title = song.title,
                                subtitle = song.artistNames.joinToString(", "),
                                onClick = { onSongClick(song) }
                            )
                        }
                    }

//                    items(songsNotNull) { song ->
//                        MediaItemGridCard(
//                            imageModel = ContentUris.withAppendedId(
//                                "content://media/external/audio/albumart".toUri(),
//                                song.albumId
//                            ),
//                            title = song.title,
//                            subtitle = song.artistNames.joinToString(", "),
//                            onPlay = { onSongClick(song) },
//                            onClick = { onSongClick(song) }
//                        )
//                    }
                }
        } ?: Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "Error")
        }
    }
}


@Composable
@Preview
fun preview() {
    AppTheme {
        Surface(
            modifier = Modifier
                .fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {

            }
        }
    }
}