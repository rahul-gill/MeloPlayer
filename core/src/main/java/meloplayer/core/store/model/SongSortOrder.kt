package meloplayer.core.store.model

import androidx.annotation.StringRes
import wow.app.core.R

sealed class SongSortOrder {
    

    open val isAscending: Boolean = false
    data class Name(override val isAscending: Boolean = true) : SongSortOrder()
    data class Album(override val isAscending: Boolean = true) : SongSortOrder()
    data class AlbumArtist(override val isAscending: Boolean = true) : SongSortOrder()
    data class SongArtist(override val isAscending: Boolean = true) : SongSortOrder()
    data class Composer(override val isAscending: Boolean = true) : SongSortOrder()
    data class DateModified(override val isAscending: Boolean = false) : SongSortOrder()
    data class Year(override val isAscending: Boolean = false) : SongSortOrder()
    data class Duration(override val isAscending: Boolean = false) : SongSortOrder()
    

    companion object {
        val allTypes = listOf(
            Name(), Album(), AlbumArtist(), SongArtist(), Composer(),
             DateModified(), Year(), Duration()
        )
    }
}

@StringRes
fun SongSortOrder.toStringResource(): Int {
    return when(this){
        is SongSortOrder.Album -> R.string.album
        is SongSortOrder.AlbumArtist -> R.string.album_artist
        is SongSortOrder.Composer -> R.string.composer
        is SongSortOrder.DateModified -> R.string.date_modified
        is SongSortOrder.Duration -> R.string.duration
        is SongSortOrder.Name -> R.string.name
        is SongSortOrder.SongArtist -> R.string.artist
        is SongSortOrder.Year -> R.string.year
    }
}