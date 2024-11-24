package meloplayer.app.db

import android.content.Context
import android.os.Build
import android.provider.BaseColumns
import android.provider.MediaStore
import androidx.compose.ui.util.fastDistinctBy
import com.simplecityapps.ktaglib.AudioProperties
import com.simplecityapps.ktaglib.KTagLib
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import meloplayer.app.playbackx.songIdToUri
import meloplayer.app.db.entities.Album
import meloplayer.app.db.entities.Artist
import meloplayer.app.db.entities.Genre
import meloplayer.app.db.entities.Song
import meloplayer.app.db.entities.SongArtist
import meloplayer.app.db.entities.SongGenre
import meloplayer.core.store.MediaStoreUtils.getArtworkUriForSong
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime

class SyncManager(
    private val context: Context,
    private val db: MeloDB
) {
    private val taglib = KTagLib()
    private val artistsSeparators: List<Char> = listOf(';', ',', '&')
    private val albumArtistsSeparators: List<Char> = listOf(';', ',', '&')
    private val genreSeparators: List<Char> = listOf(';', ',', '&')

    //////STEP 1: take items from media store///////////////////////////////////////////////////////
    data class MediaStoreProperties(
        val id: Long,
        val path: String,
        val dateModified: Long
    )

    class ContentResolverQueryNullException : RuntimeException()


    private fun getSongsMediaStoreProperties(): Result<List<MediaStoreProperties>> {
        val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }
        try {
            val cursor = context.contentResolver.query(
                uri,
                arrayOf(
                    BaseColumns._ID, // 0
                    MediaStore.Audio.Media.DATA, // 1
                    MediaStore.Audio.AudioColumns.DATE_MODIFIED, // 2
                ),
                null,
                null,
                null
            ) ?: return Result.failure(ContentResolverQueryNullException())
            val songs = generateSequence { if (cursor.moveToNext()) cursor else null }
                .map {
                    val id = cursor.getLong(
                        cursor.getColumnIndexOrThrow(MediaStore.Audio.AudioColumns._ID)
                    )
                    val data = cursor.getString(
                        cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
                    )
                    val dateModified = cursor.getLong(
                        cursor.getColumnIndexOrThrow(
                            MediaStore.Audio.AudioColumns.DATE_MODIFIED
                        )
                    )
                    MediaStoreProperties(
                        id = id,
                        path = data,
                        dateModified = dateModified
                    )
                }
                .toList()
            cursor.close()
            return Result.success(songs)
        } catch (ex: SecurityException) {
            return Result.failure(ex)
        }
    }

    //////STEP 2: take metadata tags details////////////////////////////////////////////////////////
    data class TagDetails(
        val title: String?,
        val album: String?,
        val artists: List<String>,
        val albumArtists: List<String>,
        val genres: List<String>,
        val trackNumber: Int?,
        val diskNumber: Int?,
        val subtitle: String?
    )

    class MetadataParseFailureException : RuntimeException()
    class AudioPropertiesParseFailureException : RuntimeException()
    class FileDescriptorOpeningFailureException : RuntimeException()

    private suspend fun mapMediaStorePropsWithTagValues(
        list: List<MediaStoreProperties>
    ): List<Triple<MediaStoreProperties, TagDetails, AudioProperties>> {
        return coroutineScope {
            val pairList =
                mutableListOf<Triple<MediaStoreProperties, TagDetails, AudioProperties>>()
            list.map {
                async {
                    val tagAudioPropsPair = getTagValuesOne(it)
                    //log here error
                    tagAudioPropsPair.getOrNull()?.let { (tagDetails, audioDetails) ->
                        pairList.add(Triple(it, tagDetails, audioDetails))
                    }
                }
            }.awaitAll()
            return@coroutineScope pairList
        }
    }

    private fun getTagValuesOne(mediaStoreProperties: MediaStoreProperties): Result<Pair<TagDetails, AudioProperties>> {
        val uri = songIdToUri(mediaStoreProperties.id)
        return context.contentResolver.openFileDescriptor(uri, "r")?.use { fd ->
            val metadataX = taglib.getMetadata(fd.dup().detachFd()) ?: kotlin.run {
                return Result.failure(MetadataParseFailureException())
            }
            val audioProperties = metadataX.audioProperties ?: run {
                return Result.failure(AudioPropertiesParseFailureException())
            }
            val metadata = metadataX.propertyMap
            val getAttrsOfPropMap =
                { mp: Map<String, List<String>>, attrKey: String, separators: List<Char> ->
                    mp[attrKey]?.map {
                        if (separators.isEmpty()) listOf(it)
                        else it.split(*separators.toCharArray())
                    }?.flatten() ?: listOf()
                }


            val title = getAttrsOfPropMap(metadata, "TITLE", listOf()).firstOrNull()?.trim()
            val album = getAttrsOfPropMap(metadata, "ALBUM", listOf()).firstOrNull()?.trim()
            val artists =
                getAttrsOfPropMap(metadata, "ARTIST", artistsSeparators).fastDistinctBy { it }
                    .map { it.trim() }
            val albumArtists =
                getAttrsOfPropMap(
                    metadata,
                    "ALBUMARTIST",
                    albumArtistsSeparators
                ).fastDistinctBy { it }.map { it.trim() }
            val genres = getAttrsOfPropMap(metadata, "GENRE", listOf()).fastDistinctBy { it }
                .map { it.trim() }
            val trackNumber =
                getAttrsOfPropMap(metadata, "TRACKNUMBER", listOf()).firstOrNull()?.trim()
                    ?.toIntOrNull()
            val diskNumber =
                getAttrsOfPropMap(metadata, "DISCNUMBER", listOf()).firstOrNull()?.trim()
                    ?.toIntOrNull()
            val subtitle = getAttrsOfPropMap(metadata, "SUBTITLE", listOf()).firstOrNull()?.trim()

            //TODO: other useful tags
            listOf(
                "DATE",
                "COMPOSER",
                "LYRICIST",
                "COPYRIGHT",
            )
            println("")

            return@use Result.success(
                Pair(
                    TagDetails(
                        title,
                        album,
                        artists,
                        albumArtists,
                        genres,
                        trackNumber,
                        diskNumber,
                        subtitle
                    ), audioProperties
                )
            )
        } ?: return Result.failure(FileDescriptorOpeningFailureException())
    }

    suspend fun syncDatabaseWithFileSystem() {
        println("syncDatabaseWithFileSystem start")
        withContext(Dispatchers.IO) {
            val syncDao = db.syncDao()
            val times = mutableListOf<Pair<String, LocalDateTime>>()
            times.add(Pair("init", LocalDateTime.now()))
            val mediaStoreRes = getSongsMediaStoreProperties()

            println("mediaStoreRes: $mediaStoreRes")
            times.add(Pair("media_store_get", LocalDateTime.now()))
            //log error if there is
            if (mediaStoreRes.getOrNull() == null) {
                return@withContext
            }
            val detailPairs = mapMediaStorePropsWithTagValues(mediaStoreRes.getOrThrow())
            println("detailPairsAlbum: ${detailPairs.map { it.second.album }}")
            println("detailPairsArtist: ${detailPairs.map { it.second.artists }}")
            println("detailPairsGenre: ${detailPairs.map { it.second.genres }}")
            times.add(Pair("tag_lib_get", LocalDateTime.now()))


            //val existingSongs = syncDao.existingSongIds()
            var existingAlbums = syncDao.existingAlbumIdNamePairs()
                .associate { Pair(it.first, it.second) }
            var existingArtists = syncDao.existingArtistIdNamePairs()
                .associate { Pair(it.first, it.second) }
            var existingGenres = syncDao.existingGenreIdNamePairs()
                .associate { Pair(it.first, it.second) }

            println("existingArtists: $existingArtists")
            println("existingAlbums: $existingAlbums")
            println("existingGenres: $existingGenres")
            times.add(Pair("existing_details_get", LocalDateTime.now()))


            val albumNamesToCreate = mutableListOf<Album>()
            val artistNamesToCreate = mutableListOf<Artist>()
            val genreNamesToCreate = mutableListOf<Genre>()

            detailPairs.forEach { (_, tagDetails, _) ->
                if (tagDetails.album != null && !existingAlbums.containsKey(tagDetails.album)) {
                    albumNamesToCreate.add(Album(title = tagDetails.album))
                }
                tagDetails.artists.forEach { oneName ->
                    if (!existingArtists.containsKey(oneName)) {
                        artistNamesToCreate.add(Artist(name = oneName))
                    }
                }
                tagDetails.albumArtists.forEach { oneName ->
                    if (!existingArtists.containsKey(oneName)) {
                        artistNamesToCreate.add(Artist(name = oneName))
                    }
                }
                tagDetails.genres.forEach { oneName ->
                    if (!existingGenres.containsKey(oneName)) {
                        genreNamesToCreate.add(Genre(name = oneName))
                    }
                }
            }
            println("albumNamesToCreate: $albumNamesToCreate")
            println("genreNamesToCreate $genreNamesToCreate")
            println("artistNamesToCreate: $artistNamesToCreate")
            times.add(Pair("get_album_artist_genre_to_create", LocalDateTime.now()))


            syncDao.insertAlbumsOrIgnoreIfPresent(albumNamesToCreate)
            syncDao.insertArtistsOrIgnoreIfPresent(artistNamesToCreate)
            syncDao.insertGenresOrIgnoreIfPresent(genreNamesToCreate)

            times.add(Pair("done_album_artist_genre_to_create", LocalDateTime.now()))


            existingAlbums =
                syncDao.existingAlbumIdNamePairs().associate { Pair(it.first, it.second) }
            existingArtists = syncDao.existingArtistIdNamePairs()
                .associate { Pair(it.first, it.second) }
            existingGenres = syncDao.existingGenreIdNamePairs()
                .associate { Pair(it.first, it.second) }

            times.add(Pair("existing_details_get_re", LocalDateTime.now()))


            val songArtistsCrossRefsToInsert = mutableListOf<SongArtist>()
            val songGenresCrossRefsToInsert = mutableListOf<SongGenre>()
            val songsToInsert = mutableListOf<Song>()

            println("existingArtists: $existingArtists")
            println("existingAlbums: $existingAlbums")
            println("existingGenres: $existingGenres")
            detailPairs.forEach { (mediaStoreProperties, tagDetails, audioProperties) ->
                val songId = mediaStoreProperties.id
                val albumId = if (tagDetails.album != null) {
                    existingAlbums[tagDetails.album]//!!
                } else {
                    null
                }
                val onlySongArtistsMap = tagDetails.artists.associateWith { 1 }
                val onlySongAlbumArtistsMap = tagDetails.artists.associateWith { 1 }
                (tagDetails.albumArtists + tagDetails.artists)
                    .associateWith { artist ->
                        Pair(
                            onlySongArtistsMap.containsKey(artist),
                            onlySongAlbumArtistsMap.containsKey(artist)
                        )
                    }
                    .forEach thisX@{ artistName, (isSongArtist, isAlbumArtist) ->
                        val artistId = existingArtists[artistName] ?: return@thisX
                        songArtistsCrossRefsToInsert.add(
                            SongArtist(
                                songId = songId,
                                artistId = artistId,
                                isSongArtist = isSongArtist,
                                isAlbumArtist = isAlbumArtist
                            )
                        )
                    }
                tagDetails.genres.forEach thisX@{ oneName ->
                    val genreId = existingGenres[oneName] ?: return@thisX//!!
                    songGenresCrossRefsToInsert.add(SongGenre(songId, genreId))
                }
                songsToInsert.add(
                    Song(
                        id = mediaStoreProperties.id,
                        title = tagDetails.title ?: mediaStoreProperties.path
                            .substringAfterLast("/", missingDelimiterValue = ""),
                        fileSystemPath = mediaStoreProperties.path,
                        lengthMs = audioProperties.duration,
                        bitRateKbps = audioProperties.bitrate,
                        sampleRateHz = audioProperties.sampleRate,
                        channelsCount = audioProperties.channelCount,
                        coverImageUri = getArtworkUriForSong(mediaStoreProperties.id).toString(),
                        trackNumber = tagDetails.trackNumber,
                        cdNumber = tagDetails.diskNumber,
                        albumId = albumId,
                        subtitle = tagDetails.subtitle,
                        dateModified = mediaStoreProperties.dateModified
                    )
                )
            }

            times.add(Pair("get_song_cross_refs_to_insert", LocalDateTime.now()))


            syncDao.insertSongs(songsToInsert)
            syncDao.insertSongArtistCrossRefs(songArtistsCrossRefsToInsert)
            syncDao.insertSongGenreCrossRefs(songGenresCrossRefsToInsert)


            times.add(Pair("done_song_cross_refs_to_insert", LocalDateTime.now()))

            for (i in 1 until times.size) {
                val name = times[i].first
                val tookTimeMs = Duration.between(times[i - 1].second, times[i].second).toMillis()
                println("$name took ${tookTimeMs}ms")
            }
        }
    }

}