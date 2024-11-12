package meloplayer.app.store

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import androidx.compose.ui.util.fastDistinctBy
import com.google.common.collect.Lists
import com.simplecityapps.ktaglib.AudioProperties
import com.simplecityapps.ktaglib.KTagLib
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import meloplayer.app.db.MeloDatabase
import meloplayer.app.db.SchemaQueries
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime


data class FetchDetails(
    val title: String?,
    val album: String?,
    val artists: List<String>,
    val albumArtists: List<String>,
    val genres: List<String>,
    val trackNumber: String?,
    val diskNumber: String?,
    val subtitle: String?
)
data class MediaStoreProperties(
    val id: Long,
    val path: String,
    val dateModified: Instant
)
data class AudioProperties(
    val duration: Int,
    val bitrate: Int,
    val sampleRate: Int,
    val channelCount: Int
)

object MetadataDBPopulate {

    suspend fun refreshMetadataDatabase(
        context: Context,
        db: MeloDatabase,
        propsList: List<MediaStoreFetcherUtil.MediaStoreProperties>,
        artistsSeparators: List<Char> = listOf(';', ',', '&')
    ) {
        withContext(Dispatchers.IO) {

            val q = db.schemaQueries
            val startTime = LocalDateTime.now()
            val nullDetailsSongsPaths = mutableListOf<String>()
            val nullPropertiesSongsPaths = mutableListOf<String>()
            val taglib = KTagLib()

            val t1 = LocalDateTime.now()
            println("Before starting metadata fetch $t1")
            val x = propsList.fastDistinctBy { it.path }.stream().parallel().map { songItem ->
                val uri = songIdToUri(songItem.id)
                context.contentResolver.openFileDescriptor(uri, "r")?.use { fd ->
                    val p = fetchFileDetails(
                        taglib,
                        fd,
                        nullDetailsSongsPaths,
                        songItem,
                        nullPropertiesSongsPaths,
                        artistsSeparators
                    )
                    if(p == null){
                        null
                    } else {
                        val (audioProperties, fetchDetails) = p
                        Triple(songItem, audioProperties, fetchDetails)
                    }
                }
            }

            val t2 = LocalDateTime.now()
            println("After metadata fetch $t2")
            println("took time = ${Duration.between(t1,t2).toMillis()}ms in metadata fetch")

            val t1_x = LocalDateTime.now()
            println("Before starting db_insert $t1_x")
            try {
                db.transaction {
                    x.forEach { p ->
                        if(p != null){
                            val (songItem, audioProperties, fetchDetails) = p
                            doSingleItemInserts(fetchDetails, q, songItem, audioProperties)
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            val t2_x = LocalDateTime.now()
            println("After db_insert $t2_x")
            println("took time = ${Duration.between(t1_x,t2_x).toMillis()}ms in db_insert")
            //cleanup for deleted songs
            val t1_y = LocalDateTime.now()
            println("Before starting db_cleanup $t1_y")
            db.transaction {
                val deletedSongIds = q.findDeletedSongs(propsList.map { it.id }).executeAsList()
                q.deleteRecordsForDeletedSongs1(deletedSongIds)
                q.deleteRecordsForDeletedSongs2(deletedSongIds)
                q.deleteRecordsForDeletedSongs3(deletedSongIds)
                q.deleteOrphanedRecords1()
                q.deleteOrphanedRecords2()
                q.deleteOrphanedRecords3()
            }
            val t2_y = LocalDateTime.now()
            println("After db_cleanup $t2_y")
            println("took time = ${Duration.between(t1_y,t2_y).toMillis()}ms in db_cleanup")



            val endTime = LocalDateTime.now()
            println("Song paths for which details null: $nullDetailsSongsPaths")
            println("Song paths for which properties null: $nullPropertiesSongsPaths")
            println(
                "Took ${
                    Duration.between(startTime, endTime).toMillis()
                }ms to refresh; items: ${propsList.size}"
            )
        }
    }

    private fun fetchFileDetails(
        taglib: KTagLib,
        fd: ParcelFileDescriptor,
        nullDetailsSongsPaths: MutableList<String>,
        songItem: MediaStoreFetcherUtil.MediaStoreProperties,
        nullPropertiesSongsPaths: MutableList<String>,
        artistsSeparators: List<Char>
    ): Pair<AudioProperties, FetchDetails>? {
        val metadataX = taglib.getMetadata(fd.dup().detachFd()) ?: kotlin.run {
            nullDetailsSongsPaths.add(songItem.path)
            return null
        }
        val audioProperties = metadataX.audioProperties ?: run {
            nullPropertiesSongsPaths.add(songItem.path)
            return null
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
        val artists = getAttrsOfPropMap(metadata, "ARTIST").fastDistinctBy { it }.map { it.trim() }
        val albumArtists =
            getAttrsOfPropMap(metadata, "ALBUMARTIST").fastDistinctBy { it }.map { it.trim() }
        val genres = getAttrsOfPropMap(metadata, "GENRE").fastDistinctBy { it }.map { it.trim() }
        val trackNumber = getAttrsOfPropMap(metadata, "TRACKNUMBER").firstOrNull()?.trim()
        val diskNumber = getAttrsOfPropMap(metadata, "DISCNUMBER").firstOrNull()?.trim()
        val subtitle = getAttrsOfPropMap(metadata, "SUBTITLE").firstOrNull()?.trim()


        val fetchDetails = FetchDetails(
            title,
            album,
            artists,
            albumArtists,
            genres,
            trackNumber,
            diskNumber,
            subtitle
        )
        return Pair(audioProperties, fetchDetails)
    }

    private fun doSingleItemInserts(
        fetchDetails: FetchDetails,
        q: SchemaQueries,
        songItem: MediaStoreFetcherUtil.MediaStoreProperties,
        audioProperties: AudioProperties
    ): Boolean {
        val albumId = if (fetchDetails.album != null) {
            q.insertAlbum(
                fetchDetails.album,
                null,
                cover_image_uri = getArtworkUriForSong(songItem.id).toString()
            )
            q.lastInsertRowId().executeAsOneOrNull()
        } else {
            null
        }

        q.insertOrReplaceSong(
            song_id = songItem.id,
            title = fetchDetails.title
                ?: songItem.path.substringAfterLast("/", missingDelimiterValue = "")
                    .ifBlank { "Unknown title" },
            album_id = albumId,
            file_system_path = songItem.path,
            bit_rate_kbps = audioProperties.bitrate.toLong(),
            sample_rate_hz = audioProperties.sampleRate.toLong(),
            length_ms = audioProperties.duration.toLong(),
            channels_count = audioProperties.channelCount.toLong(),
            cd_number = fetchDetails.diskNumber?.toLongOrNull(),
            track_number = fetchDetails.trackNumber?.toLongOrNull(),
            cover_image_uri = getArtworkUriForSong(songItem.id).toString(),
            subtitle = fetchDetails.subtitle,
            date_modified = songItem.dateModified
        )

        // Batch checking for artist and genre existence
        val artistNames = fetchDetails.artists + fetchDetails.albumArtists
        val existingArtistsMap = q.getArtistsByNames(artistNames).executeAsList().associateBy { it.name }

        // Insert or update artists
        fetchDetails.artists.forEach { artistName ->
            val existingArtist = existingArtistsMap[artistName]
            if (existingArtist != null) {
                q.setArtistIsSongArtist(true, existingArtist.artist_id)
            } else {
                q.insertArtist(artistName, null, null, true, false)
            }
        }

        fetchDetails.albumArtists.forEach { albumArtistName ->
            val existingArtist = existingArtistsMap[albumArtistName]
            if (existingArtist != null) {
                q.setArtistIsAlbumArtist(true, existingArtist.artist_id)
            } else {
                q.insertArtist(albumArtistName, null, null, false, true)
            }
        }

        // Handle genres
        val existingGenresMap = q.getGenresByNames(fetchDetails.genres).executeAsList().associateBy { it.name }
        fetchDetails.genres.forEach { genreName ->
            val existingGenre = existingGenresMap[genreName]
            if (existingGenre == null) {
                q.insertGenre(genreName, null)
            }
        }

        // Now perform batch inserts into song_artists and song_genres
        val artistIds = q.getArtistIdsByNames(fetchDetails.artists).executeAsList()
        val albumArtistIds = q.getArtistIdsByNames(fetchDetails.albumArtists).executeAsList()
        val genreIds = q.getGenreIdsByNames(fetchDetails.genres).executeAsList()

        genreIds.forEach { q.insertOrIgnoreInSongGenre(songItem.id, it) }
        artistIds.forEach { q.insertOrIgnoreInSongArtist(songItem.id, it) }
        albumArtistIds.forEach { q.insertOrIgnoreInSongArtist(songItem.id, it) }

        return true
    }



    fun getArtworkUriForSong(songId: Long): Uri =
        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI.buildUpon()
            .appendPath(songId.toString())
            .appendPath("albumart").build()

    fun songIdToUri(songId: Long) = ContentUris.withAppendedId(
        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, songId
    )
}