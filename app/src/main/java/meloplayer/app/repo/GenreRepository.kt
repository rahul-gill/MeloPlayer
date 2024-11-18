package meloplayer.app.repo

import meloplayer.app.db.entities.Genre
import meloplayer.app.db.entities.Song
import meloplayer.app.parts.genres.list.GenreSortOrder

interface GenreRepository {
    fun genres(query: String, genreSortOrder: GenreSortOrder? = null): Result<List<Genre>>

    fun genres(genreSortOrder: GenreSortOrder? = null): Result<List<Genre>>

    fun songs(genreId: Long): Result<List<Song>>
}
