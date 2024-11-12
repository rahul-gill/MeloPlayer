package meloplayer.app.store.models


data class SongListItem(
    val songID: Long,
    val title: String,
    val coverImageUri: String?,
    val dateModified: Long?,
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