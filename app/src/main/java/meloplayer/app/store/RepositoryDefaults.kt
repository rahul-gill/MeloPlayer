package meloplayer.app.store

import android.os.Build
import android.os.Environment
import android.os.Environment.getExternalStoragePublicDirectory

object RepositoryDefaults {
    val BlacklistDirectories by lazy {
        buildList {
            add(getExternalStoragePublicDirectory(Environment.DIRECTORY_ALARMS))
            add(getExternalStoragePublicDirectory(Environment.DIRECTORY_RINGTONES))
            add(getExternalStoragePublicDirectory(Environment.DIRECTORY_NOTIFICATIONS))
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                add(getExternalStoragePublicDirectory(Environment.DIRECTORY_RECORDINGS))
            }
            add(getExternalStoragePublicDirectory(Environment.DIRECTORY_PODCASTS))
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                add(getExternalStoragePublicDirectory(Environment.DIRECTORY_AUDIOBOOKS))
            }
        }
    }
}