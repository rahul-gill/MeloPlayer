package meloplayer.app.db.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.ColumnInfo

@Entity(tableName = "song_artists",
    primaryKeys = ["song_id", "artist_id"],
    foreignKeys = [
        ForeignKey(
            entity = Song::class,
            parentColumns = ["song_id"],
            childColumns = ["song_id"]
        ),
        ForeignKey(
            entity = Artist::class,
            parentColumns = ["artist_id"],
            childColumns = ["artist_id"]
        )
    ]
)
data class SongArtist(
    @ColumnInfo(name = "song_id", index = true)
    val songId: Long,
    @ColumnInfo(name = "artist_id", index = true)
    val artistId: Long,
    val isAlbumArtist: Boolean,
    val isSongArtist: Boolean
)
