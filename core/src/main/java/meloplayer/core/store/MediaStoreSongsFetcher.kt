package meloplayer.core.store

import android.content.Context
import android.database.Cursor
import android.provider.BaseColumns
import android.provider.MediaStore
import androidx.core.database.getStringOrNull
import java.lang.RuntimeException


/**
 * This interface just queries the
 */
fun interface MediaStoreSongsFetcher {

    /**
     * Can result in either [ContentResolverQueryNullException] or [SecurityException]
     */
    fun getSongs(
        filters: List<SongFilter>,
        sortOrder: SongSortOrder?
    ): Result<List<MediaStoreSong>>

    class ContentResolverQueryNullException : RuntimeException()


    companion object {
        fun getImpl(context: Context): MediaStoreSongsFetcher {
            return MediaStoreSongsFetcherImpl(context)
        }
    }
}


private class MediaStoreSongsFetcherImpl(
    private val context: Context
) : MediaStoreSongsFetcher {
    override fun getSongs(
        filters: List<SongFilter>,
        sortOrder: SongSortOrder?
    ): Result<List<MediaStoreSong>> {
        //${MediaStore.Audio.Media.IS_MUSIC} != 0
        var selection = ""
        val selectionValues = mutableListOf<String>()
        filters.forEach { filter ->
            selection = "$selection AND ${filter.cursorSelectionCondition}"
            selectionValues.add(filter.cursorSelectionConditionParamValue)
        }

        val uri = MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)

        try {
            val cursor = context.contentResolver.query(
                uri,
                BaseProjection,
                selection.ifBlank { null },
                selectionValues.toTypedArray().ifEmpty { null },
                sortOrder?.getOrderByCondition
            ) ?: return Result.failure(MediaStoreSongsFetcher.ContentResolverQueryNullException())
            val songs = generateSequence { if (cursor.moveToNext()) cursor else null }
                .map(::songCursorToSong)
                .filterNotNull()
                .toList()
            cursor.close()
            return Result.success(songs)
        } catch (ex: SecurityException) {
            return Result.failure(ex)
        }
    }

    fun songCursorToSong(cursor: Cursor): MediaStoreSong? {
        try {
            val id =
                cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.AudioColumns._ID))
            val title =
                cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.TITLE))
            val trackNumber =
                cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.TRACK))
            val year =
                cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.YEAR))
            val duration =
                cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.DURATION))
            val data = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA))
            val dateModified =
                cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.DATE_MODIFIED))
            val albumId =
                cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.ALBUM_ID))
            val albumName =
                cursor.getStringOrNull(cursor.getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.ALBUM))
            val artistId =
                cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.ARTIST_ID))
            val artistName =
                cursor.getStringOrNull(cursor.getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.ARTIST))
            val composer =
                cursor.getStringOrNull(cursor.getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.COMPOSER))
            val albumArtist =
                cursor.getStringOrNull(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ARTIST))
            return MediaStoreSong(
                id,
                title,
                trackNumber,
                year,
                duration,
                data,
                dateModified,
                albumId,
                albumName ?: "",
                artistId,
                artistName ?: "",
                composer ?: "",
                albumArtist ?: ""
            )
        } catch (e: IllegalArgumentException) {
            return null
        }
    }


    companion object {
        val BaseProjection = arrayOf(
            BaseColumns._ID, // 0
            MediaStore.Audio.AudioColumns.TITLE, // 1
            MediaStore.Audio.AudioColumns.TRACK, // 2
            MediaStore.Audio.AudioColumns.YEAR, // 3
            MediaStore.Audio.AudioColumns.DURATION, // 4
            MediaStore.Audio.Media.DATA, // 5
            MediaStore.Audio.AudioColumns.DATE_MODIFIED, // 6
            MediaStore.Audio.AudioColumns.ALBUM_ID, // 7
            MediaStore.Audio.AudioColumns.ALBUM, // 8
            MediaStore.Audio.AudioColumns.ARTIST_ID, // 9
            MediaStore.Audio.AudioColumns.ARTIST, // 10
            MediaStore.Audio.AudioColumns.COMPOSER, // 11
            MediaStore.Audio.Media.ALBUM_ARTIST // 12
        )
    }


}


