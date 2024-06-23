package meloplayer.app.playback

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import meloplayer.core.prefs.Preference


enum class LoopMode {
    None, One, All;

    fun toggleNextValue() = when (this) {
        None -> All
        One -> None
        All -> One
    }
}

enum class QueueEvent {
    CurrentSongChanged,
    QueueCleared,
    QueueUpdated
}
//TODO; currentItem and SongIndex not in sync
interface PlaybackQueueManager {
    val currentQueue: StateFlow<List<Long>>

    val currentItem: Flow<Long?>

    /**
     * Even if song is same, this index can change,, for example when you shuffle
     * If you want same value for same song, use [currentItem]
     */
    val currentSongIndex: StateFlow<Int?>

    /**
     * Add songs to end of queue or at specified index.
     * Result will be same regardless of shuffle mode.
     * Does nothing if songIds is null or index is out of bounds
     */
    fun addSongs(songIds: List<Long>, index: Int? = null)
    fun reset()

    /**
     * Do nothing if index out of bounds
     */
    fun removeAtIndex(index: Int)

    /**
     * Ignore the indexes which are out of bounds
     */
    fun removeAtIndex(indices: List<Int>)

    /**
     * Do nothing if index out of bounds
     */
    fun setCurrentSongIndex(index: Int)

    fun moveTrack(fromIndex: Int, toIndex: Int)

    fun nextTrackIndex(): Int?
    fun previousTrackIndex(): Int?

    companion object {
        fun getImpl(
            scope: CoroutineScope,
            loopMode: Preference<LoopMode>,
            shuffleEnabled: Preference<Boolean>,
            onPlaybackQueueEvent: (PlaybackQueueManager, QueueEvent) -> Unit,
        ): PlaybackQueueManager =
            PlaybackQueueManagerImpl(scope, loopMode, shuffleEnabled, onPlaybackQueueEvent)
    }
}

