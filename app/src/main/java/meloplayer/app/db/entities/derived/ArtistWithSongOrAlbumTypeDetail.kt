package meloplayer.app.db.entities.derived

import androidx.room.ColumnInfo
import androidx.room.DatabaseView


@DatabaseView(
    "SELECT artists.*, isSongArtist, isAlbumArtist, song_id " +
            "FROM artists JOIN song_artists " +
            "WHERE song_artists.artist_id = artists.artist_id"
)
data class ArtistWithSongOrAlbumTypeDetail(
    @ColumnInfo(name = "artist_id")
    val artistId: Long = 0,
    val name: String,
    val bio: String? = null,
    @ColumnInfo(name = "image_uri")
    val imageUri: String? = null,
    val isSongArtist: Boolean,
    val isAlbumArtist: Boolean,
    @ColumnInfo("song_id")
    val songId: Long
)