package meloplayer.app.parts.songs.list

import androidx.annotation.StringRes
import wow.app.core.R

sealed class SongSortOrder {
    open val isAscending: Boolean = false

    data class Name(override val isAscending: Boolean = true) : SongSortOrder()
    data class Album(override val isAscending: Boolean = true) : SongSortOrder()
    //data class AlbumArtist(override val isAscending: Boolean = true) : SongSortOrder()
    //data class SongArtist(override val isAscending: Boolean = true) : SongSortOrder()
    //data class Composer(override val isAscending: Boolean = true) : SongSortOrder()
    data class DateModified(override val isAscending: Boolean = false) : SongSortOrder()
    //TODO
    //data class Year(override val isAscending: Boolean = false) : SongSortOrder()
    data class Duration(override val isAscending: Boolean = false) : SongSortOrder()


    @StringRes
    fun toStringResource(): Int {
        return when(this){
            is Album -> R.string.album
            is DateModified -> R.string.date_modified
            is Duration -> R.string.duration
            is Name -> R.string.name
        }
    }

    companion object {
        val allTypes = listOf(
            Name(), Album(), /*AlbumArtist(), SongArtist(), Composer(),*/
            DateModified(), /*Year(),*/ Duration()
        )
    }
}