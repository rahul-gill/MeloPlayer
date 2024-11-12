package meloplayer.app.store.db.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "songs",
    foreignKeys = [
        ForeignKey(
            entity = Album::class,
            parentColumns = ["albumId"],
            childColumns = ["albumId"],
            onDelete = ForeignKey.NO_ACTION
        )
    ]
)
data class Song(
    @PrimaryKey
    val songId: Long, // Media store id
    val lengthMs: Long,
    val bitRateKbps: Int,
    val sampleRateHz: Int,
    val channelsCount: Int,
    val trackNumber: Int?,
    val cdNumber: Int?,
    @ColumnInfo(name = "albumId", index = true)
    val albumId: Long?, // Nullable to handle songs without an album
    val subtitle: String?,
    val dateModified: Long // Store Instant as milliseconds
)
