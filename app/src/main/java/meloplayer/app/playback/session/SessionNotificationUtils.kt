package meloplayer.app.playback.session

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.core.app.NotificationCompat
import meloplayer.app.MainActivity
import meloplayer.app.R
import meloplayer.app.playback.LoopMode

object SessionNotificationUtils {

    fun updateMediaSessionDetails(
        mediaSession: MediaSessionCompat,
        req: RadioSessionUpdateRequest,
    ) {
        mediaSession.run {
            setMetadata(
                MediaMetadataCompat.Builder().run {
                    putString(MediaMetadataCompat.METADATA_KEY_TITLE, req.song.title)
                    if (req.song.artistNames.isNotEmpty()) {
                        putString(
                            MediaMetadataCompat.METADATA_KEY_ARTIST,
                            req.song.artistNames.joinToString(", ")
                        )
                    }
                    putString(MediaMetadataCompat.METADATA_KEY_ALBUM, req.song.albumName)
                    req.artworkUri.toString().let {
                        putString(MediaMetadataCompat.METADATA_KEY_ART_URI, it)
                        putString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI, it)
                        putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_ICON_URI, it)
                    }
                    req.artworkBitmap.let {
                        putBitmap(MediaMetadataCompat.METADATA_KEY_ART, it)
                        putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, it)
                        putBitmap(MediaMetadataCompat.METADATA_KEY_DISPLAY_ICON, it)
                    }
                    putLong(
                        MediaMetadataCompat.METADATA_KEY_DURATION,
                        req.playbackPosition.totalDurationMillis
                    )
                    build()
                }
            )
            setPlaybackState(
                PlaybackStateCompat.Builder().run {
                    setState(
                        when {
                            req.isPlaying -> PlaybackStateCompat.STATE_PLAYING
                            else -> PlaybackStateCompat.STATE_PAUSED
                        },
                        req.playbackPosition.currentDurationMillis,
                        1f
                    )
                    setActions(
                        PlaybackStateCompat.ACTION_PLAY
                                or PlaybackStateCompat.ACTION_PAUSE
                                or PlaybackStateCompat.ACTION_PLAY_PAUSE
                                or PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
                                or PlaybackStateCompat.ACTION_SKIP_TO_NEXT
                                or PlaybackStateCompat.ACTION_STOP
                                or PlaybackStateCompat.ACTION_REWIND
                                or PlaybackStateCompat.ACTION_FAST_FORWARD
                                or PlaybackStateCompat.ACTION_SEEK_TO
                                or PlaybackStateCompat.ACTION_SET_SHUFFLE_MODE
                                or PlaybackStateCompat.ACTION_SET_REPEAT_MODE
                    )
                    build()
                }
            )
        }
    }


    fun buildNotification(
        context: Context,
        mediaSession: MediaSessionCompat,
        req: RadioSessionUpdateRequest
    ): Notification {
        val notification = NotificationCompat.Builder(
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
            setContentText(req.song.artistNames.joinToString(", "))
            setLargeIcon(req.artworkBitmap)
            setOngoing(req.isPlaying)
            addAction(
                when {
                    req.isShuffleOn -> context.createAction(
                        R.drawable.shuffle_on,
                        context.getString(R.string.disable_shuffle_mode),
                        MediaSessionAction.ACTION_SHUFFLE_MODE_SWITCH.actionName
                    )

                    else -> context.createAction(
                        R.drawable.shuffle,
                        context.getString(R.string.enable_shuffle_mode),
                        MediaSessionAction.ACTION_SHUFFLE_MODE_SWITCH.actionName
                    )
                }
            )
            addAction(
                context.createAction(
                    R.drawable.skip_back,
                    context.getString(R.string.previous),
                    MediaSessionAction.ACTION_PREVIOUS.actionName
                )
            )
            addAction(
                when {
                    req.isPlaying -> context.createAction(
                        R.drawable.pause,
                        context.getString(R.string.play),
                        MediaSessionAction.ACTION_PLAY_PAUSE.actionName
                    )

                    else -> context.createAction(
                        R.drawable.play,
                        context.getString(R.string.pause),
                        MediaSessionAction.ACTION_PLAY_PAUSE.actionName
                    )
                }
            )
            addAction(
                context.createAction(
                    R.drawable.skip_fwd,
                    context.getString(R.string.next),
                    MediaSessionAction.ACTION_NEXT.actionName
                )
            )
            addAction(
                when (req.loopMode) {
                    LoopMode.None -> context.createAction(
                        R.drawable.repeat_mode,
                        context.getString(R.string.change_repeat_mode),
                        MediaSessionAction.ACTION_SHUFFLE_MODE_SWITCH.actionName
                    )

                    LoopMode.One -> context.createAction(
                        R.drawable.repeat_mode_one,
                        context.getString(R.string.change_repeat_mode),
                        MediaSessionAction.ACTION_SHUFFLE_MODE_SWITCH.actionName
                    )

                    LoopMode.All -> context.createAction(
                        R.drawable.repeat_mode_all,
                        context.getString(R.string.change_repeat_mode),
                        MediaSessionAction.ACTION_SHUFFLE_MODE_SWITCH.actionName
                    )
                }
            )

            setStyle(
                androidx.media.app.NotificationCompat.MediaStyle()
                    .setMediaSession(mediaSession.sessionToken)
                    .setShowActionsInCompactView(0, 1, 2)
            )
        }.build()
        return notification
    }


    private fun Context.createAction(
        icon: Int,
        title: String,
        action: String
    ): NotificationCompat.Action {
        val flags = PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        val pendingIntent = PendingIntent.getBroadcast(this, 0, Intent(action), flags)
        return NotificationCompat.Action.Builder(icon, title, pendingIntent).build()
    }

    val CHANNEL_ID = "${R.string.app_name}_media_notification"
    const val NOTIFICATION_ID = 69423
    val MEDIA_SESSION_ID = "${R.string.app_name}_media_session"
}

enum class MediaSessionAction(val actionName: String) {
    ACTION_PLAY_PAUSE("${R.string.app_name}_play_pause"),
    ACTION_SHUFFLE_MODE_SWITCH("${R.string.app_name}_shuffle_switch"),
    ACTION_PREVIOUS("${R.string.app_name}_previous"),
    ACTION_NEXT("${R.string.app_name}_next"),
    ACTION_LOOP_MODE_SWITCH("${R.string.app_name}_loop_switch"),
    ACTION_ON_STOP("${R.string.app_name}_stop");


    companion object {
        fun fromActionName(name: String): MediaSessionAction? {
            return entries.find { mediaSessionAction -> mediaSessionAction.actionName == name }
        }
    }
}