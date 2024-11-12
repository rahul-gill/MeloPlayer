package meloplayer.app.store.repo

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import meloplayer.app.db.MeloDatabase
import meloplayer.app.store.RepositoryDefaults
import meloplayer.app.store.db.MediaMetadataDB
import meloplayer.app.store.models.SongFilter
import meloplayer.app.store.models.SongListItem
import meloplayer.app.store.models.SongSortOrder
import java.time.Instant
import kotlin.math.exp
import kotlin.math.min

interface SongRepo {
    fun getSongs(
        songFilter: List<SongFilter>
    ): Flow<List<SongListItem>>
}

class SongRepoImpl(
    private val database: MediaMetadataDB
) : SongRepo {

    private suspend fun fetchSongs(): List<SongListItem> {
        return withContext(Dispatchers.IO) {
            // Fetch all songs
            val songs = database.songDao().getAllSongs()

            // Fetch all albums, artists, genres in one go
            val albums = database.albumDao().getAllAlbums().first().associateBy { it.albumId }
            val artists = database.artistDao().getAllArtists().first().associateBy { it.artistId }
            val genres = database.genreDao().getAllGenres().associateBy { it.genreId }

            // Fetch all song-artist and song-genre relationships
            val songArtists = database.relationsDao().getAllSongArtists() // Fetch all relationships
            val songGenres = database.relationsDao().getAllSongGenres()   // Fetch all relationships

            // Create a map to track artist IDs for each song
            val songArtistMap = songArtists.groupBy { it.songId }
            // Create a map to track genre IDs for each song
            val songGenreMap = songGenres.groupBy { it.songId }

            // Build the list of SongListItems
            songs.map { song ->
                val album = albums[song.albumId]
                val songArtistIds = songArtistMap[song.songId]?.map { it.artistId } ?: emptyList()
                val songGenreNames = songGenreMap[song.songId]?.map { genres[it.genreId]?.name }?.filterNotNull()?.joinToString(", ")

                val artistNames = songArtistIds.mapNotNull { artists[it]?.name }.joinToString(", ")
                val albumArtistNames = album?.let { albumArtists ->
                    songArtistIds.mapNotNull { artists[it]?.name }.joinToString(", ")
                }

                SongListItem(
                    songID = song.songId,
                    title = song.title,
                    coverImageUri = song.coverImageUri,
                    dateModified = song.dateModified,
                    lengthMs = song.lengthMs,
                    fileSystemPath = song.fileSystemPath,
                    albumId = album?.albumId,
                    albumName = album?.title,
                    artistNames = artistNames,
                    artistIds = songArtistIds.joinToString(", "),
                    albumArtistNames = albumArtistNames,
                    albumArtistIds = songArtistIds.joinToString(", "), // Assuming album artist IDs are same as song artist IDs
                    genreNames = songGenreNames
                )
            }
        }
    }


    override fun getSongs(
        songFilter: List<SongFilter>
    ): Flow<List<SongListItem>> {
        val defaultFilters = RepositoryDefaults.BlacklistDirectories.map {
            SongFilter.DirectoryPathRecursive(
                folderPath = it.absolutePath.toString(),
                isExclude = true
            )
        }
        val finalFilters = songFilter + defaultFilters

        val res = database.songDao().getAllSongsSummary()
                .map { list ->
                    println("Got songs new: " + list)
                    list.filter { item ->
                        finalFilters.all { item.isMatchingFilter(it) }
                    }
                }
        return res
    }

}

//Comparator like func
fun compareSongs(song1: SongListItem, song2: SongListItem, sortOrder: SongSortOrder): Int {
    val res: Int = when (sortOrder) {
        is SongSortOrder.Album -> if (song1.albumName == null && song2.albumName == null) 0
        else if (song1.albumName == null) 1
        else if (song2.albumName == null) -1
        else song1.albumName.compareTo(song2.albumName)

        is SongSortOrder.DateModified ->  when {
            (song1.dateModified ?: 0) < (song2.dateModified ?: 0) -> -1
            (song1.dateModified ?: 0) > (song2.dateModified ?: 0) -> -1
            else -> 0
        }
        is SongSortOrder.Duration -> song1.lengthMs.compareTo(song2.lengthMs)
        is SongSortOrder.Name -> song1.title.compareTo(song2.title)
    }
    return if (sortOrder.isAscending) res
    else if (res == 1) -1
    else if (res == -1) 1
    else 0
}


