package meloplayer.core.store.repo

import meloplayer.core.store.MediaStoreSongsFetcher
import meloplayer.core.store.model.Album
import meloplayer.core.store.model.Artist
import meloplayer.core.store.model.ArtistsSortOrder
import meloplayer.core.store.model.MediaStoreSong
import meloplayer.core.store.model.SongFilter
import java.text.Collator

interface ArtistRepository {
    fun artists(sortOrder: ArtistsSortOrder? = null): Result<List<Artist>>

    fun artists(query: String, sortOrder: ArtistsSortOrder? = null): Result<List<Artist>>

    fun artist(artistId: Long, sortOrder: ArtistsSortOrder? = null): Result<Artist>

    class ArtistNotFoundException : RuntimeException()

    companion object {

        val instance: ArtistRepository by lazy {
            ArtistRepositoryImpl(MediaStoreSongsFetcher.instance, { false })
        }
        fun getImpl(
            fetcher: MediaStoreSongsFetcher,
            shouldFetchAlbumArtistTag: () -> Boolean
        ): ArtistRepository = ArtistRepositoryImpl(fetcher, shouldFetchAlbumArtistTag)
    }
}

private class ArtistRepositoryImpl(
    private val fetcher: MediaStoreSongsFetcher,
    private var shouldFetchAlbumArtistTag: () -> Boolean
) : ArtistRepository {

    override fun artists(sortOrder: ArtistsSortOrder?): Result<List<Artist>> {
        val songsResult = fetcher.getSongs(
            filters = listOf(),
            sortOrder = null
        )
        return songResultToArtists(songsResult, sortOrder)
    }

    override fun artists(query: String, sortOrder: ArtistsSortOrder?): Result<List<Artist>> {
        val songsResult = fetcher.getSongs(
            filters = listOf(SongFilter.Artist(query)),
            sortOrder = null
        )
        return songResultToArtists(songsResult, sortOrder)
    }

    override fun artist(artistId: Long, sortOrder: ArtistsSortOrder?): Result<Artist> {
        val songsResult = fetcher.getSongs(
            filters = listOf(),
            sortOrder = null
        )

        val artistResult = songResultToArtists(songsResult, sortOrder).map { it.firstOrNull() }
        return when {
            artistResult.isFailure ->
                Result.failure(artistResult.exceptionOrNull()!!)

            artistResult.getOrNull() == null ->
                Result.failure(ArtistRepository.ArtistNotFoundException())

            else -> artistResult.map { it!! }
        }
    }


    private fun songResultToArtists(
        songsResult: Result<List<MediaStoreSong>>,
        sortOrder: ArtistsSortOrder? = null
    ): Result<List<Artist>> {
        val exp = songsResult.exceptionOrNull()
        if (exp != null) {
            return Result.failure(exp)
        }
        val songs = songsResult.getOrNull()!!

        val artists = if (shouldFetchAlbumArtistTag()) {
            songs
                .filter { it.albumArtist != null }
                .groupBy { it.albumArtist }
                .map { (key, value) ->
                    val albums = value.groupBy { it.albumId }.map { Album(it.key, it.value) }
                    Artist(key!!, albums = albums, isAlbumArtist = true)
                }
        } else {
            val artistNames = songs.flatMap { it.artistNames }
            val albums = songs.groupBy { it.albumId }
                .map { if (it.value.isNotEmpty()) Album(it.key, it.value) else null }
                .filterNotNull()
            artistNames.map { artistName ->
                Artist(
                    name = artistName,
                    albums = albums.filter { album ->
                        album.songs.any { song ->
                            song.artistNames.contains(
                                artistName
                            )
                        }
                    }
                )
            }
        }


        val collator = Collator.getInstance()
        return Result.success(
            when (sortOrder) {
                is ArtistsSortOrder.Name -> artists.sortedWith { a, b ->
                    collator.compare(
                        if (sortOrder.isAscending) a.name else b.name,
                        if (sortOrder.isAscending) b.name else a.name
                    )
                }

                is ArtistsSortOrder.NumberOfSongs -> {
                    if (sortOrder.isAscending) artists.sortedBy { it.songCount }
                    else artists.sortedByDescending { it.songCount }
                }

                is ArtistsSortOrder.NumberOfAlbums -> {
                    if (sortOrder.isAscending) artists.sortedBy { it.albumCount }
                    else artists.sortedByDescending { it.albumCount }
                }

                null -> artists
            }
        )
    }


}