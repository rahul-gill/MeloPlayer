//package meloplayer.app.ui.comps.list
//
//import androidx.compose.foundation.background
//import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
//import androidx.compose.foundation.gestures.scrollBy
//import androidx.compose.foundation.layout.Arrangement
//import androidx.compose.foundation.layout.Row
//import androidx.compose.foundation.layout.fillMaxSize
//import androidx.compose.foundation.layout.padding
//import androidx.compose.material3.ExperimentalMaterial3Api
//import androidx.compose.material3.Text
//import androidx.compose.material3.TopAppBarDefaults
//import androidx.compose.runtime.Composable
//import androidx.compose.runtime.LaunchedEffect
//import androidx.compose.runtime.MutableState
//import androidx.compose.runtime.mutableFloatStateOf
//import androidx.compose.runtime.remember
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.geometry.Offset
//import androidx.compose.ui.hapticfeedback.HapticFeedback
//import androidx.compose.ui.hapticfeedback.HapticFeedbackType
//import androidx.compose.ui.input.pointer.pointerInput
//import androidx.compose.ui.platform.LocalDensity
//import androidx.compose.ui.platform.LocalHapticFeedback
//import androidx.compose.ui.semantics.onLongClick
//import androidx.compose.ui.semantics.semantics
//import androidx.compose.ui.unit.dp
//import androidx.compose.ui.unit.round
//import androidx.compose.ui.unit.toIntRect
//import kotlinx.coroutines.delay
//import kotlinx.coroutines.isActive
//import meloplayer.core.ui.components.MediaItemGridCard
//import meloplayer.core.ui.components.MediaItemListCard
//import meloplayer.core.ui.components.nowplaying.isLayoutTypeVertical
//import ua.hospes.lazygrid.GridCells
//import ua.hospes.lazygrid.LazyGridState
//import ua.hospes.lazygrid.LazyVerticalGrid
//import ua.hospes.lazygrid.items
//import ua.hospes.lazygrid.rememberLazyGridState
//
//@OptIn(ExperimentalMaterial3Api::class)
//@Composable
//fun <T> ResponsiveListGrid(
//    modifier: Modifier = Modifier,
//    itemsGrouped: Map<String, List<Pair<Int, T>>>,
//    selectedIds: Set<Int>,
//    setSelectedIds: (Set<Int>) -> Unit,
//    onItemClick: (T) -> Unit,
//
//) {
//    val state = rememberLazyGridState()
//    val autoScrollSpeed = remember { mutableFloatStateOf(0f) }
//    LaunchedEffect(autoScrollSpeed.floatValue) {
//        if (autoScrollSpeed.floatValue != 0f) {
//            while (isActive) {
//                state.scrollBy(autoScrollSpeed.floatValue)
//                delay(10)
//            }
//        }
//    }
//
//
//    LazyVerticalGrid(
//        modifier = Modifier
//            .gridSelectionDragHandler(
//                lazyGridState = state,
//                haptics = LocalHapticFeedback.current,
//                selectedIds = selectedIds,
//                autoScrollSpeed = autoScrollSpeed,
//                autoScrollThreshold = with(LocalDensity.current) { 40.dp.toPx() },
//                setSelectedIds = setSelectedIds
//            )
//            .then(modifier),
//        state = state,
//        columns = GridCells.Fixed(if (isLayoutTypeVertical()) 1 else 6),
//        horizontalArrangement = Arrangement.spacedBy(2.dp),
//        verticalArrangement = Arrangement.spacedBy(6.dp)
//    ) {
//        itemsGrouped.forEach { (header, itemList) ->
//            stickyHeader {
//                Row(
//                    modifier = Modifier
//                        .fillMaxSize()
//                        .background(
//                            TopAppBarDefaults.topAppBarColors().scrolledContainerColor
//                        )
//                        .padding(8.dp)
//
//                ) {
//                    Text(text = header)
//                }
//            }
//            items(itemList, key = { it.first }) { indexSongPair ->
//                val isSelected = if (selectedIds.isEmpty()) {
//                    null
//                } else {
//                    selectedIds.contains(indexSongPair.first)
//                }
//                if (isLayoutTypeVertical()) {
//                    MediaItemListCard(
//                        modifier = Modifier
//                            .semantics {
//                                if (selectedIds.isEmpty()) {
//                                    onLongClick("Select") {
//                                        setSelectedIds(selectedIds + indexSongPair.first)
//                                        true
//                                    }
//                                }
//                            },
//                        imageModel = indexSongPair.second.song.coverImageUri,
//                        title = indexSongPair.second.song.title,
//                        subtitle = (indexSongPair.second.album?.title ?: "") + " - " + artists,
//                        onClick = {
//                            if (isSelected == null) {
//                                onItemClick(indexSongPair.second)
//                            } else if (!isSelected) {
//                                setSelectedIds(selectedIds + indexSongPair.first)
//                            } else {
//                                setSelectedIds(selectedIds - indexSongPair.first)
//                            }
//                        },
//                        isSelected = isSelected
//                    )
//                } else {
//                    MediaItemGridCard(
//                        modifier = Modifier
//                            .semantics {
//                                if (selectedIds.isEmpty()) {
//                                    onLongClick("Select") {
//                                        setSelectedIds(selectedIds + indexSongPair.first)
//                                        true
//                                    }
//                                }
//                            },
//                        imageModel = indexSongPair.second.song.coverImageUri,
//                        title = indexSongPair.second.song.title,
//                        subtitle = (indexSongPair.second.album?.title ?: "") + " - " + artists,
//                        onClick = {
//                            if (isSelected == null) {
//                                onItemClick(indexSongPair.second)
//                            } else if (!isSelected) {
//                                setSelectedIds(selectedIds + indexSongPair.first)
//                            } else {
//                                setSelectedIds(selectedIds - indexSongPair.first)
//                            }
//                        },
//                        isSelected = isSelected
//                    )
//                }
//            }
//        }
//    }
//}
//
//
//
//private fun Modifier.gridSelectionDragHandler(
//    lazyGridState: LazyGridState,
//    haptics: HapticFeedback,
//    selectedIds: Set<Int>,
//    autoScrollSpeed: MutableState<Float>,
//    autoScrollThreshold: Float,
//    setSelectedIds: (Set<Int>) -> Unit
//) = pointerInput(Unit) {
//    fun LazyGridState.gridItemKeyAtPosition(hitPoint: Offset): Int? =
//        layoutInfo.visibleItemsInfo.find { itemInfo ->
//            itemInfo.size.toIntRect().contains(hitPoint.round() - itemInfo.offset)
//        }?.key as? Int
//
//    var initialKey: Int? = null
//    var currentKey: Int? = null
//    detectDragGesturesAfterLongPress(
//        onDragStart = { offset ->
//            lazyGridState.gridItemKeyAtPosition(offset)?.let { key ->
//                if (!selectedIds.contains(key)) {
//                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
//                    initialKey = key
//                    currentKey = key
//                    setSelectedIds(selectedIds + key)
//                }
//            }
//        },
//        onDragCancel = { initialKey = null; autoScrollSpeed.value = 0f },
//        onDragEnd = { initialKey = null; autoScrollSpeed.value = 0f },
//        onDrag = { change, _ ->
//            if (initialKey != null) {
//                val distFromBottom =
//                    lazyGridState.layoutInfo.viewportSize.height - change.position.y
//                val distFromTop = change.position.y
//                autoScrollSpeed.value = when {
//                    distFromBottom < autoScrollThreshold -> autoScrollThreshold - distFromBottom
//                    distFromTop < autoScrollThreshold -> -(autoScrollThreshold - distFromTop)
//                    else -> 0f
//                }
//
//                lazyGridState.gridItemKeyAtPosition(change.position)?.let { key ->
//                    if (currentKey != key) {
//                        setSelectedIds(
//                            selectedIds.minus(initialKey!!..currentKey!!)
//                                .minus(currentKey!!..initialKey!!)
//                                .plus(initialKey!!..key)
//                                .plus(key..initialKey!!)
//                        )
//                        currentKey = key
//                    }
//                }
//            }
//        }
//    )
//}
