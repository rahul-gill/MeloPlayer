package meloplayer.app.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow
import meloplayer.app.db.entities.Album

@Dao
interface AlbumsDao {

    @Transaction
    @Query("SELECT * FROM albums")
    fun getAllAlbums(): Flow<List<Album>>

}