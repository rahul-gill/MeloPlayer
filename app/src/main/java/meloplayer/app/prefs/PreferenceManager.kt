package meloplayer.app.prefs

import androidx.compose.ui.graphics.Color
import meloplayer.app.playbackx.RepeatMode
import meloplayer.app.playbackx.SongTransitionType
import meloplayer.core.prefs.BooleanPreference
import meloplayer.core.prefs.FloatPreference
import meloplayer.core.prefs.IntPreference
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

    object Playback {
        val speed = FloatPreference(key = "playback_speed", defaultValue = 1f)
        val pitch = FloatPreference(key = "playback_pitch", defaultValue = 1f)
        val isShuffleOn = BooleanPreference(key = "is_shuffle_on", defaultValue = true)
        val loopMode = enumPreference(key = "repeat_mode", defaultValue = RepeatMode.All)
        val pauseFadeOutDurationMillis = LongPreference(key = "", defaultValue = 100)
        val playFadeInDurationMillis = LongPreference(key = "", defaultValue = 100)

        val crossFadeOutDurationMillis = LongPreference(key = "", defaultValue = 100)
        val crossFadeInDurationMillis = LongPreference(key = "", defaultValue = 100)
        val songTransitionType = customPreference(
            backingPref = IntPreference(key = "", defaultValue = 1),
            defaultValue = SongTransitionType.CrossFade(
                crossFadeInDurationMillis.value,
                crossFadeOutDurationMillis.value
            ),
            deserialize = { intVal ->
                if (intVal == 0) SongTransitionType.Simple
                else SongTransitionType.CrossFade(
                    crossFadeInDurationMillis.value,
                    crossFadeOutDurationMillis.value
                )
            },
            serialize = { type ->
                when (type) {
                    SongTransitionType.Simple -> 0
                    else -> 1
                }
            }
        )
        val shouldPauseOnZeroVolume = BooleanPreference(key = "", defaultValue = false)
        val shouldResumeOnExternalDeviceConnect = BooleanPreference(key = "", defaultValue = false)
        val shouldGoToPreviousSongOnExternalDeviceAction =
            BooleanPreference(key = "", defaultValue = false)
        val durationToSkipPreviousSongMillis = LongPreference(key = "", defaultValue = 4000)

        val rewindBackDuration = LongPreference(key = "rewind_back_duration", defaultValue = 5000L)
        val forwardDuration = LongPreference(key = "forward_duration", defaultValue = 5000L)
    }


    //Now playing panel related
}


enum class NowPlayingOverallStyle {
    Normal, ControlCard, Peek, HalfExpandedQueue
}

enum class NowPlayingBackgroundStyle {
    SolidBackgroundColor, SolidColorFromAlbumArt, AlbumArtBlur, GradientColorFromAlbumArt
}




