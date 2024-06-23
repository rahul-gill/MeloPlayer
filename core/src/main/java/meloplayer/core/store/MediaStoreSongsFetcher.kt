package meloplayer.core.store

import android.content.Context
import android.database.Cursor
import android.os.Build
import android.provider.BaseColumns
import android.provider.MediaStore
import androidx.core.database.getStringOrNull
import meloplayer.core.prefs.Preference
import meloplayer.core.startup.applicationContextGlobal
import meloplayer.core.store.model.MediaStoreSong
import meloplayer.core.store.model.SongFilter
import meloplayer.core.store.model.SongSortOrder
import java.lang.RuntimeException
import java.text.Collator
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId


interface MediaStoreSongsFetcher {

    /**
     * Can result in either [ContentResolverQueryNullException] or [SecurityException]
     */
    fun getSongs(
        filters: List<SongFilter>,
        sortOrder: SongSortOrder?,
        ignoreBlackLists: Boolean = false
    ): Result<List<MediaStoreSong>>


    class ContentResolverQueryNullException : RuntimeException()


    companion object {

        val instance: MediaStoreSongsFetcher by lazy {
            MediaStoreSongsFetcherImpl(
                applicationContextGlobal,
                multipleValueSeparatorGetter = { listOf(",", "|") })
        }

        fun getImpl(
            context: Context,
            multipleValueSeparatorGetter: () -> List<String>
        ): MediaStoreSongsFetcher {
            return MediaStoreSongsFetcherImpl(context, multipleValueSeparatorGetter)
        }
    }
}


private class MediaStoreSongsFetcherImpl(
    private val context: Context,
    private val multipleValueSeparatorGetter: () -> List<String>
) : MediaStoreSongsFetcher {
    override fun getSongs(
        filters: List<SongFilter>,
        sortOrder: SongSortOrder?,
        ignoreBlackLists: Boolean
    ): Result<List<MediaStoreSong>> {
        //${MediaStore.Audio.Media.IS_MUSIC} != 0
        var selection = "  "
        val selectionValues = mutableListOf<String>()
        val defaultFilters = if (ignoreBlackLists) listOf()
        else StoreDefaults.BlacklistDirectories.map {
            SongFilter.DirectoryPathRecursive(
                it.absolutePath,
                isExclude = true
            )
        }
        (defaultFilters + filters).forEach { filter ->

            selection = if (selection.isBlank())
                " ${filter.cursorSelectionCondition} "
            else "$selection AND ${filter.cursorSelectionCondition}"
            selectionValues.add(filter.cursorSelectionConditionParamValue)
        }

        val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }


        try {
            val cursor = context.contentResolver.query(
                uri,
                BaseProjection,
                selection.ifBlank { null },
                selectionValues.toTypedArray().ifEmpty { null },
                sortOrder?.getOrderByCondition
            ) ?: return Result.failure(MediaStoreSongsFetcher.ContentResolverQueryNullException())

            val songs = generateSequence { if (cursor.moveToNext()) cursor else null }
                .map { songCursorToSong(it, multipleValueSeparatorGetter()) }
                .filterNotNull()
                .toList()

            cursor.close()
            return Result.success(songs)
        } catch (ex: SecurityException) {
            return Result.failure(ex)
        }
    }

    fun songCursorToSong(
        cursor: Cursor, multipleValueSeparator: List<String>
    ): MediaStoreSong? {
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
                LocalDate.ofInstant(Instant.ofEpochSecond(dateModified), ZoneId.systemDefault()),
                albumId,
                albumName ?: "",
                artistId,
                artistName?.split(*multipleValueSeparator.toTypedArray()) ?: listOf(),
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

private fun sortBySortOrderType(list: List<MediaStoreSong>, sortOrder: SongSortOrder) {
    val collator = Collator.getInstance()
    when (sortOrder) {
        is SongSortOrder.Album -> list.sortedWith { a, b ->
            collator.compare(
                if (sortOrder.isAscending) a.albumName else b.albumName,
                if (sortOrder.isAscending) b.albumName else a.albumName
            )
        }

        is SongSortOrder.AlbumArtist -> list.sortedWith { a, b ->
            collator.compare(
                if (sortOrder.isAscending) a.title else b.title,
                if (sortOrder.isAscending) b.title else a.title
            )
        }

        is SongSortOrder.Composer -> list.sortedWith { a, b ->
            collator.compare(
                if (sortOrder.isAscending) a.title else b.title,
                if (sortOrder.isAscending) b.title else a.title
            )
        }


        is SongSortOrder.DateModified -> {
            if (sortOrder.isAscending) list.sortedBy { it.dateModified }
            else list.sortedByDescending { it.dateModified }
        }

        is SongSortOrder.Duration -> {
            if (sortOrder.isAscending) list.sortedBy { it.duration }
            else list.sortedByDescending { it.duration }
        }

        is SongSortOrder.Name -> list.sortedWith { a, b ->
            collator.compare(
                if (sortOrder.isAscending) a.title else b.title,
                if (sortOrder.isAscending) b.title else a.title
            )
        }

        is SongSortOrder.SongArtist -> list.sortedWith { a, b ->
            collator.compare(
                if (sortOrder.isAscending) a.title else b.title,
                if (sortOrder.isAscending) b.title else a.title
            )
        }

        is SongSortOrder.Year -> {
            if (sortOrder.isAscending) list.sortedBy { it.year }
            else list.sortedByDescending { it.year }
        }
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
        is SongFilter.DirectoryPathRecursive -> "$folderPath%"
        is SongFilter.DirectoryPathExact -> folderPath
        is SongFilter.GenreExact -> genreName
        is SongFilter.SearchByTitle -> "%$titleQuery%"
        is SongFilter.GetOneById -> "$id"
        is SongFilter.AlbumIdExact -> "$albumId"
        is SongFilter.AlbumName -> "%$album%"
        is SongFilter.Artist -> "%$artist%"
        is SongFilter.ArtistsIdExact -> artistId
        is SongFilter.AlbumArtist -> "%$albumArtist%"
        is SongFilter.AlbumArtistExact -> albumArtist
    }
private val SongFilter.cursorSelectionCondition: String
    get() {
        val likeClause = if (this.isExclude) {
            " NOT LIKE "
        } else {
            " LIKE "
        }
        val lessThanClause = if (this.isExclude) {
            " >= "
        } else {
            " <= "
        }

        return when (this) {
            is SongFilter.DirectoryPathRecursive ->
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

