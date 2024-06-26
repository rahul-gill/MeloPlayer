package meloplayer.app.playback.session

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.MediaSessionCompat.Callback
import meloplayer.app.R


interface PlaybackSessionManager {
    val mediaSession: MediaSessionCompat
    fun start()
    fun cancel()
    fun destroy()
    fun update(
        req: RadioSessionUpdateRequest
    )

    companion object {
        fun getImpl(
            context: Context,
            mediaSessionCallback: Callback,
            handleAction: (MediaSessionAction) -> Unit
        ): PlaybackSessionManager = PlaybackSessionManagerImpl(
            context, mediaSessionCallback, handleAction
        )
    }
}


private class PlaybackSessionManagerImpl(
    private val context: Context,
    private val mediaSessionCallbackImpl: Callback,
    private val handleAction: (MediaSessionAction) -> Unit
) : PlaybackSessionManager {
    override val mediaSession = MediaSessionCompat(context, MEDIA_SESSION_ID)
    private var receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            intent?.action?.let { action ->
                MediaSessionAction.fromActionName(action)?.let(handleAction)
            }
        }
    }

    override fun start() {
        val intentFilter = IntentFilter().apply {
            MediaSessionAction.entries.toTypedArray().forEach {
                addAction(it.actionName)
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(receiver, intentFilter, Context.RECEIVER_EXPORTED)
        } else {
            context.registerReceiver(receiver, intentFilter)
        }
        mediaSession.setCallback(mediaSessionCallbackImpl)
    }


    override fun cancel() {
        mediaSession.isActive = false
    }

    override fun destroy() {
        cancel()
        context.unregisterReceiver(receiver)
    }

    override fun update(
        req: RadioSessionUpdateRequest
    ) {
        SessionNotificationUtils.updateMediaSessionDetails(mediaSession, req)

    }


    companion object {
        val MEDIA_SESSION_ID = "${R.string.app_name}_media_session"
    }
}