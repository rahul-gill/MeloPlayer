package meloplayer.app.playbackx.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import meloplayer.app.playbackx.EventSource
import meloplayer.app.playbackx.EventSourceImpl


class PlaybackServiceX : Service() {

    //overrides
    override fun onBind(p0: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        instance = this
        _events.dispatchEvent(RadioNotificationServiceEvents.START)
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        destroy(false)
    }

    companion object {

        private val _events = EventSourceImpl<RadioNotificationServiceEvents>()
        val events: EventSource<RadioNotificationServiceEvents> = _events
        var instance: PlaybackServiceX? = null
            private set

        fun destroy(stop: Boolean = true) {
            instance?.let {
                instance = null
                if (stop) {
                    it.stopForeground(STOP_FOREGROUND_REMOVE)
                    it.stopSelf()
                }
                _events.dispatchEvent(RadioNotificationServiceEvents.STOP)
            }
        }
    }
}

enum class RadioNotificationServiceEvents {
    START,
    STOP,
}
