package meloplayer.app.store.models

data class Song(
    val songID: Long,
    val title: String,
    val fileSystemPath: String,
    val lengthMs: Long,
    val bitRateKbps: Long,
    val sampleRateHz: Long,
    val channelCount: Long,
    val coverImageUri: String?,
    val trackNumber: Long?,
    val cdNUmber: Long?,
    val albumIdName: Pair<Long, String>?,
    val subtitle: String?,
    val artistNames: List<String>,
    val genreNames: List<String>
)