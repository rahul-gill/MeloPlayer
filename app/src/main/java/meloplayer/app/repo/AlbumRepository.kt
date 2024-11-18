package meloplayer.app.repo

import meloplayer.app.db.entities.Album
import meloplayer.app.parts.albums.list.AlbumSortOrder


interface AlbumRepository {
    fun albums(
        sortOrder: AlbumSortOrder? = null
    ): Result<List<Album>>

    fun albums(
        query: String,
        sortOrder: AlbumSortOrder? = null
    ): Result<List<Album>>

    fun album(albumId: Long): Result<Album>

    class AlbumNotFoundException : RuntimeException()
}
