package meloplayer.app.storex.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.ColumnInfo

@Entity(tableName = "song_tags",
    primaryKeys = ["song_id", "tag_id"],
    foreignKeys = [
        ForeignKey(
            entity = Song::class,
            parentColumns = ["song_id"],
            childColumns = ["song_id"]
        ),
        ForeignKey(
            entity = Tag::class,
            parentColumns = ["tag_id"],
            childColumns = ["tag_id"]
        )
    ]
)
data class SongTag(
    @ColumnInfo(name = "song_id", index = true)
    val songId: Int,

    @ColumnInfo(name = "tag_id", index = true)
    val tagId: Int
)
