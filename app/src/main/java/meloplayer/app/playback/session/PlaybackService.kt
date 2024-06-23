package meloplayer.app.playback.session

import android.app.Notification
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.support.v4.media.session.MediaSessionCompat
import android.util.Log
import androidx.core.app.ServiceCompat
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import meloplayer.app.playback.PlaybackManger
import meloplayer.app.playback.PlaybackPosition
import meloplayer.app.playback.session.SessionNotificationUtils.NOTIFICATION_ID
import meloplayer.app.prefs.PreferenceManager
import meloplayer.app.ui.playbackManager
import meloplayer.core.startup.applicationContextGlobal
import meloplayer.core.store.MediaStoreUtils
import meloplayer.core.store.repo.SongsRepository
import kotlin.coroutines.CoroutineContext



class PlaybackService : Service() {

    //overrides
    override fun onBind(p0: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        instance = this
        println("onStartCommand")
        events.dispatch(RadioNotificationServiceEvents.START)
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        destroy(false)
    }

    companion object {

        val events = Eventer<RadioNotificationServiceEvents>()
        var instance: PlaybackService? = null
            private set

        fun destroy(stop: Boolean = true) {
            instance?.let {
                instance = null
                if (stop) {
                    it.stopForeground(STOP_FOREGROUND_REMOVE)
                    it.stopSelf()
                }
                events.dispatch(RadioNotificationServiceEvents.STOP)
            }
        }
    }
}

enum class RadioNotificationServiceEvents {
    START,
    STOP,
}
