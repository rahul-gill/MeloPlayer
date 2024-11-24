package meloplayer.app.parts.songs.list

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import meloplayer.app.db.SyncManager
import meloplayer.app.db.entities.derived.SongWithAllDetails
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset

class SongListViewModel(
    songRepo: SongRepo,
    private val syncM: SyncManager
) : ViewModel() {

    private val _songSortOrder = MutableStateFlow<SongSortOrder>(SongSortOrder.Duration(isAscending = false))
    val songSortOrder: MutableStateFlow<SongSortOrder>
        get() = _songSortOrder

    private val _selectedSongsIndexes = MutableStateFlow<Set<Int>>(setOf())
    val selectedSongsIndexes: StateFlow<Set<Int>>
        get() = _selectedSongsIndexes

    private val groupingKeyGetter = { itx: SongWithAllDetails, sortOrder: SongSortOrder ->
        println("SONG:${itx.song.title} DATE: ${itx.song.dateModified}")
        when (sortOrder) {
            is SongSortOrder.Name -> itx.song.title.firstOrNull()
                ?.let { if(it.isLetter()) it else "#" }
                ?.toString() ?: ""
            is SongSortOrder.Album -> itx.album?.title ?: "Unknown Album"
            is SongSortOrder.DateModified -> monthFormatter.format(
                Instant.ofEpochMilli(itx.song.dateModified * 1000).atZone(ZoneId.systemDefault()).also {
                    println("SONG:${itx.song.title} DATE: $it")

                }
            )

            is SongSortOrder.Duration -> when {
                itx.song.lengthMs < 60_000 -> "Less than a minute"
                itx.song.lengthMs in 60__000..120_000 -> "1 to 2 minutes"
                itx.song.lengthMs in 120..180_000 -> "2 to 3 minutes"
                itx.song.lengthMs in 180_000..240_000 -> "3 to 4 minutes"
                itx.song.lengthMs in 240_000..300_000 -> "4 to 5 minutes"
                itx.song.lengthMs in 300_000..360_000 -> "5 to 6 minutes"
                itx.song.lengthMs in 360_000..420_000 -> "6 to 7 minutes"
                itx.song.lengthMs in 420_000..480_000 -> "7 to 8 minutes"
                itx.song.lengthMs in 480_000..540_000 -> "8 to 9 minutes"
                itx.song.lengthMs in 540_000..600_000 -> "9 to 10 minutes"
                else -> "More than 10 minutes"
            }

            is SongSortOrder.AlbumArtist ->
                itx.artists.firstOrNull { art -> art.isAlbumArtist }?.name ?: "# Unknown artist"
            is SongSortOrder.SongArtist ->
                itx.artists.firstOrNull { art -> art.isSongArtist }?.name ?: "# Unknown artist"
            is SongSortOrder.Year -> "year"//TODO()
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

    fun onSync() {
        GlobalScope.launch {
            syncM.syncDatabaseWithFileSystem()
        }
    }

}
