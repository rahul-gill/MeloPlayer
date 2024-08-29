package meloplayer.app.store.models


/**
 * Need functionality for
 * 2. Filter by artist name, album name, title name or by combination of these three
 * 3. Filter by albumId, artistId //Since we're splitting single artist name, probably we'll filter
 *          with (artistId, artistName) or just artistName
 * 4. Filter by album artist name
 * 5. Filter by genre name
 * 6. Date added filter and sorted order for last 50 added songs
 * 7. Fuzzy matching where matching by some property name
 * 8.
 */
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
    class ArtistsIdExact(val artistId: Long) : SongFilter()
    class AlbumArtist(val albumArtist: String, val fuzzyMatch: Boolean = false) : SongFilter()
    class AlbumArtistIdExact(val albumArtistId: Long) : SongFilter()
}