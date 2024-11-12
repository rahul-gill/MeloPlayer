package meloplayer.app.store.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "artists")
data class Artist(
    @PrimaryKey(autoGenerate = true)
    val artistId: Long = 0,
    val name: String,
    val isSongArtist: Boolean,
    val isAlbumArtist: Boolean,
    val bio: String?,
    val imageUri: String?
)
