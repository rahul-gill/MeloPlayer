package meloplayer.app.store.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Delete
import meloplayer.app.store.db.entities.Genre

@Dao
interface GenreDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGenres(genre: List<Genre>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGenre(genre: Genre)

    @Query("SELECT * FROM genres WHERE genreId = :genreId")
    suspend fun getGenreById(genreId: Long): Genre?

    @Query("SELECT * FROM genres WHERE name = :name")
    suspend fun getGenreByName(name: String): Genre?

    @Query("SELECT * FROM genres")
    suspend fun getAllGenres(): List<Genre>

    @Delete
    suspend fun deleteGenre(genre: Genre)
}
