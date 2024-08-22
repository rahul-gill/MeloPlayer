package meloplayer.app

import android.content.Intent
import android.graphics.Color
import android.os.Build
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
import meloplayer.app.playbackx.glue.PlaybackGlue
import meloplayer.app.playbackx.service.PlaybackServiceX
import meloplayer.app.prefs.PreferenceManager
import meloplayer.app.store.MediaStoreFetcherUtil.getSongsMediaStoreProperties
import meloplayer.app.store.MetadataDBPopulate
import meloplayer.app.ui.RootScreen
import meloplayer.app.ui.screen.SongListScreen
import meloplayer.core.startup.applicationContextGlobal
import meloplayer.core.ui.AppTheme
import meloplayer.core.ui.ColorSchemeType
import java.time.Instant


val db by lazy {
    MeloDatabase(
        AndroidSqliteDriver(MeloDatabase.Schema, applicationContextGlobal, "some.db"),
        albumsAdapter = Albums.Adapter(object : ColumnAdapter<Instant, Long> {
            override fun decode(databaseValue: Long): Instant {
                return Instant.ofEpochMilli(databaseValue)
            }
            override fun encode(value: Instant): Long {
                return value.toEpochMilli()
            }
        })
    )
}

class MainActivity : ComponentActivity() {

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