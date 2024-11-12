package meloplayer.app.store

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import androidx.compose.ui.util.fastDistinctBy
import com.simplecityapps.ktaglib.AudioProperties
import com.simplecityapps.ktaglib.KTagLib
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import meloplayer.app.db.MeloDatabase
import java.time.Duration
import java.time.LocalDateTime


object MetadataDBPopulate {

    suspend fun refreshMetadataDatabase(
        context: Context,
        db: MeloDatabase,
        propsList: List<MediaStoreProperties>,
        artistsSeparators: List<Char> = listOf(';', ',', '&')
    ) {
        withContext(Dispatchers.IO){
            println("Starting refresh of db")
            val q = db.schemaQueries
            val startTime = LocalDateTime.now()
            val nullDetailsSongsPaths = mutableListOf<String>()
            val nullPropertiesSongsPaths = mutableListOf<String>()
            val taglib = KTagLib()
            var index = 0
            var totalMs: Long = 0
            var totalRecords: Long = 0

            for (songItem in propsList) {
                val start = LocalDateTime.now()
                try {
                    db.transaction {
                        val uri = songIdToUri(songItem.id)
                        context.contentResolver.openFileDescriptor(uri, "r")?.use { fd ->
                            val metadataX = taglib.getMetadata(fd.dup().detachFd()) ?: kotlin.run {
                                nullDetailsSongsPaths.add(songItem.path)
                                return@use
                            }
                            val audioProperties = metadataX.audioProperties ?: run {
                                nullPropertiesSongsPaths.add(songItem.path)
                                return@use
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
                            val albumArtists = getAttrsOfPropMap(metadata, "ALBUMARTIST").fastDistinctBy { it }.map { it.trim() }
                            val genres = getAttrsOfPropMap(metadata, "GENRE").fastDistinctBy { it }.map { it.trim() }
                            val trackNumber = getAttrsOfPropMap(metadata, "TRACKNUMBER").firstOrNull()?.trim()
                            val diskNumber = getAttrsOfPropMap(metadata, "DISCNUMBER").firstOrNull()?.trim()
                            val subtitle = getAttrsOfPropMap(metadata, "SUBTITLE").firstOrNull()?.trim()

                            val albumId = if (album != null) {
                                q.insertOrIgnoreAlbum(album, null, cover_image_uri = getArtworkUriForSong(songItem.id).toString())
                                q.lastInsertRowId().executeAsOneOrNull()
                            } else {
                                null
                            }
                            q.insertOrReplaceSong(
                                song_id = songItem.id,
                                title = title
                                    ?: songItem.path.substringAfterLast("/", missingDelimiterValue = "")
                                        .ifBlank { "Unknown title" },
                                album_id = albumId,
                                file_system_path = songItem.path,
                                bit_rate_kbps = audioProperties.bitrate.toLong(),
                                sample_rate_hz = audioProperties.sampleRate.toLong(),
                                length_ms = audioProperties.duration.toLong(),
                                channels_count = audioProperties.channelCount.toLong(),
                                cd_number = diskNumber?.toLongOrNull(),
                                track_number = trackNumber?.toLongOrNull(),
                                cover_image_uri = getArtworkUriForSong(songItem.id).toString(),
                                subtitle = subtitle,
                                date_modified = songItem.dateModified
                            )
                            //TODO: fix O(n) queries
                            val artistsExist =
                                artists.map {
                                    val idsWithArtistName = q.artistExistsWithName(it)
                                        .executeAsList()
                                    if(idsWithArtistName.size > 1){
                                        Pair(it, null)
                                    } else if(idsWithArtistName.size == 1){
                                        Pair(it, idsWithArtistName.first())
                                    } else {
                                        Pair(it, null)
                                    }
                                }
                                    .toMutableList()
                            artistsExist.forEachIndexed { index, (name, existingId) ->
                                var newId: Long? = null
                                if (existingId != null) {
                                    //no need to update just the name, because its already same

                                    q.setArtistIsSongArtist(true, existingId)
                                } else {

                                    q.insertArtistOrIgnore(
                                        name, null, null, is_song_artist = true,
                                        is_album_artist = false,
                                    )
                                    newId = q.lastInsertRowId().executeAsOne()
                                }
                                q.insertOrIgnoreInSongArtist(songItem.id, existingId ?: newId)
                            }


                            val albumArtistsExist = albumArtists.map {
                                val idsWithArtistName = q.artistExistsWithName(it)
                                    .executeAsList()
                                if(idsWithArtistName.size > 1){
                                    Pair(it, null)
                                } else if(idsWithArtistName.size == 1){
                                    Pair(it, idsWithArtistName.first())
                                } else {
                                    Pair(it, null)
                                }
                            }.toMutableList()
                            albumArtistsExist.forEachIndexed { index, (name, existingId) ->
                                var newId: Long? = null
                                if (existingId != null) {
                                    //no need to update just the name, because its already same

                                    q.setArtistIsAlbumArtist(true, existingId)
                                } else {

                                    q.insertArtistOrIgnore(
                                        name, null, null, is_song_artist = false,
                                        is_album_artist = true,
                                    )
                                    newId = q.lastInsertRowId().executeAsOne()
                                }
                                q.insertOrIgnoreInSongArtist(songItem.id, newId)

                            }

                            val genresExist =
                                genres.map {
                                    val idsWithArtistName = q.genreExistsWithName(it)
                                        .executeAsList()
                                    if(idsWithArtistName.size > 1){
                                        Pair(it, null)
                                    } else if(idsWithArtistName.size == 1){
                                        Pair(it, idsWithArtistName.first())
                                    } else {
                                        Pair(it, null)
                                    }
                                }
                                    .toMutableList()
                            genresExist.forEachIndexed { index, (name, existingId) ->
                                //no need to update just the name, because its already same
                                var newId: Long? = null
                                if (existingId == null) {
                                    q.insertGenreOrIgnore(name, null)
                                    newId = q.lastInsertRowId().executeAsOne()
                                }
                                q.insertOrIgnoreInSongGenre(song_id = songItem.id, existingId ?: newId)
                            }


                        }
                    }
                } catch (e: Exception){
                    e.printStackTrace()
                }

                val end = LocalDateTime.now()
                val time = Duration.between(start , end).toMillis()
                totalMs += time
                println("Done ${index+1}/${propsList.size} avg_time_ms_per_record = ${totalMs * 1.0/(index+1)}")
                index += 1
            }
            //cleanup for deleted songs
            db.transaction {
                val deletedSongIds = q.findDeletedSongs(propsList.map { it.id }).executeAsList()
                q.deleteRecordsForDeletedSongs1(deletedSongIds)
                q.deleteRecordsForDeletedSongs2(deletedSongIds)
                q.deleteRecordsForDeletedSongs3(deletedSongIds)
                q.deleteOrphanedRecords1()
                q.deleteOrphanedRecords2()
                q.deleteOrphanedRecords3()
            }
            val endTime = LocalDateTime.now()
            println("Song paths for which details null: $nullDetailsSongsPaths")
            println("Song paths for which properties null: $nullPropertiesSongsPaths")
            println("Took ${Duration.between(startTime ,endTime).toMillis()}ms to refresh; items: ${propsList.size}")
        }
    }


    fun getArtworkUriForSong(songId: Long): Uri =
        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI.buildUpon()
            .appendPath(songId.toString())
            .appendPath("albumart").build()

    fun songIdToUri(songId: Long) = ContentUris.withAppendedId(
        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, songId
    )
}