package meloplayer.app.playback.session

import android.Manifest
import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.media3.session.MediaStyleNotificationHelper.MediaStyle
import meloplayer.app.MainActivity
import meloplayer.app.R
import meloplayer.app.playback.PlaybackPosition
import meloplayer.core.store.model.MediaStoreSong

class RadioNotification(private val context: Context) {
    enum class State {
        PREPARING,
        READY,
        DESTROYED,
    }

    private val manager by lazy {
        NotificationManagerCompat.from(context)
    }
    private var lastNotification: Notification? = null


    private var state = State.DESTROYED
    private val service: PlaybackService? get() = PlaybackService.instance
    private val hasService: Boolean
        get() = state == State.READY && service != null

    fun start() {
        manager.createNotificationChannel(
            NotificationChannelCompat.Builder(
                CHANNEL_ID,
                NotificationManagerCompat.IMPORTANCE_LOW,
            ).run {
                setName(context.getString(R.string.app_name))
                setLightsEnabled(false)
                setVibrationEnabled(false)
                setShowBadge(false)
                build()
            }
        )
        PlaybackService.events.subscribe {
//            when (it) {
//                PlaybackService.ServiceEvent.START -> onServiceStart()
//                PlaybackService.ServiceEvent.STOP -> onServiceStop()
//            }
        }
    }

    fun cancel() {
        //destroyNotification()
        PlaybackService.destroy()
    }

    fun update(req: RadioSessionUpdateRequest) {
        NotificationCompat.Builder(
            context,
            CHANNEL_ID
        ).run {
            setSmallIcon(R.drawable.app_icon)
            setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            setContentIntent(
                PendingIntent.getActivity(
                    context,
                    0,
                    Intent(context, MainActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT),
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
                )
            )
            setContentTitle(req.song.title)
            setContentText(req.song.artistNames.joinToString { "" })
            setLargeIcon(req.artworkBitmap)
            setOngoing(req.isPlaying)
            addAction(
                createAction(
                    R.drawable.skip_back,
                    context.getString(R.string.previous),
                    ACTION_PREVIOUS
                )
            )
            addAction(
                when {
                    req.isPlaying -> createAction(
                        R.drawable.pause,
                        context.getString(R.string.play),
                        ACTION_PLAY_PAUSE
                    )

                    else -> createAction(
                        R.drawable.play,
                        context.getString(R.string.pause),
                        ACTION_PLAY_PAUSE
                    )
                }
            )
            addAction(
                createAction(
                    R.drawable.skip_fwd,
                    context.getString(R.string.next),
                    ACTION_NEXT
                )
            )
            addAction(
                createAction(
                    R.drawable.stop,
                    context.getString(R.string.stop),
                    ACTION_STOP
                )
            )
//            setStyle(
//                NotificationCompat.MediaStyle()
//                    .setMediaSession(sessionToken)
//                    .setShowActionsInCompactView(0, 1, 2)
//            )
            kotlin.runCatching {
                notifyNotification(build())
            }
        }
    }


    private fun notifyNotification(notification: Notification) {
        if (!hasService && state != State.PREPARING) {
            val intent = Intent(context, PlaybackService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
            state = State.PREPARING
            lastNotification = notification
            return
        }
        if (
            ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            manager.notify(NOTIFICATION_ID, notification)
        }

    }

    private fun createAction(icon: Int, title: String, action: String) =
        NotificationCompat.Action.Builder(
            icon,
            title,
            PendingIntent.getBroadcast(
                context,
                0,
                Intent(action),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
        ).build()

    companion object {
        private val CHANNEL_ID = "${R.string.app_name}_media_notification"
        private const val NOTIFICATION_ID = 69423
        private val MEDIA_SESSION_ID = "${R.string.app_name}_media_session"
        private val ACTION_PLAY_PAUSE = "${R.string.app_name}_play_pause"
        private val ACTION_PREVIOUS = "${R.string.app_name}_previous"
        private val ACTION_NEXT = "${R.string.app_name}_next"
        private val ACTION_STOP = "${R.string.app_name}_stop"
    }
}

data class RadioSessionUpdateRequest(
    val sessionToken: String,
    val song: MediaStoreSong,
    val artworkUri: Uri,
    val artworkUriString: String,
    val artworkBitmap: Bitmap,
    val playbackPosition: PlaybackPosition,
    val isPlaying: Boolean,
)