package meloplayer.app.store

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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.job
import kotlinx.coroutines.withContext
import meloplayer.app.store.MetadataDBPopulate.getArtworkUriForSong
import meloplayer.app.store.MetadataDBPopulate.songIdToUri
import meloplayer.app.store.db.MediaMetadataDB
import meloplayer.app.store.db.entities.Album
import meloplayer.app.store.db.entities.Artist
import meloplayer.app.store.db.entities.Genre
import meloplayer.app.store.db.entities.Song
import meloplayer.app.store.db.entities.SongArtist
import meloplayer.app.store.db.entities.SongGenre
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.util.concurrent.Callable

data class MediaStoreProperties(
    val id: Long,
    val path: String,
    val dateModified: Instant
)

data class Details(
    val title: String?,
    val album: String?,
    val artists: List<String>,
    val albumArtists: List<String>,
    val genres: List<String>,
    val trackNumber: String?,
    val diskNumber: String?,
    val subtitle: String?,
    val audioProperties: AudioProperties,
    val mediaStoreProperties: MediaStoreProperties
)


class ContentResolverQueryNullException : RuntimeException()

class LibrarySyncManger(
    private val context: Context,
    private val db: MediaMetadataDB,
    private val artistsSeparators: List<Char> = listOf(';', ',', '&')
) {
    private val taglib = KTagLib()
    private val nullDetailsSongsPaths = mutableListOf<String>()
    private val nullPropertiesSongsPaths = mutableListOf<String>()

    private suspend fun <T> logTiming(task: suspend () -> T, name: String = "Unnamed task"): T {
        val startTime = LocalDateTime.now()
        println("Starting task '$name' at $startTime")
        val res = task()
        val endTime = LocalDateTime.now()
        println("Completed task '$name' at $endTime")
        println("task '$name' took ${Duration.between(startTime, endTime).toMillis()}ms")
        return res
    }


    suspend fun steps() {
        val mediaStoreRecords = logTiming(
            name = "1_fetch_media_store_records",
            task = { getMediaStoreRecords() }
        )
        if (mediaStoreRecords.getOrNull() == null) {
            println("Media store records had an error: " + mediaStoreRecords.exceptionOrNull())
            mediaStoreRecords.exceptionOrNull()?.printStackTrace()
        }
        val tagDetails = logTiming(
            name = "2_tag_processing",
            task = { getTagDetails(mediaStoreRecords.getOrNull()!!) }
        )
        logTiming(
            name = "3_sync_db",
            task = { syncDb(tagDetails) }
        )
    }

    suspend fun syncDb(tagDetails: List<Details>) {

        withContext(Dispatchers.IO) {
            val t1 = LocalDateTime.now()
            val existingArtists =
                db.artistDao().getAllArtists().first().associateBy { it.name }.toMutableMap()
            val existingAlbums =
                db.albumDao().getAllAlbums().first().associateBy { it.title }.toMutableMap()
            val existingGenres = db.genreDao().getAllGenres().associateBy { it.name }.toMutableMap()
            val existingSongs =
                db.songDao().getAllSongs().associateBy { it.fileSystemPath }.toMutableMap()
            val t2 = LocalDateTime.now()
            println("t2-t1 = ${Duration.between(t1, t2).toMillis()}")

            // Prepare batch operations
            val artistsToInsert = mutableListOf<Artist>()
            val albumsToInsert = mutableListOf<Album>()
            val genresToInsert = mutableListOf<Genre>()
            val songsToInsert = mutableListOf<Song>()
            // This existingGenres hold the relationships
            val songArtistsToInsert = mutableListOf<SongArtist>()
            val songGenresToInsert = mutableListOf<SongGenre>()


            // Track seen song file paths
            val seenSongPaths = mutableSetOf<String>()


            //albums
            val t3 = LocalDateTime.now()
            for (details in tagDetails) {
                val albumId = if (details.album != null) {
                    existingAlbums[details.album]?.albumId ?: run {
                        val newAlbum = Album(
                            title = details.album,
                            releaseDate = System.currentTimeMillis(),
                            coverImageUri = null
                        )
                        albumsToInsert.add(newAlbum)
                        newAlbum.albumId // This will be set after insertion
                    }
                } else {
                    null
                }
            }
            db.albumDao().insertAlbums(albumsToInsert)
            existingAlbums.clear()
            existingAlbums.putAll(db.albumDao().getAllAlbums().first().associateBy { it.title })

            val t4 = LocalDateTime.now()
            println("albumsTookTime = ${Duration.between(t3, t4).toMillis()}ms")

            // Handle songs
            for (details in tagDetails) {
                val songId = existingSongs[details.mediaStoreProperties.path]?.songId ?: details.mediaStoreProperties.id
                val newSong = Song(
                    songId = songId,
                    title = details.title ?: details.mediaStoreProperties.path.substringAfterLast(
                        "/",
                        missingDelimiterValue = ""
                    )
                        .ifBlank { "Unknown title" },
                    fileSystemPath = details.mediaStoreProperties.path,
                    lengthMs = details.audioProperties.duration.toLong(),
                    bitRateKbps = details.audioProperties.bitrate,
                    sampleRateHz = details.audioProperties.sampleRate,
                    channelsCount = details.audioProperties.channelCount,
                    coverImageUri = getArtworkUriForSong(details.mediaStoreProperties.id).toString(),
                    trackNumber = details.trackNumber?.toIntOrNull(),
                    cdNumber = details.diskNumber?.toIntOrNull(),
                    albumId = if (details.album != null) {
                        existingAlbums[details.album]!!.albumId
                    } else {
                        null
                    },
                    subtitle = details.subtitle,
                    dateModified = System.currentTimeMillis()
                )
                songsToInsert.add(newSong)
                seenSongPaths.add(details.mediaStoreProperties.path)
            }
            db.songDao().insertSongs(songsToInsert)
            existingSongs.clear()
            existingSongs.putAll(db.songDao().getAllSongs().associateBy { it.title })

            val t5 = LocalDateTime.now()
            println("songsTookTime = ${Duration.between(t4, t5).toMillis()}ms")

            // Handle artists
            for (details in tagDetails) {
                for (artistName in details.artists) {
                    existingArtists[artistName]?.artistId ?: run {
                        val newArtist = Artist(
                            name = artistName,
                            isSongArtist = true,
                            isAlbumArtist = true,
                            bio = null,
                            imageUri = null
                        )
                        artistsToInsert.add(newArtist)
                        newArtist.artistId // This will be set after insertion
                    }
                }
            }
            db.artistDao().insertArtists(artistsToInsert)
            existingArtists.clear()
            existingArtists.putAll(db.artistDao().getAllArtists().first().associateBy { it.name })

            for (details in tagDetails) {
                for (artistName in details.artists) {
                    val artistId = existingArtists[artistName]!!.artistId
                    songArtistsToInsert.add(SongArtist(songId = details.mediaStoreProperties.id, artistId = artistId))
                }
            }

            val t6 = LocalDateTime.now()
            println("artistsTookTime = ${Duration.between(t5, t6).toMillis()}ms")

            // Handle genres
            for (details in tagDetails) {
                for (genreName in details.genres) {
                    existingGenres[genreName]?.genreId ?: run {
                        val newGenre = Genre(name = genreName, imageUri = null)
                        genresToInsert.add(newGenre)
                        newGenre.genreId // This will be set after insertion
                    }
                }
            }

            db.genreDao().insertGenres(genresToInsert)
            existingGenres.clear()
            existingGenres.putAll(db.genreDao().getAllGenres().associateBy { it.name })
            for (details in tagDetails) {
                for (genreName in details.genres) {
                    val genreId = existingGenres[genreName]!!.genreId
                    songGenresToInsert.add(SongGenre(songId = details.mediaStoreProperties.id, genreId = genreId))
                }
            }

            val t7= LocalDateTime.now()
            println("genresTookTime = ${Duration.between(t6, t6).toMillis()}ms")


            // Delete orphaned songs
            db.songDao().deleteSongs(
                existingSongs.values.filter { !seenSongPaths.contains(it.fileSystemPath) }
            )

            // Delete orphaned artists, albums, genres
            db.relationsDao().run {
                cleanupAlbumOrphans()
                cleanupGenresOrphans()
                cleanupArtistsOrphans()
            }
            val t8 = LocalDateTime.now()

            println("t8-t7 = ${Duration.between(t7, t8).toMillis()}")
        }
    }


    private suspend fun getTagDetails(propList: List<MediaStoreProperties>): List<Details> {
        return coroutineScope {
            propList.map { propItem ->
                async { fetchTagDetailsSingle(propItem) }
            }.awaitAll().filterNotNull()
        }
    }

    private fun fetchTagDetailsSingle(props: MediaStoreProperties): Details? {
        val uri = songIdToUri(props.id)
        return context.contentResolver.openFileDescriptor(uri, "r")?.use { fd ->
            val metadataX = taglib.getMetadata(fd.dup().detachFd()) ?: kotlin.run {
                nullDetailsSongsPaths.add(props.path)
                return@use null
            }
            val audioProperties = metadataX.audioProperties ?: run {
                nullPropertiesSongsPaths.add(props.path)
                return@use null
            }
            val metadata = metadataX.propertyMap
            val getAttrsOfPropMap = { mp: Map<String, List<String>>, attrKey: String ->
                mp[attrKey]?.map {
                    //TODO: different separators for artists, genres, album artists
                    it.split(*artistsSeparators.toCharArray())
                }?.flatten() ?: listOf()
            }

            val title = getAttrsOfPropMap(metadata, "TITLE").firstOrNull()?.trim()
            val album = getAttrsOfPropMap(metadata, "ALBUM").firstOrNull()?.trim()
            val artists =
                getAttrsOfPropMap(metadata, "ARTIST").fastDistinctBy { it }.map { it.trim() }
            val albumArtists =
                getAttrsOfPropMap(metadata, "ALBUMARTIST").fastDistinctBy { it }.map { it.trim() }
            val genres =
                getAttrsOfPropMap(metadata, "GENRE").fastDistinctBy { it }.map { it.trim() }
            val trackNumber = getAttrsOfPropMap(metadata, "TRACKNUMBER").firstOrNull()?.trim()
            val diskNumber = getAttrsOfPropMap(metadata, "DISCNUMBER").firstOrNull()?.trim()
            val subtitle = getAttrsOfPropMap(metadata, "SUBTITLE").firstOrNull()?.trim()
            return Details(
                title,
                album,
                artists,
                albumArtists,
                genres,
                trackNumber,
                diskNumber,
                subtitle,
                audioProperties,
                props
            )

        }
    }


    private suspend fun getMediaStoreRecords(): Result<List<MediaStoreProperties>> {
        return withContext(Dispatchers.IO) {
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
                ) ?: return@withContext Result.failure(ContentResolverQueryNullException())

                val songs = generateSequence { if (cursor.moveToNext()) cursor else null }
                    .map {
                        val id =
                            cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.AudioColumns._ID))
                        val data =
                            cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA))
                        val dateModified =
                            cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.DATE_MODIFIED))
                        MediaStoreProperties(
                            id = id,
                            path = data,
                            dateModified = Instant.ofEpochMilli(dateModified)
                        )
                    }
                    .toList()
                cursor.close()
                return@withContext Result.success(songs)
            } catch (ex: Exception) {
                return@withContext Result.failure(ex)
            }
        }
    }
}