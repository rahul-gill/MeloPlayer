package meloplayer.app.store.models

import java.time.Instant

data class SongListItem(
    val songID: Long,
    val title: String,
    val coverImageUri: String?,
    val dateModified: Instant,
    val lengthMs: Long,
    val fileSystemPath: String,
    val albumId: Long?,
    val albumName: String?,
    val artistNames: String?,
    val artistIds: String?,
    val albumArtistNames: String?,
    val albumArtistIds: String?,
    val genreNames: String?
)