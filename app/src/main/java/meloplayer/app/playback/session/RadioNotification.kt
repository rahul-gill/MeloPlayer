package meloplayer.app.playback.session

import android.Manifest
import android.app.Notification
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.support.v4.media.session.MediaSessionCompat
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import meloplayer.app.R
import meloplayer.app.playback.session.SessionNotificationUtils.CHANNEL_ID
import meloplayer.app.playback.session.SessionNotificationUtils.NOTIFICATION_ID


interface RadioNotification {
    fun start()
    fun stop()
    fun update(notification: Notification)

    companion object {
        fun getImpl(
            context: Context
        ): RadioNotification = RadioNotificationImpl(context)
    }
}

private class RadioNotificationImpl(
    private val context: Context
) : RadioNotification {

    private val manager by lazy {
        NotificationManagerCompat.from(context)
    }


    override fun start() {
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
    }

    override fun stop() {
        manager.cancel(CHANNEL_ID, NOTIFICATION_ID)
    }


    override fun update(notification: Notification) {
        println("notification-update-x")
        runCatching {
            if (
                ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS,
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                manager.notify(NOTIFICATION_ID, notification)
            }
        }
    }

}