private val SongSortOrder.getOrderByCondition: String
    get() {
        val ascDescCondition = if (isAscending) {
            " ASC "
        } else {
            " DESC "
        }
        return when (this) {
            is SongSortOrder.Album -> " ${MediaStore.Audio.Albums.DEFAULT_SORT_ORDER} $ascDescCondition"
            is SongSortOrder.AlbumArtist -> "${MediaStore.Audio.Media.ALBUM_ARTIST} $ascDescCondition"
            is SongSortOrder.Composer -> "${MediaStore.Audio.Media.COMPOSER} $ascDescCondition"
            is SongSortOrder.Name -> "${MediaStore.Audio.Media.TITLE} $ascDescCondition"
            is SongSortOrder.SongArtist -> "${MediaStore.Audio.Artists.DEFAULT_SORT_ORDER} $ascDescCondition"
            is SongSortOrder.DateAdded -> "${MediaStore.Audio.Media.DATE_ADDED} $ascDescCondition"
            is SongSortOrder.DateModified -> "${MediaStore.Audio.Media.DATE_MODIFIED} $ascDescCondition"
            is SongSortOrder.Duration -> "${MediaStore.Audio.Media.DURATION} $ascDescCondition"
            is SongSortOrder.Year -> "${MediaStore.Audio.Media.YEAR} $ascDescCondition"
        }
    }


//TODO: Top played songs
//TODO: recently added songs


private val SongFilter.cursorSelectionConditionParamValue: String
    get() = when (this) {
        is SongFilter.DurationLessThan -> "$durationMillis"
        is SongFilter.DirectoryPath -> "$folderPath%"
        is SongFilter.DirectoryPathExact -> folderPath
        is SongFilter.GenreExact -> genreName
        is SongFilter.SearchByTitle -> "%$titleQuery%"
        is SongFilter.GetOneById -> id
        is SongFilter.AlbumIdExact -> albumId
        is SongFilter.AlbumName -> "%$album%"
        is SongFilter.Artist -> "%$artist%"
        is SongFilter.ArtistsIdExact -> artistId
        is SongFilter.AlbumArtist -> "%$albumArtist%"
        is SongFilter.AlbumArtistExact -> albumArtist
    }
private val SongFilter.cursorSelectionCondition: String
    get() {
        val likeClause = if (this.isExclude) {
            " LIKE "
        } else {
            " NOT LIKE "
        }
        val lessThanClause = if (this.isExclude) {
            " >= "
        } else {
            " <= "
        }

        return when (this) {
            is SongFilter.DirectoryPath ->
                "${MediaStore.Audio.Media.DATA} $likeClause ?"

            is SongFilter.DirectoryPathExact ->
                "${MediaStore.Audio.Media.DATA} = ?"

            is SongFilter.GenreExact ->
                "${MediaStore.Audio.Media.DATA} $likeClause ?"

            is SongFilter.DurationLessThan ->
                "${MediaStore.Audio.Media.DATA} $lessThanClause ?"

            is SongFilter.SearchByTitle ->
                "${MediaStore.Audio.AudioColumns.TITLE} $likeClause ?"

            is SongFilter.GetOneById ->
                "${BaseColumns._ID} = ?"

            is SongFilter.AlbumIdExact ->
                "${MediaStore.Audio.AudioColumns.ALBUM_ID} = ?"

            is SongFilter.AlbumName ->
                "${MediaStore.Audio.AudioColumns.ALBUM} $likeClause ?"

            is SongFilter.Artist ->
                "${MediaStore.Audio.AudioColumns.ARTIST} $likeClause ?"

            is SongFilter.ArtistsIdExact ->
                "${MediaStore.Audio.AudioColumns.ARTIST_ID} = ?"

            is SongFilter.AlbumArtist ->
                "${MediaStore.Audio.AudioColumns.ALBUM_ARTIST} $likeClause ?"


            is SongFilter.AlbumArtistExact ->
                "${MediaStore.Audio.AudioColumns.ALBUM_ARTIST} = ?"

        }
    }

