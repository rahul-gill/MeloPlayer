package meloplayer.core.store.repo

import meloplayer.core.store.MediaStoreSongsFetcher
import meloplayer.core.store.model.Album
import meloplayer.core.store.model.AlbumSortOrder
import meloplayer.core.store.model.MediaStoreSong
import meloplayer.core.store.model.SongFilter
import java.text.Collator


interface AlbumRepository {
    fun albums(
        sortOrder: AlbumSortOrder? = null
    ): Result<List<Album>>

    fun albums(
        query: String,
        sortOrder: AlbumSortOrder? = null
    ): Result<List<Album>>

    fun album(albumId: Long): Result<Album>

    companion object {
        val instance: AlbumRepository by lazy {
            AlbumRepositoryImpl(MediaStoreSongsFetcher.instance)
        }

        fun getImpl(
            fetcher: MediaStoreSongsFetcher
        ): AlbumRepository = AlbumRepositoryImpl(fetcher)
    }

    class AlbumNotFoundException : RuntimeException()
}

private class AlbumRepositoryImpl(
    private val fetcher: MediaStoreSongsFetcher
) : AlbumRepository {
    override fun albums(
        sortOrder: AlbumSortOrder?
    ): Result<List<Album>> {
        val songsResult = fetcher.getSongs(
            filters = listOf(),
            sortOrder = null
        )
        return songResultToAlbums(songsResult, sortOrder)
    }

    override fun albums(query: String, sortOrder: AlbumSortOrder?): Result<List<Album>> {
        val songsResult = fetcher.getSongs(
            filters = listOf(SongFilter.AlbumName(query)),
            sortOrder = null
        )
        return songResultToAlbums(songsResult, sortOrder)
    }

    override fun album(albumId: Long): Result<Album> {
        val songsResult = fetcher.getSongs(
            filters = listOf(SongFilter.AlbumIdExact(albumId)),
            sortOrder = null
        )
        val albumResult = songResultToAlbums(songsResult).map { it.firstOrNull() }
        return when {
            albumResult.isFailure ->
                Result.failure(albumResult.exceptionOrNull()!!)

            albumResult.getOrNull() == null ->
                Result.failure(AlbumRepository.AlbumNotFoundException())

            else -> albumResult.map { it!! }
        }
    }


    private fun songResultToAlbums(
        songsResult: Result<List<MediaStoreSong>>,
        sortOrder: AlbumSortOrder? = null
    ): Result<List<Album>> {
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

}