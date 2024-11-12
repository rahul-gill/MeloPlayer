package meloplayer.app.storex.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.ColumnInfo

@Entity(tableName = "song_genres",
    primaryKeys = ["song_id", "genre_id"],
    foreignKeys = [
        ForeignKey(
            entity = Song::class,
            parentColumns = ["song_id"],
            childColumns = ["song_id"]
        ),
        ForeignKey(
            entity = Genre::class,
            parentColumns = ["genre_id"],
            childColumns = ["genre_id"]
        )
    ]
)
data class SongGenre(
    @ColumnInfo(name = "song_id", index = true)
    val songId: Long,

    @ColumnInfo(name = "genre_id", index = true)
    val genreId: Long
)
