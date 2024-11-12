package meloplayer.app.store.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Delete
import kotlinx.coroutines.flow.Flow
import meloplayer.app.store.db.entities.Artist

@Dao
interface ArtistDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertArtists(artist: List<Artist>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertArtist(artist: Artist)

    @Query("SELECT * FROM artists WHERE artistId = :artistId")
    suspend fun getArtistById(artistId: Long): Artist?

    @Query("SELECT * FROM artists WHERE name = :name")
    suspend fun getArtistByName(name: String): Artist?

    @Query("SELECT * FROM artists")
    fun getAllArtists(): Flow<List<Artist>>

    @Delete
    suspend fun deleteArtist(artist: Artist)
}
