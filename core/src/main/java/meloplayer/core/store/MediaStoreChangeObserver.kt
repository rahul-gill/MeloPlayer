package meloplayer.core.store

import android.content.Context
import android.database.ContentObserver
import android.provider.MediaStore
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus


class MediaStoreChangeObserver(
    private val context: Context,
    onMediaStoreChanged: () -> Unit
) {
    var job: Job? = null
    val scope = CoroutineScope(CoroutineName("MediaStoreObserver"))
    private val observer = object : ContentObserver(null) {
        override fun onChange(selfChange: Boolean) {
            job?.cancel()
            job = Job()
            job?.run {
                (scope + this).launch(Dispatchers.Main) {
                    delay(MEDIA_STORE_OBSERVER_DELAY_MILLIS)
                    onMediaStoreChanged()
                }
            }
        }
    }
    fun register(){
        context. contentResolver.registerContentObserver(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            true,
            observer
        )
        context.contentResolver.registerContentObserver(
            MediaStore.Audio.Media.INTERNAL_CONTENT_URI,
            true,
            observer
        )
    }

    fun unregister(){
        context.contentResolver.unregisterContentObserver(observer)
        job?.cancel()
        job = null
    }

    companion object {
        private const val MEDIA_STORE_OBSERVER_DELAY_MILLIS = 1000L
    }
}