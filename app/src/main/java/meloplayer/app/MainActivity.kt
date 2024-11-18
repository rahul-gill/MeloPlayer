package meloplayer.app

import android.graphics.Color
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
//import meloplayer.app.playbackx.glue.PlaybackGlue
import meloplayer.app.prefs.PreferenceManager
import meloplayer.app.db.MeloDB
import meloplayer.app.db.SyncManager
import meloplayer.app.ui.RootScreen
import meloplayer.core.ui.AppTheme
import meloplayer.core.ui.ColorSchemeType
import org.koin.android.ext.android.inject
import java.time.Duration
import java.time.LocalDateTime

class MainActivity : ComponentActivity() {

   // val db by inject<MeloDatabase>()


    val syncM by inject<SyncManager>()
    val db by inject<MeloDB>()

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)


//        PlaybackGlue.instance.onStartImpl()


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
                Button(

                    modifier = Modifier.padding(top = 50.dp),
                    onClick = {
                    GlobalScope.launch {
                        syncM.syncDatabaseWithFileSystem()
                    }
                }) {
                    Text("Start the sync process")
                }
                Button(

                    modifier = Modifier.padding(top = 100.dp),
                    onClick = {
                        val start = LocalDateTime.now()
                        Toast.makeText(this@MainActivity, "Start", Toast.LENGTH_SHORT).show()
                        GlobalScope.launch(Dispatchers.IO) {
                            withContext(Dispatchers.Main){
                                val end  = LocalDateTime.now()
                                Toast.makeText(this@MainActivity, "End after " +
                                        "${Duration.between(start, end).toMillis()}ms", Toast.LENGTH_LONG).show()
                            }
                        }
                    }) {
                    Text("Start query")
                }
            }
        }
    }
}