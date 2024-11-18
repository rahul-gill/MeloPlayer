package meloplayer.app.parts.artists.list


sealed class ArtistsSortOrder(val isAscending: Boolean) {
    class Name(isAscending: Boolean) : ArtistsSortOrder(isAscending)
    class NumberOfSongs(isAscending: Boolean) : ArtistsSortOrder(isAscending)
    class NumberOfAlbums(isAscending: Boolean) : ArtistsSortOrder(isAscending)
}