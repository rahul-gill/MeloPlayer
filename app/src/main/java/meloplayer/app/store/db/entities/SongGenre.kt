package meloplayer.app.store.db.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey

@Entity(
    tableName = "song_genres",
    primaryKeys = ["songId", "genreId"],
    foreignKeys = [
        ForeignKey(
            entity = Song::class,
            parentColumns = ["songId"],
            childColumns = ["songId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Genre::class,
            parentColumns = ["genreId"],
            childColumns = ["genreId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class SongGenre(
    @ColumnInfo(name = "songId", index = true)
    val songId: Long,
    @ColumnInfo(name = "genreId", index = true)
    val genreId: Long
)
