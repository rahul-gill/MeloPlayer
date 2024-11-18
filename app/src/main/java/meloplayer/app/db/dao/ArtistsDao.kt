package meloplayer.app.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow
import meloplayer.app.db.entities.Artist

@Dao
interface ArtistsDao {

    @Transaction
    @Query("SELECT * FROM artists")
    fun getAllArtists(): Flow<List<Artist>>

}