package meloplayer.app.ui.screen.songs

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import meloplayer.app.store.models.SongSortOrder
import meloplayer.app.store.repo.SongRepo
import meloplayer.app.store.repo.compareSongs
import meloplayer.app.storex.entities.derived.SongWithAllDetails
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class SongListViewModel(
    songRepo: SongRepo
) : ViewModel() {

    private val _songSortOrder = MutableStateFlow<SongSortOrder>(SongSortOrder.Duration(isAscending = false))
    val songSortOrder: MutableStateFlow<SongSortOrder>
        get() = _songSortOrder

    private val _selectedSongsIndexes = MutableStateFlow<Set<Int>>(setOf())
    val selectedSongsIndexes: StateFlow<Set<Int>>
        get() = _selectedSongsIndexes

    private val groupingKeyGetter = { it: SongWithAllDetails, sortOrder: SongSortOrder ->
        when (sortOrder) {
            is SongSortOrder.Name -> it.song.title.firstOrNull()
                ?.let { if(it.isLetter()) it else "#" }
                ?.toString() ?: ""
            is SongSortOrder.Album -> it.album?.title ?: "Unknown Album"
            is SongSortOrder.DateModified -> monthFormatter.format(
                LocalDate.ofInstant(
                    Instant.ofEpochMilli(it.song.dateModified), ZoneId.systemDefault()
                )
            )

            is SongSortOrder.Duration -> when {
                it.song.lengthMs < 60_000 -> "Less than a minute"
                it.song.lengthMs in 60__000..120_000 -> "1 to 2 minutes"
                it.song.lengthMs in 120..180_000 -> "2 to 3 minutes"
                it.song.lengthMs in 180_000..240_000 -> "3 to 4 minutes"
                it.song.lengthMs in 240_000..300_000 -> "4 to 5 minutes"
                it.song.lengthMs in 300_000..360_000 -> "5 to 6 minutes"
                it.song.lengthMs in 360_000..420_000 -> "6 to 7 minutes"
                it.song.lengthMs in 420_000..480_000 -> "7 to 8 minutes"
                it.song.lengthMs in 480_000..540_000 -> "8 to 9 minutes"
                it.song.lengthMs in 540_000..600_000 -> "9 to 10 minutes"
                else -> "More than 10 minutes"
            }
        }
    }

    val songs = combine(_songSortOrder, songRepo.getSongs(listOf())) { sortOrder, list ->
        list
            .sortedWith { o1, o2 ->
                compareSongs(o1, o2, sortOrder)
            }
            .mapIndexed { index, songListItem -> Pair(index, songListItem) }
            .groupBy { groupingKeyGetter(it.second, sortOrder) }
    }


    fun clearSelection() {
        _selectedSongsIndexes.value = setOf()
    }

    fun setSortOrder(newVal: SongSortOrder) {
        _songSortOrder.value = newVal
    }

    fun onShuffleSongs() {

    }

    fun setSelectedIds(newSet: Set<Int>) {
        _selectedSongsIndexes.value = newSet
    }

}
