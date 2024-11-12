package meloplayer.app.store.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "genres")
data class Genre(
    @PrimaryKey(autoGenerate = true)
    val genreId: Long = 0,
    val name: String,
    val imageUri: String?
)
