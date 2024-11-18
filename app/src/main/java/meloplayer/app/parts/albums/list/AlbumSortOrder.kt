package meloplayer.app.parts.albums.list

sealed class AlbumSortOrder(val isAscending: Boolean) {
    class Name(isAscending: Boolean) : AlbumSortOrder(isAscending)
    class NumberOfSongs(isAscending: Boolean) : AlbumSortOrder(isAscending)
    class Year(isAscending: Boolean) : AlbumSortOrder(isAscending)
}