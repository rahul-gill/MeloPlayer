package meloplayer.core.store.repo

import meloplayer.core.store.model.MediaStoreSong
import meloplayer.core.store.MediaStoreSongsFetcher
import meloplayer.core.store.model.SongFilter
import meloplayer.core.store.model.SongSortOrder
import java.lang.RuntimeException


interface SongsRepository {
    fun songs(): Result<List<MediaStoreSong>>
    fun songs(query: String): Result<List<MediaStoreSong>>
    fun songsByFilePathRecursive(
        filePath: String,
        ignoreBlackLists: Boolean = true
    ): Result<List<MediaStoreSong>>

    fun songById(id: Long): Result<MediaStoreSong>

    companion object {

        val instance: SongsRepository by lazy {
            SongsRepositoryImpl(MediaStoreSongsFetcher.instance)
        }

        fun getImpl(fetcher: MediaStoreSongsFetcher): SongsRepository = SongsRepositoryImpl(fetcher)
    }

}

private class SongsRepositoryImpl(
    private val fetcher: MediaStoreSongsFetcher
) : SongsRepository {

    fun songsByPath(path: String, ignoreBlackLists: Boolean = false): Result<List<MediaStoreSong>> {
        return fetcher.getSongs(
            filters = listOf(
                SongFilter.DirectoryPathRecursive(path, isExclude = false)
            ),
            sortOrder = SongSortOrder.DateModified(isAscending = true)
        )
    }

    class SongDoesNotExists : RuntimeException()

    override fun songs(): Result<List<MediaStoreSong>> {
        return fetcher.getSongs(
            filters = listOf(),
            sortOrder = SongSortOrder.DateModified(isAscending = true)
        )
    }

    override fun songs(query: String): Result<List<MediaStoreSong>> {
        return fetcher.getSongs(
            filters = listOf(
                SongFilter.SearchByTitle(query)
            ),
            sortOrder = SongSortOrder.DateModified(isAscending = true)
        )
    }

    override fun songsByFilePathRecursive(
        filePath: String,
        ignoreBlackLists: Boolean
    ): Result<List<MediaStoreSong>> {
        return fetcher.getSongs(
            filters = listOf(
                SongFilter.DirectoryPathRecursive(filePath, isExclude = false)
            ),
            sortOrder = SongSortOrder.DateModified(isAscending = true)
        )
    }

    override fun songById(id: Long): Result<MediaStoreSong> {
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
}