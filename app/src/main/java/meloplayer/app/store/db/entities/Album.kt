package meloplayer.app.store.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDateTime

@Entity(tableName = "albums")
data class Album(
    @PrimaryKey(autoGenerate = true)
    val albumId: Long = 0,
    val title: String,
    val releaseDate: Long = 0, // Store Instant as milliseconds
    val coverImageUri: String?
)
