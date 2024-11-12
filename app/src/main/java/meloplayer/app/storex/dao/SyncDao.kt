package meloplayer.app.storex.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import meloplayer.app.storex.entities.Album
import meloplayer.app.storex.entities.Artist
import meloplayer.app.storex.entities.Genre
import meloplayer.app.storex.entities.Song
import meloplayer.app.storex.entities.SongArtist
import meloplayer.app.storex.entities.SongGenre

data class PairReturn(
    val first: String,
    val second: Long
)

@Dao
interface SyncDao {
    @Transaction
    @Query("select song_id FROM songs")
    fun existingSongIds(): List<Long>

    @Transaction
    @Query("select title AS first, album_id AS second FROM albums")
    fun existingAlbumIdNamePairs(): List<PairReturn>

    @Transaction
    @Query("select name  AS first, artist_id AS second FROM artists")
    fun existingArtistIdNamePairs(): List<PairReturn>

    @Transaction
    @Query("select name AS first, genre_id AS second FROM genres")
    fun existingGenreIdNamePairs(): List<PairReturn>

    @Transaction
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertSongs(songs: List<Song>)

    @Transaction
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertAlbumsOrIgnoreIfPresent(songs: List<Album>)

    @Transaction
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertArtistsOrIgnoreIfPresent(songs: List<Artist>)

    @Transaction
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertGenresOrIgnoreIfPresent(songs: List<Genre>)

    @Transaction
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertSongArtistCrossRefs(songs: List<SongArtist>)

    @Transaction
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertSongGenreCrossRefs(songs: List<SongGenre>)
}