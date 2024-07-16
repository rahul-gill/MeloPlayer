package meloplayer.core.store.model

import java.time.LocalDate

class MediaStoreSong constructor(
    val id: Long,
    val title: String,
    val trackNumber: Int,
    val year: Int,
    val duration: Long,
    val directoryPath: String,
    val dateModified: LocalDate,
    val albumId: Long,
    val albumName: String,
    val artistId: Long,
    val artistNames: List<String>,
    val composer: String?,
    val albumArtist: String?
)