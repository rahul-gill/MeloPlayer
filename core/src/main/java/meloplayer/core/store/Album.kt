package meloplayer.core.store

data class Album(
    val id: Long,
    val songs: List<MediaStoreSong>
) {

    val title: String
        get() = songs.first().albumName

    val artistId: Long
        get() = songs.first().artistId

    val artistName: String
        get() = songs.first().artistName

    val year: Int
        get() = songs.first().year

    val dateModified: Long
        get() = songs.first().dateModified

    val songCount: Int
        get() = songs.size

    val albumArtist: String?
        get() = songs.first().albumArtist
}