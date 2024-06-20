package meloplayer.app.playback.session

import android.app.Service
import android.content.Intent
import android.os.IBinder

class PlaybackService : Service() {
    override fun onBind(p0: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        instance = this
        events.dispatch(ServiceEvent.START)
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        destroy(false)
    }

    enum class ServiceEvent {
        START,
        STOP,
    }

    companion object {
        val events = Eventer<ServiceEvent>()
        var instance: PlaybackService? = null

        fun destroy(stop: Boolean = true) {
            instance?.let {
                instance = null
                if (stop) {
                    it.stopForeground(STOP_FOREGROUND_REMOVE)
                    it.stopSelf()
                }
                events.dispatch(ServiceEvent.STOP)
            }
        }
    }
}
