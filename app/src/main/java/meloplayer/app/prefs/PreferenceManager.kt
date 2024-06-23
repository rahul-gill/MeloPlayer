package meloplayer.app.prefs

import androidx.compose.ui.graphics.Color
import meloplayer.app.playback.LoopMode
import meloplayer.core.prefs.BooleanPreference
import meloplayer.core.prefs.LongPreference
import meloplayer.core.prefs.customPreference
import meloplayer.core.prefs.enumPreference
import meloplayer.core.ui.DarkThemeType
import meloplayer.core.ui.DefaultThemeSeed
import meloplayer.core.ui.ThemeConfig


object PreferenceManager {
    //Theme related
    val themeConfig = enumPreference(
        key = "theme_config",
        defaultValue = ThemeConfig.FollowSystem
    )
    val darkThemeType = enumPreference(
        key = "dark_theme_type",
        defaultValue = DarkThemeType.Dark
    )
    val followSystemColors = BooleanPreference(key = "follow_system_colors", defaultValue = true)
    val colorSchemeSeed = customPreference(
        backingPref = LongPreference("color_scheme_type", 0),
        defaultValue = DefaultThemeSeed,
        serialize = { color -> color.value.toLong() },
        deserialize = { if (it == 0L) DefaultThemeSeed else Color(it.toULong()) }
    )


    //Playback related
    val isShuffleOn = BooleanPreference(key = "is_shuffle_on", defaultValue = true)
    val loopMode = enumPreference(key = "loop_mode", defaultValue = LoopMode.All)
    val rewindBackDuration = LongPreference(key = "rewind_back_duration", defaultValue = 5000L)
    val forwardDuration = LongPreference(key = "forward_duration", defaultValue = 5000L)



    //Now playing panel related
}


enum class NowPlayingOverallStyle {
    Normal, ControlCard, Peek, HalfExpandedQueue
}

enum class NowPlayingBackgroundStyle {
    SolidBackgroundColor, SolidColorFromAlbumArt, AlbumArtBlur, GradientColorFromAlbumArt
}




