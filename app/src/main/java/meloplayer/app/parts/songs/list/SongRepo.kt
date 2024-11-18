package meloplayer.app.parts.songs.list

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import meloplayer.app.common.BlacklistDirectories
import meloplayer.app.common.LevenshteinDistanceThreshold
import meloplayer.app.common.levenshteinDistanceNormalized
import meloplayer.app.db.dao.SongsDao
import meloplayer.app.db.entities.derived.SongWithAllDetails

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
        val defaultFilters = BlacklistDirectories.map {
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
        is SongSortOrder.Album -> (song1.album?.title ?: "").compareTo(song2.album?.title ?: "")
        is SongSortOrder.DateModified -> song1.song.dateModified.compareTo(song2.song.dateModified)
        is SongSortOrder.Duration -> song1.song.lengthMs.compareTo(song2.song.lengthMs)
        is SongSortOrder.Name -> song1.song.title.compareTo(song2.song.title)
    }
    return if (sortOrder.isAscending) res
    else when {
        res > 0 -> -1
        res < 0 -> 1
        else -> 0
    }
}


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
            artists.any { artist ->  songFilter.albumArtistId == artist.artistId && artist.isAlbumArtist }
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
            artists.any { it.artistId == songFilter.artistId  && it.isSongArtist}
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


