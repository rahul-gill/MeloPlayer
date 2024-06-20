package meloplayer.core.store.repo

import android.content.Context
import meloplayer.core.store.model.Album
import meloplayer.core.store.model.Artist
import meloplayer.core.store.model.MediaStoreSong

interface SearchAndLibraryRepository {

    fun recentlyPlayedTracks(): List<MediaStoreSong>

    fun topTracks(): List<MediaStoreSong>

    fun notRecentlyPlayedTracks(): List<MediaStoreSong>

    fun topAlbums(): List<Album>

    fun topArtists(): List<Artist>


    fun recentlyAddedMediaStoreSongs(): List<MediaStoreSong>

    fun recentlyAddedAlbums(): List<Album>

    fun recentlyAddedArtists(): List<Artist>





    suspend fun searchAll(context: Context, query: String, resultType: ResultType)


    enum class ResultType {
        MediaStoreSongS,
        ARTISTS,
        ALBUMS,
        ALBUM_ARTISTS,
        GENRES,
        PLAYLISTS,
        NO_FILTER
    }
}