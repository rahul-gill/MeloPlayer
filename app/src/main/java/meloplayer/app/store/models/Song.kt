package meloplayer.app.store.models

import java.time.Instant

data class Song(
    val songID: Long,
    val title: String,
    val fileSystemPath: String,
    val dateModified: Instant,
    val lengthMs: Long,
    val bitRateKbps: Long,
    val sampleRateHz: Long,
    val channelCount: Long,
    val coverImageUri: String?,
    val trackNumber: Long?,
    val cdNUmber: Long?,
    val subtitle: String?,
    val albumIdName: Pair<Long, String>?,
    val artistIdNames: List<Pair<Long, String>>,
    val albumArtistIdNames: List<Pair<Long, String>>,
    val genreIdNames: List<Pair<Long,String>>
)