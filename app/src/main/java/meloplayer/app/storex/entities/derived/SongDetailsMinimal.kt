package meloplayer.app.storex.entities.derived

data class SongDetailsMinimal(
    val songId: Long,
    val songName: String,
    val lengthMs: String,
    val albumName: String?,
    val artistNames: String?,
    val albumArtistNames : String?,
    val genres: String?
)