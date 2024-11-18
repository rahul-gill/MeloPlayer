package meloplayer.app.repo

import android.content.Context
import meloplayer.app.db.entities.Album
import meloplayer.app.db.entities.Artist
import meloplayer.app.db.entities.Song

interface SearchAndLibraryRepository {

    fun recentlyPlayedTracks(): List<Song>

    fun topTracks(): List<Song>

    fun notRecentlyPlayedTracks(): List<Song>

    fun topAlbums(): List<Album>

    fun topArtists(): List<Artist>


    fun recentlyAddedSongs(): List<Song>

    fun recentlyAddedAlbums(): List<Album>

    fun recentlyAddedArtists(): List<Artist>





    suspend fun searchAll(context: Context, query: String, resultType: ResultType)


    enum class ResultType {
        SONGS,
        ARTISTS,
        ALBUMS,
        ALBUM_ARTISTS,
        GENRES,
        PLAYLISTS,
        NO_FILTER
    }
}