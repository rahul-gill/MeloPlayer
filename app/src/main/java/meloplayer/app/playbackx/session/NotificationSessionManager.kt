//package meloplayer.app.playbackx.session
//
//import android.Manifest
//import android.app.Notification
//import android.content.BroadcastReceiver
//import android.content.Context
//import android.content.Intent
//import android.content.IntentFilter
//import android.content.pm.PackageManager
//import android.content.pm.ServiceInfo
//import android.os.Build
//import android.support.v4.media.session.MediaSessionCompat
//import android.support.v4.media.session.MediaSessionCompat.Callback
//import androidx.core.app.ActivityCompat
//import androidx.core.app.NotificationChannelCompat
//import androidx.core.app.NotificationManagerCompat
//import androidx.core.app.ServiceCompat
//import kotlinx.coroutines.CoroutineScope
//import kotlinx.coroutines.launch
//import meloplayer.app.R
//import meloplayer.app.playbackx.PlaybackCommand
//import meloplayer.app.playbackx.PlaybackTimeline
//import meloplayer.app.playbackx.RepeatMode
//import meloplayer.app.playbackx.service.MediaSessionCallbackX
//import meloplayer.app.playbackx.service.PlaybackServiceX
//import meloplayer.app.playbackx.service.RadioNotificationServiceEvents
//import meloplayer.app.playbackx.session.NotificationSessionUtils.CHANNEL_ID
//import meloplayer.app.playbackx.session.NotificationSessionUtils.NOTIFICATION_ID
//import meloplayer.app.prefs.PreferenceManager
//import meloplayer.core.startup.applicationContextGlobal
//import meloplayer.core.store.MediaStoreUtils
//import meloplayer.app.repo.SongsRepository
//
//
//interface NotificationSessionManager {
//    val mediaSession: MediaSessionCompat
//    fun start()
//    fun cancel()
//    fun destroy()
//    suspend fun update(
//        currentSong: Long,
//        isPlaying: Boolean,
//        position: PlaybackTimeline,
//        isShuffleOn: Boolean,
//        loopMode: RepeatMode
//    )
//
//    companion object {
//        fun getImpl(
//            context: Context,
//            coroutineScope: CoroutineScope,
//            commandHandler: suspend (PlaybackCommand) -> Unit,
//        ): NotificationSessionManager = NotificationSessionManagerImpl(
//            context, coroutineScope, commandHandler
//        )
//    }
//}
//
//
//private class NotificationSessionManagerImpl(
//    private val context: Context,
//    coroutineScope: CoroutineScope,
//    private val commandHandler: suspend (PlaybackCommand) -> Unit
//) : NotificationSessionManager {
//    enum class State {
//        PREPARING,
//        READY,
//        DESTROYED,
//    }
//
//    private val mediaSessionCallbackImpl: Callback = MediaSessionCallbackX(
//        coroutineScope = coroutineScope, commandHandler = commandHandler
//    )
//
//    private var lastNotification: Notification? = null
//        private var state = State.DESTROYED
//    private val service: PlaybackServiceX? get() = PlaybackServiceX.instance
//    private val hasService: Boolean get() = state == State.READY && service != null
//
//    override val mediaSession = MediaSessionCompat(context, MEDIA_SESSION_ID)
//    private var receiver = object : BroadcastReceiver() {
//        override fun onReceive(context: Context?, intent: Intent?) {
//            intent?.action?.let { actionName ->
//                MediaSessionAction.fromActionName(actionName)?.let { action ->
//                    coroutineScope.launch {
//                        when (action) {
//                            MediaSessionAction.ACTION_PLAY_PAUSE -> {
//                                commandHandler(PlaybackCommand.SwitchPlaying)
//                            }
//
//                            MediaSessionAction.ACTION_SHUFFLE_MODE_SWITCH -> PreferenceManager.Playback.isShuffleOn.run {
//                                setValue(!value)
//                            }
//
//                            MediaSessionAction.ACTION_PREVIOUS -> commandHandler(
//                                PlaybackCommand.SkipPrevious
//                            )
//
//                            MediaSessionAction.ACTION_NEXT -> commandHandler(
//                                PlaybackCommand.SkipPrevious
//                            )
//
//                            MediaSessionAction.ACTION_LOOP_MODE_SWITCH -> PreferenceManager.Playback.loopMode.run {
//                                setValue(value.nextInShuffle())
//                            }
//
//                            MediaSessionAction.ACTION_ON_STOP -> commandHandler(
//                                PlaybackCommand.Pause
//                            )
//                        }
//                    }
//                }
//            }
//        }
//    }
//    private val notifManager by lazy {
//        NotificationManagerCompat.from(context)
//    }
//
//    override fun start() {
//        val intentFilter = IntentFilter().apply {
//            MediaSessionAction.entries.toTypedArray().forEach {
//                addAction(it.actionName)
//            }
//        }
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
//            context.registerReceiver(receiver, intentFilter, Context.RECEIVER_EXPORTED)
//        } else {
//            context.registerReceiver(receiver, intentFilter)
//        }
//        mediaSession.setCallback(mediaSessionCallbackImpl)
//        notifManager.createNotificationChannel(
//            NotificationChannelCompat.Builder(
//                CHANNEL_ID,
//                NotificationManagerCompat.IMPORTANCE_LOW,
//            ).run {
//                setName(context.getString(R.string.app_name))
//                setLightsEnabled(false)
//                setVibrationEnabled(false)
//                setShowBadge(false)
//                build()
//            }
//        )
//        PlaybackServiceX.events.subscribe { event ->
//            when (event) {
//                RadioNotificationServiceEvents.START -> {
//                    state = State.READY
//                }
//
//                RadioNotificationServiceEvents.STOP -> destroy()
//            }
//        }
//    }
//
//
//    override fun cancel() {
//        mediaSession.isActive = false
//        notifManager.cancel(CHANNEL_ID, NOTIFICATION_ID)
//    }
//
//    override fun destroy() {
//        if (state == State.DESTROYED) return
//        state = State.DESTROYED
//        lastNotification = null
//        cancel()
//        context.unregisterReceiver(receiver)
//    }
//
//    override suspend fun update(
//        currentSong: Long,
//        isPlaying: Boolean,
//        position: PlaybackTimeline,
//        isShuffleOn: Boolean,
//        loopMode: RepeatMode
//    ) {
//        val req = buildUpdateRequest(
//            mediaSession,
//            currentSong,
//            isPlaying,
//            position,
//            isShuffleOn,
//            loopMode
//        ) ?: return
//        if(lastNotification == null)
//            lastNotification = NotificationSessionUtils.buildNotification(
//            context,
//            mediaSession,
//            req
//        )
//        if (!hasService) {
//            if (hasService || state == State.PREPARING) return
//            val intent = Intent(applicationContextGlobal, PlaybackServiceX::class.java)
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//                applicationContextGlobal.startForegroundService(intent)
//            } else {
//                applicationContextGlobal.startService(intent)
//            }
//            state = State.PREPARING
//            return
//        }
//        ServiceCompat.startForeground(
//            service!!,
//            NOTIFICATION_ID,
//            lastNotification!!,
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
//                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
//            } else {
//                0
//            }
//        )
//
//        NotificationSessionUtils.updateMediaSessionDetails(service!!, mediaSession, req)
//        runCatching {
//            if (
//                ActivityCompat.checkSelfPermission(
//                    context,
//                    Manifest.permission.POST_NOTIFICATIONS,
//                ) == PackageManager.PERMISSION_GRANTED
//            ) {
//                notifManager.notify(NOTIFICATION_ID, lastNotification!!)
//            }
//        }
//    }
//
//
//    private suspend fun buildUpdateRequest(
//        mediaSession: MediaSessionCompat,
//        songId: Long,
//        isPlaying: Boolean,
//        playbackPosition: PlaybackTimeline,
//        shuffleMode: Boolean,
//        repeatMode: RepeatMode
//    ): RadioSessionUpdateRequest? {
//        //TODO
//        return null
////        val song = SongsRepository.instance.songById(songId).getOrNull() ?: return null
////        val artworkUri = MediaStoreUtils.getArtworkUriForSong(song.id)
////        val artworkBitmap = MediaStoreUtils.getArtworkBitmap(context, songId)
////
////
////        if (songId != song.id) return null
////
////
////        //actual updates
////        if (!mediaSession.isActive) {
////            mediaSession.isActive = true
////        }
////        return RadioSessionUpdateRequest(
////            song = song,
////            artworkUri = artworkUri,
////            artworkBitmap = artworkBitmap,
////            playbackPosition = playbackPosition,
////            isPlaying = isPlaying,
////            isShuffleOn = shuffleMode,
////            loopMode = repeatMode
////        )
//
//    }
//
//    companion object {
//        val MEDIA_SESSION_ID = "${R.string.app_name}_media_session"
//    }
//}