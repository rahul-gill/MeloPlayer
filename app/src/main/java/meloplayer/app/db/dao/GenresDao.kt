package meloplayer.app.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow
import meloplayer.app.db.entities.Genre

@Dao
interface GenresDao {

    @Transaction
    @Query("SELECT * FROM genres")
    fun getAllGenres(): Flow<List<Genre>>

}