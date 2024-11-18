package meloplayer.app.parts.songs.list

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import meloplayer.app.R
import meloplayer.app.db.entities.derived.SongWithAllDetails
import meloplayer.core.ui.components.MediaItemGridCard
import meloplayer.core.ui.components.MediaItemListCard
import meloplayer.core.ui.components.base.LargeTopAppBar
import meloplayer.core.ui.components.nowplaying.isLayoutTypeVertical
import org.koin.androidx.compose.koinViewModel
import ua.hospes.lazygrid.GridCells
import ua.hospes.lazygrid.LazyGridState
import ua.hospes.lazygrid.LazyVerticalGrid
import ua.hospes.lazygrid.items
import ua.hospes.lazygrid.rememberLazyGridState
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter


val monthFormatter by lazy {
    DateTimeFormatter.ofPattern("MMMM yyyy")
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongListScreen(
    viewModel: SongListViewModel = koinViewModel()
) {
    val onSongClick = { song: SongWithAllDetails ->
//        if (!songs.isNullOrEmpty()) {
//            if (PlaybackGlue.instance.playbackManagerX.playbackStateX.value is PlaybackStateX.Empty) {
//                PlaybackGlue.instance.playbackManagerX.handleCommand(PlaybackCommand.AddItemsToQueue(
//                    items = songsDir.map { it.songID }
//                ))
//            } else {
//                PlaybackGlue.instance.playbackManagerX.handleCommand(
//                    PlaybackCommand.AddItemsToQueue(
//                        items = listOf(song.songID)
//                    )
//                )
//            }
//
//            PlaybackGlue.instance.playbackManagerX.handleCommand(PlaybackCommand.SetCurrentQueueItemIndex(
//                index = songsDir.indexOfFirst { it.songID == song.songID }
//            ))
//            PlaybackGlue.instance.playbackManagerX.handleCommand(PlaybackCommand.Play)
//        }
    }


    val scrollBehavior =
        TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    var showOverflowMenu by remember {
        mutableStateOf(false)
    }

    val selectedSongIndexed by viewModel.selectedSongsIndexes.collectAsStateWithLifecycle()
    val sortOrder by viewModel.songSortOrder.collectAsStateWithLifecycle()
    val songsList by viewModel.songs.collectAsStateWithLifecycle(initialValue = mapOf())

    var lastTime by remember {
        mutableStateOf<LocalDateTime?>(null)
    }

    LaunchedEffect(songsList) {
        if(lastTime == null){
            lastTime = LocalDateTime.now()
        } else {
            val now = LocalDateTime.now()
            lastTime = now
        }
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
                    if (selectedSongIndexed.isNotEmpty()) {
                        TopAppBar(
                            title = { "${selectedSongIndexed.size} Selected" },
                            navigationIcon = {
                                IconButton(
                                    onClick = { viewModel.clearSelection() }
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
                            songSortOrder = sortOrder,
                            onDismiss = { showOverflowMenu = false },
                            onSelectSongSortOrder = viewModel::setSortOrder
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = viewModel::onShuffleSongs) {
                Icon(
                    imageVector = Icons.Default.Shuffle,
                    contentDescription = stringResource(R.string.shuffle_songs_and_play)
                )
            }
        }
    ) { innerPadding ->
        SongsResponsiveGrid(
            modifier = Modifier.padding(innerPadding),
            selectedSongIndexed, songsList, selectedSongIndexed.isNotEmpty(), setSelectedIds = viewModel::setSelectedIds,
            onSongClick = onSongClick
        )
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


                                is SongSortOrder.DateModified -> choice.copy(
                                    newSortOrder.isAscending
                                )

                                is SongSortOrder.Duration -> choice.copy(
                                    newSortOrder.isAscending
                                )

                                is SongSortOrder.Name -> choice.copy(
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


                        is SongSortOrder.DateModified -> choice.copy(
                            newIsAscending
                        )

                        is SongSortOrder.Duration -> choice.copy(
                            newIsAscending
                        )

                        is SongSortOrder.Name -> choice.copy(
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
    selectedIds: Set<Int>,
    songsNotNull: Map<String, List<Pair<Int, SongWithAllDetails>>>,
    inSelectionMode: Boolean,
    onSongClick: (SongWithAllDetails) -> Unit,
    setSelectedIds: (Set<Int>) -> Unit
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
                autoScrollThreshold = with(LocalDensity.current) { 40.dp.toPx() },
                setSelectedIds = setSelectedIds
            )
            .then(modifier),
        state = state,
        columns = GridCells.Fixed(if (isLayoutTypeVertical()) 1 else 6),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        songsNotNull.forEach { (header, itemList) ->
            stickyHeader {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            TopAppBarDefaults.topAppBarColors().scrolledContainerColor
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
                    selectedIds.contains(indexSongPair.first)
                }
                val artists = remember(indexSongPair) {
                    var artists = ""
                    indexSongPair.second.artists
                        .filter { it.isSongArtist }
                        .forEach { artist -> artists += "${artist.name} | " }
                    artists
                }
                if (isLayoutTypeVertical()) {
                    MediaItemListCard(
                        modifier = Modifier
                            .semantics {
                                if (!inSelectionMode) {
                                    onLongClick("Select") {
                                        setSelectedIds(selectedIds + indexSongPair.first)
                                        true
                                    }
                                }
                            },
                        imageModel = indexSongPair.second.song.coverImageUri,
                        title = indexSongPair.second.song.title,
                        subtitle = (indexSongPair.second.album?.title ?: "") + " - " + artists,
                        onClick = {
                            if (isSelected == null) {
                                onSongClick(indexSongPair.second)
                            } else if (!isSelected) {
                                setSelectedIds(selectedIds + indexSongPair.first)
                            } else {
                                setSelectedIds(selectedIds - indexSongPair.first)
                            }
                        },
                        isSelected = isSelected
                    )
                } else {
                    MediaItemGridCard(
                        modifier = Modifier
                            .semantics {
                                if (!inSelectionMode) {
                                    onLongClick("Select") {
                                        setSelectedIds(selectedIds + indexSongPair.first)
                                        true
                                    }
                                }
                            },
                        imageModel = indexSongPair.second.song.coverImageUri,
                        title = indexSongPair.second.song.title,
                        subtitle = (indexSongPair.second.album?.title ?: "") + " - " + artists,
                        onClick = {
                            if (isSelected == null) {
                                onSongClick(indexSongPair.second)
                            } else if (!isSelected) {
                                setSelectedIds(selectedIds + indexSongPair.first)
                            } else {
                                setSelectedIds(selectedIds - indexSongPair.first)
                            }
                        },
                        isSelected = isSelected
                    )
                }
            }
        }
    }
}


private fun Modifier.gridSelectionDragHandler(
    lazyGridState: LazyGridState,
    haptics: HapticFeedback,
    selectedIds: Set<Int>,
    autoScrollSpeed: MutableState<Float>,
    autoScrollThreshold: Float,
    setSelectedIds: (Set<Int>) -> Unit
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
                if (!selectedIds.contains(key)) {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    initialKey = key
                    currentKey = key
                    setSelectedIds(selectedIds + key)
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
                        setSelectedIds(
                            selectedIds.minus(initialKey!!..currentKey!!)
                                .minus(currentKey!!..initialKey!!)
                                .plus(initialKey!!..key)
                                .plus(key..initialKey!!)
                        )
                        currentKey = key
                    }
                }
            }
        }
    )
}
