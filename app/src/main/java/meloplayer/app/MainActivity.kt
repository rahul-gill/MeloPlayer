package meloplayer.app

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import app.cash.sqldelight.ColumnAdapter
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import meloplayer.app.db.Albums
import meloplayer.app.db.MeloDatabase
import meloplayer.app.db.Songs
import meloplayer.app.playbackx.glue.PlaybackGlue
import meloplayer.app.prefs.PreferenceManager
import meloplayer.app.store.MediaStoreFetcherUtil.getSongsMediaStoreProperties
import meloplayer.app.store.MetadataDBPopulate
import meloplayer.app.ui.RootScreen
import meloplayer.core.startup.applicationContextGlobal
import meloplayer.core.ui.AppTheme
import meloplayer.core.ui.ColorSchemeType
import org.koin.android.ext.android.inject
import org.koin.compose.koinInject
import java.time.Instant

class MainActivity : ComponentActivity() {

    val db by inject<MeloDatabase>()

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        GlobalScope.launch {
            val props = getSongsMediaStoreProperties(this@MainActivity).getOrNull()
            if (props == null) {
                println("Can't get props")
            } else
                MetadataDBPopulate.refreshMetadataDatabase(this@MainActivity, db, props)
        }

        PlaybackGlue.instance.onStartImpl()


        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(
                Color.TRANSPARENT, Color.TRANSPARENT
            ),
            navigationBarStyle = SystemBarStyle.light(
                Color.TRANSPARENT, Color.TRANSPARENT
            )
        )

        setContent {
            val followSystemColor = PreferenceManager.followSystemColors.asState()
            val seedColor = PreferenceManager.colorSchemeSeed.asState()
            val theme = PreferenceManager.themeConfig.asState()
            val darkThemeType = PreferenceManager.darkThemeType.asState()
            AppTheme(
                colorSchemeType = if (followSystemColor.value) {
                    ColorSchemeType.Dynamic
                } else {
                    ColorSchemeType.WithSeed(seedColor.value)
                },
                themeConfig = theme.value,
                darkThemeType = darkThemeType.value
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    RootScreen()
                }
            }
        }
    }
}