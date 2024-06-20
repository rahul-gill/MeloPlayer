package meloplayer.core.store.repo

import meloplayer.core.store.MediaStoreSongsFetcher
import meloplayer.core.store.model.Album
import meloplayer.core.store.model.AlbumSortOrder
import meloplayer.core.store.model.Genre
import meloplayer.core.store.model.GenreSortOrder
import meloplayer.core.store.model.MediaStoreSong
import java.text.Collator

interface GenreRepository {
    fun genres(query: String, genreSortOrder: GenreSortOrder? = null): Result<List<Genre>>

    fun genres(genreSortOrder: GenreSortOrder? = null): Result<List<Genre>>

    fun songs(genreId: Long): Result<List<MediaStoreSong>>


    companion object {
//        fun getImpl(
//            fetcher: MediaStoreSongsFetcher
//        ): GenreRepository = GenreRepositoryImpl(fetcher)
    }
}
