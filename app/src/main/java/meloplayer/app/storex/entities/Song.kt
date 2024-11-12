package meloplayer.app.storex.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import androidx.room.ColumnInfo

@Entity(tableName = "songs",
    foreignKeys = [ForeignKey(
        entity = Album::class,
        parentColumns = ["album_id"],
        childColumns = ["album_id"]
    )]
)
data class Song(
    @PrimaryKey
    @ColumnInfo(name = "song_id")
    val id: Long,

    @ColumnInfo(name = "title")
    val title: String,

    @ColumnInfo(name = "file_system_path")
    val fileSystemPath: String,

    @ColumnInfo(name = "length_ms")
    val lengthMs: Int,

    @ColumnInfo(name = "bit_rate_kbps")
    val bitRateKbps: Int,

    @ColumnInfo(name = "sample_rate_hz")
    val sampleRateHz: Int,

    @ColumnInfo(name = "channels_count")
    val channelsCount: Int,

    @ColumnInfo(name = "cover_image_uri")
    val coverImageUri: String?,

    @ColumnInfo(name = "track_number")
    val trackNumber: Int?,

    @ColumnInfo(name = "cd_number")
    val cdNumber: Int?,

    @ColumnInfo(name = "album_id", index = true)
    val albumId: Long?,

    @ColumnInfo(name = "subtitle")
    val subtitle: String?,

    @ColumnInfo(name = "date_modified")
    val dateModified: Long
)
