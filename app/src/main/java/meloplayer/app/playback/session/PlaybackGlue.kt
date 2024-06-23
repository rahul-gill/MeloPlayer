package meloplayer.app.playback.session

import android.app.Notification
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.support.v4.media.session.MediaSessionCompat
import android.util.Log
import androidx.core.app.ServiceCompat
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import meloplayer.app.playback.PlaybackPosition
import meloplayer.app.playback.session.SessionNotificationUtils.NOTIFICATION_ID
import meloplayer.app.prefs.PreferenceManager
import meloplayer.app.ui.playbackManager
import meloplayer.core.startup.applicationContextGlobal
import meloplayer.core.store.MediaStoreUtils
import meloplayer.core.store.repo.SongsRepository
import kotlin.coroutines.CoroutineContext

class PlaybackGlue(private val context: Context = applicationContextGlobal) {
    enum class State {
        PREPARING,
        READY,
        DESTROYED,
    }

    private var lastNotification: Notification? = null
    private var state = State.DESTROYED
    private val service: PlaybackService? get() = PlaybackService.instance
    private val hasService: Boolean get() = state == State.READY && service != null

    private val job = SupervisorJob()
    private val errorHandler = CoroutineExceptionHandler { _, throwable ->
        Log.e("PlaybackService", throwable.toString())
    }
    private val coroutineScope = object : CoroutineScope {
        override val coroutineContext: CoroutineContext
            get() = job + errorHandler + Dispatchers.Main

    }
    private val sessionManager by lazy {
        PlaybackSessionManager.getImpl(
            context = context,
            scope = coroutineScope,
            mediaSessionCallback = MediaSessionCallbackImpl(playbackManger = playbackManager),
            handleAction = {
                when (it) {
                    MediaSessionAction.ACTION_PLAY_PAUSE -> playbackManager.player.switchIsPlaying()
                    MediaSessionAction.ACTION_SHUFFLE_MODE_SWITCH -> PreferenceManager.isShuffleOn.run {
                        setValue(!value)
                    }

                    MediaSessionAction.ACTION_PREVIOUS -> playbackManager.skipToPrevious()
                    MediaSessionAction.ACTION_NEXT -> playbackManager.goToNextSong()
                    MediaSessionAction.ACTION_LOOP_MODE_SWITCH -> PreferenceManager.loopMode.run {
                        setValue(value.toggleNextValue())
                    }

                    MediaSessionAction.ACTION_ON_STOP -> if (playbackManager.player.isPlaying.value) {
                        playbackManager.player.switchIsPlaying()
                    }
                }
            }
        )
    }

    private val notificationManager by lazy {
        RadioNotification.getImpl(context)
    }

    fun onStartImpl() {
        println("onStartImpl")
        playbackManager.setScope(coroutineScope)
        sessionManager.start()
        notificationManager.start()
        PlaybackService.events.subscribe { event ->
            when (event) {
                RadioNotificationServiceEvents.START -> {
                    state = State.READY
                    lastNotification?.let { notification ->
                        lastNotification = null
                        ServiceCompat.startForeground(
                            service!!,
                            NOTIFICATION_ID,
                            notification,
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
                            } else {
                                0
                            }
                        )
                    }
                }

                RadioNotificationServiceEvents.STOP -> TODO()
            }
        }


        val isPlaying = playbackManager.player.isPlaying
        val shuffleMode = PreferenceManager.isShuffleOn.observableValue
        val loopMode = PreferenceManager.loopMode.observableValue
        val position = playbackManager.player.playbackPosition
        val currentSong = playbackManager.queueManager.currentItem
        println("before somevalue")
        combine(
            isPlaying,
            shuffleMode,
            loopMode,
            position,
            currentSong
        ) { isPlayingNow, _, _, positionNow, currentSongNow ->
            println("Some-values $isPlayingNow $currentSongNow $positionNow")
            if (currentSongNow != null && positionNow != null) {
                val req = buildUpdateRequest(
                    sessionManager.mediaSession,
                    currentSongNow,
                    isPlayingNow,
                    positionNow
                ) ?: return@combine
                lastNotification = SessionNotificationUtils.buildNotification(
                    context,
                    sessionManager.mediaSession,
                    req
                )
                if (!hasService) {
                    if (hasService || state == State.PREPARING) return@combine
                    val intent = Intent(applicationContextGlobal, PlaybackService::class.java)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        applicationContextGlobal.startForegroundService(intent)
                    } else {
                        applicationContextGlobal.startService(intent)
                    }
                    state = State.PREPARING
                    return@combine
                }
                ServiceCompat.startForeground(
                    service!!,
                    NOTIFICATION_ID,
                    lastNotification!!,
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
                    } else {
                        0
                    }
                )
                sessionManager.update(req)
                notificationManager.update(lastNotification!!)
            } else {
                notificationManager.stop()
                sessionManager.cancel()
            }
        }.launchIn(scope = coroutineScope)
    }

    private fun onStopImpl() {
        destroyNotification()
        PlaybackService.destroy()
    }


    private fun destroyNotification() {
        if (state == State.DESTROYED) return
        state = State.DESTROYED
        lastNotification = null
        notificationManager.stop()
        sessionManager.destroy()
    }

    private suspend fun buildUpdateRequest(
        mediaSession: MediaSessionCompat,
        songId: Long,
        isPlaying: Boolean,
        playbackPosition: PlaybackPosition
    ): RadioSessionUpdateRequest? {
        println("buildUpdateRequest start")
        val song = SongsRepository.instance.songById(songId).getOrNull() ?: return null
        println("buildUpdateRequest song:$song")
        val artworkUri = MediaStoreUtils.getArtworkUriForSong(song.id)
        val artworkBitmap = MediaStoreUtils.getArtworkBitmap(context, songId)


        if (songId != song.id) return null

        println("actual buildUpdateRequest")

        //actual updates
        if (!mediaSession.isActive) {
            mediaSession.isActive = true
        }
        return RadioSessionUpdateRequest(
            song = song,
            artworkUri = artworkUri,
            artworkBitmap = artworkBitmap,
            playbackPosition = playbackPosition,
            isPlaying = isPlaying,
            isShuffleOn = PreferenceManager.isShuffleOn.value,
            loopMode = PreferenceManager.loopMode.value
        )

    }

    companion object {
        val instance by lazy {
            PlaybackGlue()
        }
    }
}
