package meloplayer.app.playback

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import meloplayer.core.prefs.Preference


enum class LoopMode {
    None, One, All
}

enum class QueueEvent {
    CurrentSongChanged,
    QueueCleared,
    QueueUpdated
}

interface PlaybackQueueManager {
    val currentQueue: StateFlow<List<Long>>
    val currentSongIndex: StateFlow<Int?>
    val currentLoopMode: Preference<LoopMode>
    val currentShuffleMode: Preference<Boolean>
    fun addSong(songId: Long, index: Int? = null)
    fun addSongs(songIds: List<Long>, index: Int? = null)
    fun reset()
    fun removeAtIndex(index: Int)
    fun removeAtIndex(indices: List<Int>)
    fun toggleLoopMode()
    fun toggleShuffleMode()
    fun setCurrentSongIndex(index: Int)

    companion object {
        fun getImpl(
            loopMode: Preference<LoopMode>,
            shuffleEnabled: Preference<Boolean>,
            onPlaybackQueueEvent: (PlaybackQueueManager, QueueEvent) -> Unit,
        ): PlaybackQueueManager =
            PlaybackQueueManagerImpl(loopMode, shuffleEnabled, onPlaybackQueueEvent)
    }
}

private class PlaybackQueueManagerImpl(
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
    override val currentSongIndex: StateFlow<Int?>
        get() = _currentSongIndex
    override val currentLoopMode = loopMode
    override val currentShuffleMode = shuffleEnabled

    override fun addSong(songId: Long, index: Int?) {
        addSongs(listOf(songId), index)
    }

    override fun addSongs(songIds: List<Long>, index: Int?) {
        if (index != null) {
            originalQueue.addAll(index, songIds)
            _currentQueue.update { it.toMutableList().apply { addAll(songIds) } }
            _currentSongIndex.value?.let { currIndex ->
                if (index <= currIndex) {
                    _currentSongIndex.update { currIndex + songIds.size }
                    onEvent(QueueEvent.CurrentSongChanged)
                }
            } ?: run {
                _currentSongIndex.update { 0 }
                onEvent(QueueEvent.CurrentSongChanged)
            }
        } else {
            originalQueue.addAll(songIds)
            _currentQueue.update { it.toMutableList().apply { addAll(songIds) } }
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
        if (index >= _currentQueue.value.size) {
            return
        }
        val newQueue = _currentQueue.value.toMutableList()
        val deletedSongId = newQueue.removeAt(index)
        _currentQueue.update { newQueue }
        originalQueue.removeIf { it == deletedSongId }
        _currentSongIndex.value?.let { currIndex ->
            if (index < currIndex) {
                _currentSongIndex.update { currIndex - 1 }
                onEvent(QueueEvent.CurrentSongChanged)
            } else if (index == currIndex && currIndex == 0) {
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

    override fun toggleLoopMode() {
        val newLoopMode = when (loopMode.value) {
            LoopMode.None -> LoopMode.All
            LoopMode.One -> LoopMode.None
            LoopMode.All -> LoopMode.One
        }
        loopMode.setValue(newLoopMode)

    }

    override fun toggleShuffleMode() {
        shuffleEnabled.setValue(!shuffleEnabled.value)
        val newShuffleOn = shuffleEnabled.value
        val currentSongId = _currentSongIndex.value
        _currentSongIndex.update {
            if (newShuffleOn) {
                val newQueue = originalQueue.toMutableList()
                _currentSongIndex.value?.let { newQueue.removeAt(it) }
                newQueue.shuffle()
                if (currentSongId != null) {
                    newQueue.add(0, currentSongId.toLong())
                }
                _currentQueue.update { newQueue }
                0
            } else {
                _currentQueue.update { originalQueue }
                val newCurrIndex = _currentQueue.value.indexOfFirst { it.toInt() == currentSongId }
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

}