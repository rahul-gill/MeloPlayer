package meloplayer.app.repo

import meloplayer.app.db.entities.Artist
import meloplayer.app.parts.artists.list.ArtistsSortOrder

interface ArtistRepository {
    fun artists(sortOrder: ArtistsSortOrder? = null): Result<List<Artist>>

    fun artists(query: String, sortOrder: ArtistsSortOrder? = null): Result<List<Artist>>

    fun artist(artistId: Long, sortOrder: ArtistsSortOrder? = null): Result<Artist>

    class ArtistNotFoundException : RuntimeException()

}
