package meloplayer.app.storex.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo
import androidx.room.Index

@Entity(tableName = "tags",
    indices = [Index(value = ["tag_group"], name = "tag_group")])
data class Tag(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "tag_id")
    val id: Int = 0,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "tag_group")
    val tagGroup: String?
)
