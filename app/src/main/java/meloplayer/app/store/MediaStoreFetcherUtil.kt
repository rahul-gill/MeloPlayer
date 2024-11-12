package meloplayer.app.store

import android.content.Context
import android.os.Build
import android.provider.BaseColumns
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import kotlin.streams.asStream

object MediaStoreFetcherUtil {
    data class MediaStoreProperties(
        val id: Long,
        val path: String,
        val dateModified: Instant
    )

    suspend fun getSongsMediaStoreProperties(context: Context): Result<List<MediaStoreProperties>> {
        return withContext(Dispatchers.IO) {
            val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
            } else {
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
            }
            try {
                val cursor = context.contentResolver.query(
                    uri,
                    BaseProjection,
                    null,
                    null,
                    null
                ) ?: return@withContext Result.failure(ContentResolverQueryNullException())
                val t1 = LocalDateTime.now()
                println("Before starting mediastore fetch $t1")
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
                    .filterNotNull()
                    .toList()
                val t2 = LocalDateTime.now()
                println("After mediastore fetch $t2")
                println("took time = ${Duration.between(t1,t2).toMillis()}ms in mediastore fetch")

                cursor.close()
                return@withContext Result.success(songs)
            } catch (ex: SecurityException) {
                return@withContext Result.failure(ex)
            }
        }
    }


    class ContentResolverQueryNullException : RuntimeException()

    private val BaseProjection = arrayOf(
        BaseColumns._ID, // 0
        MediaStore.Audio.Media.DATA, // 1
        MediaStore.Audio.AudioColumns.DATE_MODIFIED, // 2
    )
}