private class PlaybackQueueManagerImpl(
    scope: CoroutineScope,
    private val loopMode: Preference<LoopMode>,
    private val shuffleEnabled: Preference<Boolean>,
    onEvent: (PlaybackQueueManager, QueueEvent) -> Unit
) : PlaybackQueueManager {


    private val onEvent: (QueueEvent) -> Unit = { onEvent(this, it) }

    private val _currentQueue = MutableStateFlow(listOf<Long>())
    private val _currentSongIndex = MutableStateFlow<Int?>(null)
    private val originalQueue = mutableListOf<Long>()
    override val currentQueue: StateFlow<List<Long>>
        get() = _currentQueue

    override val currentItem: Flow<Long?> =
        _currentQueue.combine(_currentSongIndex) { q, i -> getCurrentItemValue(q, i) }


    init {
        shuffleEnabled.observableValue
            .onEach { setShuffleMode(it) }
            .launchIn(scope)
    }

    override val currentSongIndex: StateFlow<Int?>
        get() = _currentSongIndex


    private fun getCurrentItemValue(q: List<Long>, i: Int?): Long? {

        return if (i == null) null else q.getOrNull(i)
    }

    override fun addSongs(songIds: List<Long>, index: Int?) {

        val invalidArgs = songIds.isEmpty() ||
                index != null && (index >= _currentQueue.value.size || index < 0)
        if (invalidArgs) {
            return
        }
        if (index != null) {
            originalQueue.addAll(index, songIds)
            _currentQueue.update { it.toMutableList().apply { addAll(index, songIds) } }
            _currentSongIndex.value?.let { currIndex ->
                if (index <= currIndex) {
                    _currentSongIndex.update { currIndex + songIds.size }
                }
            } ?: run {
                _currentSongIndex.update { 0 }
                onEvent(QueueEvent.CurrentSongChanged)
            }
        } else {
            originalQueue.addAll(songIds)
            _currentQueue.update { it.toMutableList().apply { addAll(songIds) } }
            if (_currentSongIndex.value == null) {
                _currentSongIndex.update { 0 }
            }
        }
        onEvent(QueueEvent.QueueUpdated)

    }

    override fun reset() {
        originalQueue.clear()
        _currentQueue.update { listOf() }
        _currentSongIndex.update { null }
        onEvent(QueueEvent.QueueCleared)

    }

    override fun removeAtIndex(index: Int) {
        if (index >= _currentQueue.value.size || index < 0) {
            return
        }
        val newQueue = _currentQueue.value.toMutableList()
        val deletedSongId = newQueue.removeAt(index)
        _currentQueue.update { newQueue }
        originalQueue.removeIf { it == deletedSongId }
        _currentSongIndex.value?.let { currIndex ->
            if (index < currIndex) {
                _currentSongIndex.update { currIndex - 1 }
            } else if (index == currIndex) {
                if (currIndex == 0)
                    _currentSongIndex.update { null }
                onEvent(QueueEvent.CurrentSongChanged)
            }
        }
        onEvent(QueueEvent.QueueUpdated)
    }

    override fun removeAtIndex(indices: List<Int>) {
        val sortedIndices = indices.sortedDescending()
        val songIds = sortedIndices.mapNotNull { index ->
            if (index >= _currentQueue.value.size) null
            else _currentQueue.value[index]
        }
        val currentSongId = _currentSongIndex.value?.let { _currentQueue.value[it] }
        _currentQueue.update { prev ->
            prev.toMutableList().apply { removeIf { songIds.contains(it) } }
        }
        originalQueue.removeIf { songIds.contains(it) }
        val newCurrSongIndex = originalQueue.indexOf(currentSongId)
        if (_currentSongIndex.value != newCurrSongIndex) {
            onEvent(QueueEvent.CurrentSongChanged)
        }
        _currentSongIndex.update {
            if (newCurrSongIndex == -1) null
            else newCurrSongIndex
        }
        onEvent(QueueEvent.QueueUpdated)
    }

    fun toggleLoopMode() {
        val newLoopMode = when (loopMode.value) {
            LoopMode.None -> LoopMode.All
            LoopMode.One -> LoopMode.None
            LoopMode.All -> LoopMode.One
        }
        loopMode.setValue(newLoopMode)

    }

    fun setShuffleMode(newShuffleOn: Boolean) {
        val currentSongIdIndex = _currentSongIndex.value
        _currentSongIndex.update {
            if (newShuffleOn) {
                val newQueue = originalQueue.toMutableList()
                val currentSongId = currentSongIdIndex?.run { newQueue.removeAt(this) }
                newQueue.shuffle()
                if (currentSongId != null) {
                    newQueue.add(0, currentSongId.toLong())
                }
                _currentQueue.update { newQueue }
                0
            } else {
                val currentSongId = currentSongIdIndex?.run { _currentQueue.value.getOrNull(this) }
                _currentQueue.update { originalQueue }
                val newCurrIndex = _currentQueue.value.indexOfFirst { it == currentSongId }
                if (newCurrIndex == -1) null else newCurrIndex
            }
        }
        onEvent(QueueEvent.CurrentSongChanged)
        onEvent(QueueEvent.QueueUpdated)
    }

    override fun setCurrentSongIndex(index: Int) {
        _currentSongIndex.update { index }
        onEvent(QueueEvent.CurrentSongChanged)
    }

    override fun moveTrack(fromIndex: Int, toIndex: Int) {

        if (fromIndex < 0 || toIndex < 0 || fromIndex >= _currentQueue.value.size
            || toIndex >= _currentQueue.value.size || fromIndex == toIndex
        ) {
            return
        }
        val currentQueue = _currentQueue.value.toMutableList()
        val track = currentQueue.removeAt(fromIndex)
        currentQueue.add(toIndex, track)
        val newCurrentIndex = when (val prev = _currentSongIndex.value) {
            null -> null
            fromIndex -> toIndex
            in (fromIndex + 1)..toIndex -> prev - 1
            in toIndex until fromIndex -> prev + 1
            else -> prev
        }

        if (shuffleEnabled.value) {
            _currentQueue.update { currentQueue }
        } else {
            originalQueue.clear()
            originalQueue.addAll(currentQueue)
            _currentQueue.update { currentQueue }
        }
        _currentSongIndex.update { newCurrentIndex }
    }

    override fun nextTrackIndex(): Int? {
        val currIndex = _currentSongIndex.value
        val queueSize = _currentQueue.value.size
        return when {
            currIndex == null -> null
            loopMode.value == LoopMode.One -> currIndex
            currIndex + 1 < queueSize -> currIndex + 1
            loopMode.value == LoopMode.All -> 0
            else -> null
        }
    }

    override fun previousTrackIndex(): Int? {
        val currIndex = _currentSongIndex.value
        val queueSize = _currentQueue.value.size
        return when {
            currIndex == null -> null
            loopMode.value == LoopMode.One -> currIndex
            currIndex - 1 < queueSize -> currIndex - 1
            loopMode.value == LoopMode.All -> queueSize - 1
            else -> null
        }
    }

}