package meloplayer.app.playback

import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import meloplayer.core.prefs.BooleanPreference
import meloplayer.core.prefs.FloatPreference
import meloplayer.core.prefs.LongPreference
import meloplayer.core.prefs.Preference
import meloplayer.core.prefs.enumPreference
import kotlin.coroutines.CoroutineContext

class PlaybackManger(
    context: Context,
    loopMode: Preference<LoopMode> = enumPreference("playback_loop_mode", LoopMode.All),
    shuffleEnabled: Preference<Boolean> = BooleanPreference("playback_shuffle_mode", false),
    playbackSpeedPref: Preference<Float> = FloatPreference("playback_speed", 1f),
    playbackPitchPref: Preference<Float> = FloatPreference("playback_pitch", 1f),
    fadeDurationPref: Preference<Long> = LongPreference("playback_fade_duration_millis", 1000),
    onQueueEvent: (PlaybackQueueManager, QueueEvent) -> Unit = { _, _ -> },
    //onError: () -> Unit = {}
) {
    private val job = SupervisorJob()
    private val errorHandler = CoroutineExceptionHandler { coroutineContext, throwable ->
        //TODO
    }
    private val coroutineScope = object : CoroutineScope {
        override val coroutineContext: CoroutineContext
            get() = job + errorHandler + Dispatchers.Main

    }

    val queueManager =
        PlaybackQueueManager.getImpl(coroutineScope, loopMode, shuffleEnabled, onQueueEvent)
    val player = MeloPlayer.getImpl(context, playbackSpeedPref, playbackPitchPref, coroutineScope)


    fun release() {
        coroutineScope.cancel()
        player.release()
    }

    fun startPlayingWithQueueInit(initialQueue: List<Long>, initialSongId: Long? = null) {
        if (initialQueue.isEmpty()) return
        queueManager.addSongs(initialQueue)
        player.setNextUri(
            ContentUris.withAppendedId(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                initialSongId ?: initialQueue.first()
            )
        )
        if (initialSongId != null) {
            queueManager.setCurrentSongIndex(
                queueManager.currentQueue.value.indexOf(initialSongId)
            )
        }
    }

    fun playWithId(id: Long) {
        player.setNextUri(
            ContentUris.withAppendedId(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                id
            )
        )
        queueManager.setCurrentSongIndex(
            queueManager.currentQueue.value.indexOf(id)
        )
    }

    fun goToNextSong() {
        val queue = queueManager.currentQueue.value
        val nextSong = queueManager.currentSongIndex.value?.plus(1)
        if (nextSong != null && queue.getOrNull(nextSong) != null) {
            player.setNextUri(
                ContentUris.withAppendedId(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    queue[nextSong]
                )
            )
            queueManager.setCurrentSongIndex(nextSong)
        }
    }

    fun skipToPrevious() {
        if ((player.playbackPosition.value?.currentDurationMillis ?: 0) > 4000) {
            player.seekTo(0)
        } else {
            val queue = queueManager.currentQueue.value
            val prevSong = queueManager.currentSongIndex.value?.minus(1)
            if (prevSong != null && queue.getOrNull(prevSong) != null) {
                player.setNextUri(
                    ContentUris.withAppendedId(
                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                        queue[prevSong]
                    )
                )
                queueManager.setCurrentSongIndex(prevSong)
            }
        }
    }


    init {
        player.playbackPosition.mapLatest {
            delay(100)
            val playbackAroundEndOfTrack =
                if (it == null) null
                else it.currentDurationMillis + fadeDurationPref.value >= it.totalDurationMillis
            if (playbackAroundEndOfTrack == null || !playbackAroundEndOfTrack) {
                return@mapLatest
            }
            goToNextSong()
        }.launchIn(coroutineScope)
    }
}