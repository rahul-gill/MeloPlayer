package meloplayer.app.ui.comps.nowplaying

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay


//for buttons, position seekbar, volume seekbar
enum class ControlComponentStyle {
    Material3, Flat
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NowPlayingControls(
    currentPlaybackProgress: Float,
    setPlaybackProgress: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val enabled by remember {
        mutableStateOf(true)
    }
    val colors = SliderDefaults.colors()
    Slider(
        value = currentPlaybackProgress,
        onValueChange = setPlaybackProgress,
//        interactionSource = interactionSource,
//        enabled = enabled,
//        colors = colors,
//        thumb = { state ->
//            val interactions = remember { mutableStateListOf<Interaction>() }
//            LaunchedEffect(interactionSource) {
//                interactionSource.interactions.collect { interaction ->
//                    when (interaction) {
//                        is PressInteraction.Press -> interactions.add(interaction)
//                        is PressInteraction.Release -> interactions.remove(interaction.press)
//                        is PressInteraction.Cancel -> interactions.remove(interaction.press)
//                        is DragInteraction.Start -> interactions.add(interaction)
//                        is DragInteraction.Stop -> interactions.remove(interaction.start)
//                        is DragInteraction.Cancel -> interactions.remove(interaction.start)
//                    }
//                }
//            }
//
//            val elevation = if (interactions.isNotEmpty()) {
//                6.dp
//            } else {
//                1.dp
//            }
//            val shape = RoundedCornerShape(0)
//            val thumbSize = DpSize(20.dp, 20.dp)
//            Spacer(
//                modifier
//                    .background(
//                        if (enabled) colors.thumbColor else colors.disabledThumbColor,
//                        shape
//                    )
//                    .border(width = 1.dp, color = MaterialTheme.colorScheme.onPrimary)
//                    .size(thumbSize)
//                    .indication(
//                        interactionSource = interactionSource,
//                        indication = rememberRipple(
//                            bounded = false,
//                            radius = 20.dp
//                        )
//                    )
//                    .hoverable(interactionSource = interactionSource)
//                    .shadow(if (enabled) elevation else 0.dp, shape, clip = false)
//
//            )
//        }
    )
}

@Composable
@Preview
private fun NowPlayingControlsPreview() {
    MaterialTheme(colorScheme = darkColorScheme()) {
        Surface(
            modifier = Modifier
                .fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            var progress by remember {
                mutableFloatStateOf(0f)
            }
            LaunchedEffect(key1 = Unit) {
                while (true) {
                    delay(1000)
                    progress += 0.01f
                    if (progress >= 1f) break
                }
            }
            Column(Modifier.fillMaxSize()) {
                NowPlayingControls(
                    currentPlaybackProgress = progress,
                    setPlaybackProgress = { progress = it }
                )
            }
        }
    }
}