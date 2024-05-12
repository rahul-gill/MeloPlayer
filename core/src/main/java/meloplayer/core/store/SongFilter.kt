package meloplayer.core.store

sealed class SongFilter(val isExclude: Boolean = false) {
    class DirectoryPath(val folderPath: String, isExclude: Boolean) : SongFilter(isExclude)
    class DirectoryPathExact(val folderPath: String) : SongFilter()
    class DurationLessThan(val durationMillis: Long, isExclude: Boolean) : SongFilter(isExclude)
    class GenreExact(val genreName: String, isExclude: Boolean) : SongFilter(isExclude)
    class SearchByTitle(val titleQuery: String) : SongFilter()
    class GetOneById(val id: String) : SongFilter()
    class AlbumIdExact(val albumId: String) : SongFilter()
    class AlbumName(val album: String) : SongFilter()
    class Artist(val artist: String) : SongFilter()
    class ArtistsIdExact(val artistId: String) : SongFilter()
    class AlbumArtist(val albumArtist: String) : SongFilter()
    class AlbumArtistExact(val albumArtist: String) : SongFilter()
}