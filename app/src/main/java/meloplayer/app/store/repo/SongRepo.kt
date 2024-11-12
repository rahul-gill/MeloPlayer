package meloplayer.app.store.repo

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import meloplayer.app.db.MeloDatabase
import meloplayer.app.store.RepositoryDefaults
import meloplayer.app.store.models.SongFilter
import meloplayer.app.store.models.SongListItem
import meloplayer.app.store.models.SongSortOrder
import meloplayer.app.storex.dao.SongsDao
import meloplayer.app.storex.entities.derived.SongDetailsMinimal
import meloplayer.app.storex.entities.derived.SongWithAllDetails
import kotlin.math.exp
import kotlin.math.min

interface SongRepo {
    fun getSongs(
        songFilter: List<SongFilter>
    ): Flow<List<SongWithAllDetails>>
}

class SongRepoImpl(
    private val dao: SongsDao
) : SongRepo {
    override fun getSongs(
        songFilter: List<SongFilter>
    ): Flow<List<SongWithAllDetails>> {
        val defaultFilters = RepositoryDefaults.BlacklistDirectories.map {
            SongFilter.DirectoryPathRecursive(
                folderPath = it.absolutePath.toString(),
                isExclude = true
            )
        }
        val finalFilters = songFilter + defaultFilters
        println("SongRepoImpl: getSongsStart")
        val res = dao.getSongWithAllDetails()
                .map { list ->
                    list.filter { item ->
                        finalFilters.all { item.isMatchingFilter(it) }
                    }
                }
        println("SongRepoImpl: getSongsEnd")
        return res
    }

}

//Comparator like func
fun compareSongs(song1: SongWithAllDetails, song2: SongWithAllDetails, sortOrder: SongSortOrder): Int {
    val res: Int = when (sortOrder) {
        is SongSortOrder.Album -> if (song1.album == null && song2.album == null) 0
        else if (song1.album == null) 1
        else if (song2.album == null) -1
        else song1.album.title.compareTo(song2.album.title)

        is SongSortOrder.DateModified -> when {
            song1.song.dateModified < song1.song.dateModified -> -1
            song1.song.dateModified > song1.song.dateModified -> 1
            else -> 0
        }
        is SongSortOrder.Duration -> song1.song.lengthMs.compareTo(song2.song.lengthMs)
        is SongSortOrder.Name -> song1.song.title.compareTo(song2.song.title)
    }
    return if (sortOrder.isAscending) res
    else if (res == 1) -1
    else if (res == -1) 1
    else 0
}


//TODO: write tests for it
fun SongWithAllDetails.isMatchingFilter(songFilter: SongFilter): Boolean {
    val isSatisfied: Boolean = when (songFilter) {
        is SongFilter.AlbumArtist -> {
            artists.any { artist ->
                when {
                    !artist.isAlbumArtist -> false
                    songFilter.fuzzyMatch -> levenshteinDistanceNormalized(
                        songFilter.albumArtist,
                        artist.name
                    ) < LevenshteinDistanceThreshold

                    else -> artist.name.contains(songFilter.albumArtist)
                }
            }
        }

        is SongFilter.AlbumArtistIdExact -> {
            artists.any { artist -> artist.isAlbumArtist && songFilter.albumArtistId == artist.id }
        }

        is SongFilter.AlbumIdExact -> {
            if(album == null){
                return false
            } else {
                album.id == songFilter.albumId
            }
        }

        is SongFilter.AlbumName -> {
            when {
                album == null -> false
                songFilter.fuzzyMatch -> levenshteinDistanceNormalized(
                    songFilter.album,
                    album.title
                ) < LevenshteinDistanceThreshold
                else -> album.title.contains(songFilter.album)
            }
        }

        is SongFilter.Artist -> {
            artists.any { artist ->
                when {
                    !artist.isSongArtist -> false
                    songFilter.fuzzyMatch -> levenshteinDistanceNormalized(
                        songFilter.artist,
                        artist.name
                    ) < LevenshteinDistanceThreshold

                    else -> artist.name.contains(songFilter.artist)
                }
            }
        }

        is SongFilter.ArtistsIdExact -> {
            artists.any { it.id == songFilter.artistId }
        }

        is SongFilter.DirectoryPathExact -> {
            song.fileSystemPath.substringBeforeLast("/") == songFilter.folderPath
        }

        is SongFilter.DirectoryPathRecursive -> {
            song.fileSystemPath.contains(songFilter.folderPath)
        }

        is SongFilter.DurationLessThan -> {
            song.lengthMs < songFilter.durationMillis
        }

        is SongFilter.GenreExact -> {
            genres.any { genre ->
                genre.name == songFilter.genreName
            }
        }

        is SongFilter.GetOneById -> {
            song.id == songFilter.id
        }

        is SongFilter.SearchByTitle -> {
            if (songFilter.fuzzyMatch) {
                levenshteinDistanceNormalized(
                    songFilter.titleQuery,
                    song.title
                ) < LevenshteinDistanceThreshold
            } else {
                song.title.contains(songFilter.titleQuery)
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