//TODO: write tests for it
fun SongListItem.isMatchingFilter(songFilter: SongFilter): Boolean {
    val isSatisfied: Boolean = when (songFilter) {
        is SongFilter.AlbumArtist -> {
            if(albumArtistNames == null){
                return false
            } else if (songFilter.fuzzyMatch) {
                albumArtistNames.splitToSequence(", ").any {
                    levenshteinDistanceNormalized(
                        songFilter.albumArtist,
                        it
                    ) < LevenshteinDistanceThreshold
                }
            } else {
                albumArtistNames.splitToSequence(", ").any { it.contains(songFilter.albumArtist) }
            }
        }

        is SongFilter.AlbumArtistIdExact -> {
            albumArtistIds?.splitToSequence(", ")
                ?.map { it.toLongOrNull() }
                ?.any { it == songFilter.albumArtistId }
                ?: false
        }

        is SongFilter.AlbumIdExact -> {
            if (albumId == null) {
                false
            } else {
                albumId == songFilter.albumId
            }
        }

        is SongFilter.AlbumName -> {
            if (albumName == null) {
                return false
            } else if (songFilter.fuzzyMatch) {
                levenshteinDistanceNormalized(
                    songFilter.album,
                    albumName
                ) < LevenshteinDistanceThreshold
            } else {
                albumName.contains(songFilter.album)
            }
        }

        is SongFilter.Artist -> {
            if(artistNames == null){
                return false
            } else if (songFilter.fuzzyMatch) {
                artistNames.splitToSequence(", ").any {
                    levenshteinDistanceNormalized(
                        songFilter.artist,
                        it
                    ) < LevenshteinDistanceThreshold
                }
            } else {
                artistNames.splitToSequence(", ").any { it.contains(songFilter.artist) }
            }
        }

        is SongFilter.ArtistsIdExact -> {
            artistIds?.splitToSequence(", ")
                ?.map { it.toLongOrNull() }
                ?.any { it == songFilter.artistId }
                ?: false
        }

        is SongFilter.DirectoryPathExact -> {
            fileSystemPath.substringBeforeLast("/") == songFilter.folderPath
        }

        is SongFilter.DirectoryPathRecursive -> {
            fileSystemPath.contains(songFilter.folderPath)
        }

        is SongFilter.DurationLessThan -> {
            lengthMs < songFilter.durationMillis
        }

        is SongFilter.GenreExact -> {
            genreNames?.splitToSequence(", ")?.any{
                songFilter.genreName == it
            } ?: return false
        }

        is SongFilter.GetOneById -> {
            songID == songFilter.id
        }

        is SongFilter.SearchByTitle -> {
            if (songFilter.fuzzyMatch) {
                levenshteinDistanceNormalized(
                    songFilter.titleQuery,
                    title
                ) < LevenshteinDistanceThreshold
            } else {
                title.contains(songFilter.titleQuery)
            }
        }
    }
    return if (songFilter.isExclude) {
        !isSatisfied
    } else {
        isSatisfied
    }
}


const val LevenshteinDistanceThreshold = 0.5

/**
 * Finds the Levenshtein distance between two Strings.
 * https://github.com/apache/commons-text/blob/master/src/main/java/org/apache/commons/text/similarity/LevenshteinDistance.java#L259
 * @param left the first CharSequence, must not be null
 * @param right the second CharSequence, must not be null
 * @return result distance between 0 and 1
 */
private fun levenshteinDistanceNormalized(leftArg: CharSequence, rightArg: CharSequence): Double {
    var left: CharSequence = leftArg
    var right: CharSequence = rightArg

    /*
       This implementation use two variable to record the previous cost counts,
       So this implementation use less memory than previous impl.
     */
    var n = left.length // length of left
    var m = right.length // length of right

    if (n == 0) {
        return 1.0
    }
    if (m == 0) {
        return 1.0
    }

    if (n > m) {
        // swap the input strings to consume less memory
        val tmp: CharSequence = left
        left = right
        right = tmp
        n = m
        m = right.length
    }

    val p = IntArray(n + 1)

    // indexes into strings left and right
    var i: Int // iterates through left
    var upperLeft: Int
    var upper: Int

    var rightJ: Char // jth character of right
    var cost: Int // cost

    i = 0
    while (i <= n) {
        p[i] = i
        i++
    }

    var j = 1 // iterates through right
    while (j <= m) {
        upperLeft = p[0]
        rightJ = right[j - 1]
        p[0] = j

        i = 1
        while (i <= n) {
            upper = p[i]
            cost = if (left[i - 1] == rightJ) 0 else 1
            // minimum of cell to the left+1, to the top+1, diagonally left and up +cost
            p[i] = min(
                min((p[i - 1] + 1).toDouble(), (p[i] + 1).toDouble()),
                (upperLeft + cost).toDouble()
            )
                .toInt()
            upperLeft = upper
            i++
        }
        j++
    }

    val distance = p[n]
    return 1 / exp(distance.toDouble() / (min(m, n) - distance))
}