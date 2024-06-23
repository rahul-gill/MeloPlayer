package meloplayer.core.store

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.provider.MediaStore
import androidx.annotation.DrawableRes
import androidx.core.graphics.drawable.toBitmap
import coil.imageLoader
import coil.request.ImageRequest
import wow.app.core.R

object MediaStoreUtils {

    fun getArtworkUriForSong(songId: Long): Uri =
        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI.buildUpon()
            .appendPath(songId.toString())
            .appendPath("albumart").build()

    suspend fun getArtworkBitmap(
        context: Context,
        songId: Long,
        @DrawableRes fallbackResId: Int? = R.drawable.placeholder_music,
    ): Bitmap {
        val imageRequest = ImageRequest.Builder(context).apply {
            data(getArtworkUriForSong(songId))
            fallbackResId?.let {
                placeholder(it)
                fallback(it)
                error(it)
            }
            crossfade(true)
        }
        val result = context.imageLoader.execute(imageRequest.build())
        return result.drawable?.toBitmap() ?: getPlaceholderBitmap(context)
    }


    private fun getPlaceholderBitmap(context: Context): Bitmap =
        context.getDrawable(R.drawable.placeholder_music)?.toBitmap()!!

    @DrawableRes
    private fun getPlaceholderId(): Int = R.drawable.placeholder_music
}