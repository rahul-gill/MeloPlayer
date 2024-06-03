package meloplayer.app.playback

import android.app.Service
import android.content.Intent
import android.os.IBinder

class MediaPlaybackService : Service() {
    override fun onBind(p0: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        instance = this
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        destroy(false)
    }

    companion object {
        var instance: MediaPlaybackService? = null
        var onServiceStart: (() -> Unit)? = null
        var onServiceStop: (() -> Unit)? = null

        fun destroy(stop: Boolean = true) {
            instance?.let {
                instance = null
                if (stop) {
                    it.stopForeground(STOP_FOREGROUND_REMOVE)
                    it.stopSelf()
                }
            }
        }
    }
}