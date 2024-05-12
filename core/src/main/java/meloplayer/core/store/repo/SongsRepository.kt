package meloplayer.core.store.repo

import meloplayer.core.store.MediaStoreSong
import meloplayer.core.store.MediaStoreSongsFetcher
import meloplayer.core.store.SongFilter
import meloplayer.core.store.SongSortOrder
import java.lang.RuntimeException

class SongsRepository(
    private val fetcher: MediaStoreSongsFetcher
) {
    val allSongs
        get() =
            fetcher.getSongs(
                filters = listOf(),
                sortOrder = SongSortOrder.DateModified(isAscending = true)
            )

    fun songsByNameQuery(query: String): Result<List<MediaStoreSong>> {
        return fetcher.getSongs(
            filters = listOf(
                SongFilter.SearchByTitle(query)
            ),
            sortOrder = SongSortOrder.DateModified(isAscending = true)
        )
    }

    fun songById(id: String): Result<MediaStoreSong> {
        val result = fetcher.getSongs(
            filters = listOf(SongFilter.GetOneById(id)),
            sortOrder = SongSortOrder.DateModified(isAscending = true)
        )
        val exp = result.exceptionOrNull()
        return if (exp != null) {
            Result.failure(exp)
        } else if (result.getOrNull()?.isEmpty() != false) {
            Result.failure(SongDoesNotExists())
        } else {
            Result.success(result.getOrNull()!!.first())
        }
    }

    fun songsByPath(path: String, ignoreBlackLists: Boolean = false): Result<List<MediaStoreSong>> {
        return fetcher.getSongs(
            filters = listOf(
                SongFilter.DirectoryPath(path, isExclude = false)
            ),
            sortOrder = SongSortOrder.DateModified(isAscending = true)
        )
    }

    class SongDoesNotExists : RuntimeException()
}