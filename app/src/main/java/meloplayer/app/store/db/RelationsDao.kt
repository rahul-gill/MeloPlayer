package meloplayer.app.store.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import meloplayer.app.store.db.entities.SongArtist
import meloplayer.app.store.db.entities.SongGenre

@Dao
interface RelationsDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSongArtistRecords(songArtist: List<SongArtist>)


    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSongGenreRecords(songGenre: List<SongGenre>)

    @Query("DELETE FROM ARTISTS " +
            "WHERE artistId NOT IN (" +
            "SELECT rel.artistId FROM SONG_ARTISTS rel, songs  Where rel.songId = songs.songId" +
            ")")
    suspend fun cleanupArtistsOrphans()


    @Query("DELETE FROM genres " +
            "WHERE genreId NOT IN (" +
            "SELECT rel.genreId FROM song_genres rel, songs  Where rel.songId = songs.songId" +
            ")")
    suspend fun cleanupGenresOrphans()


    @Query("DELETE FROM albums Where albumId NOT IN (SELECT songs.albumId FROM songs)")
    suspend fun cleanupAlbumOrphans()

    @Query("SELECT * from song_artists")
    fun getAllSongArtists(): List<SongArtist>


    @Query("SELECT * from song_genres")
    fun getAllSongGenres(): List<SongGenre>

}