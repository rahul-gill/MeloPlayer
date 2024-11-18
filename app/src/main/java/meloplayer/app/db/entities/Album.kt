package meloplayer.app.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo
import androidx.room.Index

@Entity(tableName = "albums",

    indices = [Index(value = ["title"], name = "album_title", unique = true)],
    )
data class Album(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "album_id")
    val id: Long = 0,

    @ColumnInfo(name = "title")
    val title: String,

    @ColumnInfo(name = "release_date")
    val releaseDate: Long? = null,

    @ColumnInfo(name = "cover_image_uri")
    val coverImageUri: String? = null
)
