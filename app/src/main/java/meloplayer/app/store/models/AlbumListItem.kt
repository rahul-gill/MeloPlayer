package meloplayer.app.store.models

import android.net.Uri

data class AlbumListItem(
    val albumId: Long,
    val albumName: String,
    val albumArtUri: String?,
    val artistNames: String?,
    val songCount: Long,
    val songIds: String?
)