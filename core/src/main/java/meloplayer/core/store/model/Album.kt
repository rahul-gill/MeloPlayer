package meloplayer.core.store.model

import java.time.LocalDate


data class Album(
    val id: Long,
    val songs: List<MediaStoreSong>
) {

    val title: String
        get() = songs.first().albumName


    val year: Int
        get() = songs.first().year

    val dateModified: LocalDate
        get() = songs.first().dateModified

    val songCount: Int
        get() = songs.size

    val albumArtist: String?
        get() = songs.first().albumArtist
}