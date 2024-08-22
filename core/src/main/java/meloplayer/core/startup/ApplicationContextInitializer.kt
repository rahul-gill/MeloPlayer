package meloplayer.core.startup

import android.content.Context
import android.util.Log
import androidx.startup.Initializer
import com.thelumierguy.crashwatcher.CrashWatcher

private var appContext: Context? = null

val applicationContextGlobal
    get() = appContext!!


internal class ApplicationContextInitializer : Initializer<Context> {
    override fun create(context: Context): Context {
        context.applicationContext.also { appContext = it }
        Log.i("ApplicationContextInitializer", "init done")
        CrashWatcher.initCrashWatcher(context)
        Log.d("CrashWatcherInitializer", "Timber initialized")
        return context.applicationContext
    }

    override fun dependencies(): List<Class<out Initializer<*>>> = emptyList()
}


