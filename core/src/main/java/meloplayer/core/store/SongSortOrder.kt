package meloplayer.core.store

sealed class SongSortOrder(val isAscending: Boolean) {
    class Name(isAscending: Boolean) : SongSortOrder(isAscending)
    class Album(isAscending: Boolean) : SongSortOrder(isAscending)
    class AlbumArtist(isAscending: Boolean) : SongSortOrder(isAscending)
    class SongArtist(isAscending: Boolean) : SongSortOrder(isAscending)
    class Composer(isAscending: Boolean) : SongSortOrder(isAscending)
    class DateAdded(isAscending: Boolean) : SongSortOrder(isAscending)
    class DateModified(isAscending: Boolean) : SongSortOrder(isAscending)
    class Year(isAscending: Boolean) : SongSortOrder(isAscending)
    class Duration(isAscending: Boolean) : SongSortOrder(isAscending)
}