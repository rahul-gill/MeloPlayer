package meloplayer.app.ui.screen

import android.content.ContentUris
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.onLongClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.round
import androidx.compose.ui.unit.toIntRect
import androidx.core.net.toUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import meloplayer.app.R
import meloplayer.app.playbackx.PlaybackCommand
import meloplayer.app.playbackx.PlaybackStateX
import meloplayer.app.playbackx.glue.PlaybackGlue
import meloplayer.core.store.model.MediaStoreSong
import meloplayer.core.store.model.SongSortOrder
import meloplayer.core.store.model.toStringResource
import meloplayer.core.store.repo.SongsRepository
import meloplayer.core.ui.components.MediaItemGridCard
import meloplayer.core.ui.components.base.LargeTopAppBar
import ua.hospes.lazygrid.GridCells
import ua.hospes.lazygrid.LazyGridState
import ua.hospes.lazygrid.LazyVerticalGrid
import ua.hospes.lazygrid.items
import ua.hospes.lazygrid.rememberLazyGridState
import java.time.format.DateTimeFormatter


private val monthFormatter by lazy {
    DateTimeFormatter.ofPattern("MMMM yyyy")
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongListScreen(
) {
    var songSortOrder: SongSortOrder by remember {
        mutableStateOf(SongSortOrder.DateModified())
    }
    val scope = rememberCoroutineScope()
    var songsDir by remember {
        mutableStateOf<List<MediaStoreSong>>(listOf())
    }
    var songs by remember {
        mutableStateOf<Map<String, List<Pair<Int, MediaStoreSong>>>?>(
            mapOf()
        )
    }
    val selectedIds = rememberSaveable { mutableStateOf(emptySet<Int>()) }
    val inSelectionMode by remember { derivedStateOf { selectedIds.value.isNotEmpty() } }


    val onSongClick = { song: MediaStoreSong ->
        if (!songs.isNullOrEmpty()) {
            if(PlaybackGlue.instance.playbackManagerX.playbackStateX.value is PlaybackStateX.Empty) {
                PlaybackGlue.instance.playbackManagerX.handleCommand(PlaybackCommand.AddItemsToQueue(
                    items = songsDir.map { it.id }
                ))
            } else {
                PlaybackGlue.instance.playbackManagerX.handleCommand(PlaybackCommand.AddItemsToQueue(
                    items = listOf(song.id)
                ))
            }

            PlaybackGlue.instance.playbackManagerX.handleCommand(PlaybackCommand.SetCurrentQueueItemIndex(
                index = songsDir.indexOfFirst { it.id == song.id }
            ))
            PlaybackGlue.instance.playbackManagerX.handleCommand(PlaybackCommand.Play)

            //playbackManager?.startPlayingWithQueueInit(songsDir.map { it.id })
        }
        //playbackManager?.playWithId(song.id)

    }
//    DisposableEffect(key1 = Unit) {
////        onDispose {
////            playbackX.release()
////        }
//    }
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
                    .mapIndexed { index, mediaStoreSong -> Pair(index, mediaStoreSong) }
                    .groupBy { sortKeyGetter(it.second) }
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
                selectionInfoContent = {
                    if (inSelectionMode) {
                        TopAppBar(
                            title = { "${selectedIds.value.size} Selected" },
                            navigationIcon = {
                                IconButton(
                                    onClick = { selectedIds.value = setOf() }
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = stringResource(id = R.string.clear_selection)
                                    )
                                }
                            },
                            actions = {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Sort,
                                    contentDescription = "TODO"
                                )
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.PlaylistAdd,
                                    contentDescription = "TODO"
                                )
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = "TODO"
                                )
                            }
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showOverflowMenu = true }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Sort,
                            contentDescription = "TODO"
                        )
                    }
                    if (showOverflowMenu) {
                        SongSortOrderSelectSheet(
                            songSortOrder = songSortOrder,
                            onDismiss = { showOverflowMenu = false },
                            onSelectSongSortOrder = { new -> songSortOrder = new }
                        )
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
//            if (LocalConfiguration.current.orientation == Configuration.ORIENTATION_PORTRAIT)
//                LazyColumn(
//                    modifier = Modifier.padding(innerPadding),
//                    verticalArrangement = Arrangement.spacedBy(4.dp)
//
//                ) {
//                    songsNotNull.forEach { (header, itemList) ->
//                        stickyHeader {
//                            Row(
//                                modifier = Modifier
//                                    .fillMaxSize()
//                                    .background(
//                                        TopAppBarDefaults.topAppBarColors().scrolledContainerColor//.copy(alpha = 0.8f)
//                                    )
//                                    .padding(8.dp)
//
//                            ) {
//                                Text(text = header)
//                            }
//                        }
//                        items(itemList) { song ->
//                            MediaItemListCard(
//                                imageModel = ContentUris.withAppendedId(
//                                    "content://media/external/audio/albumart".toUri(),
//                                    song.albumId
//                                ),
//                                title = song.title,
//                                subtitle = song.artistNames.joinToString(", "),
//                                onClick = { onSongClick(song) }
//                            )
//                        }
//                    }
//                }
//            else
            SongsResponsiveGrid(

                modifier = Modifier.padding(innerPadding),
                selectedIds, songsNotNull, inSelectionMode
            ) { onSongClick(it) }
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
@OptIn(ExperimentalMaterial3Api::class)
private fun SongSortOrderSelectSheet(
    songSortOrder: SongSortOrder,
    onDismiss: () -> Unit,
    onSelectSongSortOrder: (SongSortOrder) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        sheetState = sheetState,
        onDismissRequest = onDismiss
    ) {
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
                                    newSortOrder.isAscending
                                )

                                is SongSortOrder.AlbumArtist -> choice.copy(
                                    newSortOrder.isAscending
                                )

                                is SongSortOrder.Composer -> choice.copy(
                                    newSortOrder.isAscending
                                )

                                is SongSortOrder.DateModified -> choice.copy(
                                    newSortOrder.isAscending
                                )

                                is SongSortOrder.Duration -> choice.copy(
                                    newSortOrder.isAscending
                                )

                                is SongSortOrder.Name -> choice.copy(
                                    newSortOrder.isAscending
                                )

                                is SongSortOrder.SongArtist -> choice.copy(
                                    newSortOrder.isAscending
                                )

                                is SongSortOrder.Year -> choice.copy(
                                    newSortOrder.isAscending
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
                onClick = { newSortOrder = orderChanger(newSortOrder, true) },
                selected = newSortOrder.isAscending,
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
                    newSortOrder = orderChanger(newSortOrder, false)
                },
                selected = !newSortOrder.isAscending,
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
            TextButton(onClick = onDismiss) {
                Text(text = "Cancel")
            }
            TextButton(onClick = {
                onSelectSongSortOrder(newSortOrder)
                onDismiss()
            }) {
                Text(text = "OK")
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun SongsResponsiveGrid(
    modifier: Modifier = Modifier,
    selectedIds: MutableState<Set<Int>>,
    songsNotNull: Map<String, List<Pair<Int, MediaStoreSong>>>,
    inSelectionMode: Boolean,
    onSongClick: (MediaStoreSong) -> Unit
) {
    val state = rememberLazyGridState()
    val autoScrollSpeed = remember { mutableFloatStateOf(0f) }
    LaunchedEffect(autoScrollSpeed.floatValue) {
        if (autoScrollSpeed.floatValue != 0f) {
            while (isActive) {
                state.scrollBy(autoScrollSpeed.floatValue)
                delay(10)
            }
        }
    }


    LazyVerticalGrid(
        modifier = Modifier
            .gridSelectionDragHandler(
                lazyGridState = state,
                haptics = LocalHapticFeedback.current,
                selectedIds = selectedIds,
                autoScrollSpeed = autoScrollSpeed,
                autoScrollThreshold = with(LocalDensity.current) { 40.dp.toPx() }
            )
            .then(modifier),
        state = state,
        columns = GridCells.Fixed(3),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
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
            items(itemList, key = { it.first }) { indexSongPair ->
                val isSelected = if (!inSelectionMode) {
                    null
                } else {
                    selectedIds.value.contains(indexSongPair.first)
                }
                MediaItemGridCard(
                    modifier = Modifier
                        .semantics {
                            if (!inSelectionMode) {
                                onLongClick("Select") {
                                    selectedIds.value += indexSongPair.first
                                    true
                                }
                            }
                        },
                    imageModel = ContentUris.withAppendedId(
                        "content://media/external/audio/albumart".toUri(),
                        indexSongPair.second.albumId
                    ),
                    title = indexSongPair.second.title,
                    subtitle = indexSongPair.second.artistNames.joinToString(", "),
                    onClick = {
                        if (isSelected == null) {
                            onSongClick(indexSongPair.second)
                        } else if (!isSelected) {
                            selectedIds.value += indexSongPair.first
                        } else {
                            selectedIds.value -= indexSongPair.first
                        }
                    },
                    isSelected = isSelected
                )
            }
        }
    }
}


private fun Modifier.gridSelectionDragHandler(
    lazyGridState: LazyGridState,
    haptics: HapticFeedback,
    selectedIds: MutableState<Set<Int>>,
    autoScrollSpeed: MutableState<Float>,
    autoScrollThreshold: Float
) = pointerInput(Unit) {
    fun LazyGridState.gridItemKeyAtPosition(hitPoint: Offset): Int? =
        layoutInfo.visibleItemsInfo.find { itemInfo ->
            itemInfo.size.toIntRect().contains(hitPoint.round() - itemInfo.offset)
        }?.key as? Int

    var initialKey: Int? = null
    var currentKey: Int? = null
    detectDragGesturesAfterLongPress(
        onDragStart = { offset ->
            lazyGridState.gridItemKeyAtPosition(offset)?.let { key ->
                if (!selectedIds.value.contains(key)) {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    initialKey = key
                    currentKey = key
                    selectedIds.value += key
                }
            }
        },
        onDragCancel = { initialKey = null; autoScrollSpeed.value = 0f },
        onDragEnd = { initialKey = null; autoScrollSpeed.value = 0f },
        onDrag = { change, _ ->
            if (initialKey != null) {
                val distFromBottom =
                    lazyGridState.layoutInfo.viewportSize.height - change.position.y
                val distFromTop = change.position.y
                autoScrollSpeed.value = when {
                    distFromBottom < autoScrollThreshold -> autoScrollThreshold - distFromBottom
                    distFromTop < autoScrollThreshold -> -(autoScrollThreshold - distFromTop)
                    else -> 0f
                }

                lazyGridState.gridItemKeyAtPosition(change.position)?.let { key ->
                    if (currentKey != key) {
                        selectedIds.value = selectedIds.value
                            .minus(initialKey!!..currentKey!!)
                            .minus(currentKey!!..initialKey!!)
                            .plus(initialKey!!..key)
                            .plus(key..initialKey!!)
                        currentKey = key
                    }
                }
            }
        }
    )
}
