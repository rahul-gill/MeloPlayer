package meloplayer.app.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow
import meloplayer.app.db.entities.derived.ArtistWithSongOrAlbumTypeDetail
import meloplayer.app.db.entities.derived.SongWithAllDetails

@Dao
interface SongsDao {

    @Transaction
    @Query("SELECT * FROM songs")
    fun getSongWithAllDetails(): Flow<List<SongWithAllDetails>>

    @Transaction
    @Query("SELECT * FROM songs WHERE song_id = :id")
    fun getSongWithAllDetailsOne(id: Long): Flow<SongWithAllDetails?>

}