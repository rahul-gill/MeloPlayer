package meloplayer.core.store.model

data class Artist(
    val name: String,
    val albums: List<Album>,
    val isAlbumArtist: Boolean = false
){
    val songCount
        get() = albums.sumOf { it.songCount }
    val albumCount
        get() = albums.size
}