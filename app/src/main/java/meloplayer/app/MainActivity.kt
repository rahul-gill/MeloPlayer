package meloplayer.app

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import kotlinx.coroutines.launch
import meloplayer.app.prefs.PreferenceManager
import meloplayer.app.ui.NowPlayingPanel
import meloplayer.core.ui.AppTheme
import meloplayer.core.ui.ColorSchemeType
import meloplayer.core.ui.components.NavigationBarTabs
import meloplayer.core.ui.components.TabItem
import meloplayer.core.ui.components.sheet.PlayerSheetScaffold
import meloplayer.core.ui.components.sheet.rememberPlayerSheetState

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
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
                    val state = rememberPlayerSheetState()
                    PlayerSheetScaffold(
                        sheetState = state,
                        fullPlayerContent = {
                                            Surface(
                                                modifier = Modifier.fillMaxSize(),
                                                color = androidx.compose.ui.graphics.Color.Green
                                            ) {

                                            }
                        },
                        miniPlayerContent = {
                            Surface(modifier = Modifier.height(100.dp).fillMaxWidth(), color = androidx.compose.ui.graphics.Color.Red) {

                            }
                        },
                        navBarContent = {

                            Surface(modifier = Modifier.height(100.dp).fillMaxWidth(), color = androidx.compose.ui.graphics.Color.Blue) {

                            }
                        }) {

                    }
                }
            }
        }
    }
}