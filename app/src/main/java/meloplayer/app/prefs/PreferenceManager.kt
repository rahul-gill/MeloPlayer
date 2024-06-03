package meloplayer.app.prefs

import androidx.compose.ui.graphics.Color
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
    //Now playing panel related

}

enum class NowPlayingOverallStyle {
    Normal, ControlCard, Peek, HalfExpandedQueue
}
enum class NowPlayingBackgroundStyle {
    SolidBackgroundColor, SolidColorFromAlbumArt, AlbumArtBlur, GradientColorFromAlbumArt
}




