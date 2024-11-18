package meloplayer.app.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo
import androidx.room.Index

@Entity(tableName = "genres",
    indices = [Index(value = ["name"], name = "genre_name")])
data class Genre(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "genre_id")
    val id: Int = 0,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "image_uri")
    val imageUri: String? = null
)
