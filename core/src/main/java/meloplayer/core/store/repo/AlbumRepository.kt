package meloplayer.core.store.repo

import meloplayer.core.store.Album
import meloplayer.core.store.AlbumSortOrder
import meloplayer.core.store.MediaStoreSongsFetcher
import meloplayer.core.store.SongSortOrder
import java.text.Collator

class AlbumRepository(
    private val fetcher: MediaStoreSongsFetcher
) {
    fun allAlbums(
        sortOrder: AlbumSortOrder? = null
    ): Result<List<Album>> {
        val songsResult = fetcher.getSongs(
            filters = listOf(),
            sortOrder = null
        )
        val exp = songsResult.exceptionOrNull()
        if (exp != null) {
            return Result.failure(exp)
        }
        val songs = songsResult.getOrNull()!!
        val albums = songs.groupBy { it.albumId }
            .map { if (it.value.isNotEmpty()) Album(it.key, it.value) else null }
            .filterNotNull()
        val collator = Collator.getInstance()
        return Result.success(
            when (sortOrder) {
                is AlbumSortOrder.Name -> albums.sortedWith { a, b ->
                    collator.compare(
                        if (sortOrder.isAscending) a.title else b.title,
                        if (sortOrder.isAscending) b.title else a.title
                    )
                }

                is AlbumSortOrder.NumberOfSongs -> {
                    if (sortOrder.isAscending) albums.sortedBy { it.songCount }
                    else albums.sortedByDescending { it.songCount }
                }

                is AlbumSortOrder.Year -> {
                    if (sortOrder.isAscending) albums.sortedBy { it.year }
                    else albums.sortedByDescending { it.year }
                }

                null -> albums
            }
        )
    }

    fun albumById(id: String){

    }

}