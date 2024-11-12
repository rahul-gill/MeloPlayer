package meloplayer.app.store.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Delete
import kotlinx.coroutines.flow.Flow
import meloplayer.app.store.db.entities.Album

@Dao
interface AlbumDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlbums(album: List<Album>)


    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlbum(album: Album)

    @Query("SELECT * FROM albums WHERE albumId = :albumId")
    suspend fun getAlbumById(albumId: Long): Album?

    @Query("SELECT * FROM albums WHERE title = :title")
    suspend fun getAlbumByTitle(title: String): Album?

    @Query("SELECT * FROM albums")
    fun getAllAlbums(): Flow<List<Album>>

    @Delete
    suspend fun deleteAlbum(album: Album)
}
