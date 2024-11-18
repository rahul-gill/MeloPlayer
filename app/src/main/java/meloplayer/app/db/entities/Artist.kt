package meloplayer.app.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo
import androidx.room.Index

@Entity(tableName = "artists",
    indices = [Index(value = ["name"], name = "artist_name", unique = true)],

)
data class Artist(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "artist_id")
    val id: Long = 0,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "bio")
    val bio: String? = null,

    @ColumnInfo(name = "image_uri")
    val imageUri: String? = null
)
