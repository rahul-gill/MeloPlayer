package meloplayer.app.store.models

sealed class SongFilter(val isExclude: Boolean = false) {
    class DirectoryPathRecursive(val folderPath: String, isExclude: Boolean) : SongFilter(isExclude)
    class DirectoryPathExact(val folderPath: String) : SongFilter()
    class DurationLessThan(val durationMillis: Long, isExclude: Boolean) : SongFilter(isExclude)
    class GenreExact(val genreName: String, isExclude: Boolean) : SongFilter(isExclude)
    class SearchByTitle(val titleQuery: String, val fuzzyMatch: Boolean = false) : SongFilter()
    class GetOneById(val id: Long) : SongFilter()
    class AlbumIdExact(val albumId: Long) : SongFilter()
    class AlbumName(val album: String, val fuzzyMatch: Boolean = false) : SongFilter()
    class Artist(val artist: String, val fuzzyMatch: Boolean = false) : SongFilter()
    class ArtistsIdExact(val artistId: String) : SongFilter()
    class AlbumArtist(val albumArtist: String, val fuzzyMatch: Boolean = false) : SongFilter()
    class AlbumArtistExact(val albumArtist: String) : SongFilter()
}