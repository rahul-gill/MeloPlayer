package meloplayer.app.store.models

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