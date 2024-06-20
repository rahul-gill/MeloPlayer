package meloplayer.core.ui.components.nowplaying

import androidx.compose.animation.core.EaseOutExpo
import androidx.compose.animation.core.exponentialDecay
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.animateTo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.integerResource
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp

enum class PlayerSheetStateType {
    MiniPlayer,
    FullPlayer
}


@Composable
fun rememberPlayerSheetState(): PlayerSheetState {
    val density = LocalDensity.current
    val animTimeMillis = integerResource(id = android.R.integer.config_mediumAnimTime)
    return rememberSaveable(
        saver = PlayerSheetState.Saver(
            density = density
        ),
        init = {
            PlayerSheetState(
                PlayerSheetStateType.MiniPlayer,
                density,
                animTimeMillis
            )
        }
    )
}


@OptIn(ExperimentalFoundationApi::class)
@Stable
class PlayerSheetState(
    initialValue: PlayerSheetStateType = PlayerSheetStateType.MiniPlayer,
    density: Density,
    animationDurationMillis: Int = 300
) {
    val draggableState = AnchoredDraggableState(
        initialValue = initialValue,
        snapAnimationSpec = tween(easing = EaseOutExpo, durationMillis = animationDurationMillis),
        decayAnimationSpec = exponentialDecay(),
        positionalThreshold = { distance: Float -> distance * 0.5f },
        velocityThreshold = { with(density) { 125.dp.toPx() } },
        confirmValueChange = { true }
    )

    val currentValue: PlayerSheetStateType
        get() = draggableState.currentValue
    val targetValue: PlayerSheetStateType
        get() = draggableState.targetValue
    val currentOffset
        get() = if (draggableState.offset.isNaN()) 0f else draggableState.offset

    suspend fun expandToFullPlayer() {
        draggableState.animateTo(PlayerSheetStateType.FullPlayer)
    }

    suspend fun shrinkToMiniPlayer() {
        draggableState.animateTo(PlayerSheetStateType.MiniPlayer)
    }

    val sheetExpansionRatio: Float
        get() {
            val miniPlayerPos =
                draggableState.anchors.positionOf(PlayerSheetStateType.MiniPlayer).run {
                    if (isNaN()) 0f else this
                }
            val fullPlayerPos =
                draggableState.anchors.positionOf(PlayerSheetStateType.FullPlayer).run {
                    if (isNaN()) 0f else this
                }

            return if (fullPlayerPos - miniPlayerPos == 0f) 0f
            else (currentOffset - miniPlayerPos) / (fullPlayerPos - miniPlayerPos)
        }


    fun updateAnchors(layoutHeight: Int, bottomContentHeight: Int) {
        val newAnchors = DraggableAnchors {
            PlayerSheetStateType.MiniPlayer at layoutHeight - bottomContentHeight.toFloat()
            PlayerSheetStateType.FullPlayer at 0f
        }
        draggableState.updateAnchors(newAnchors)
    }


    companion object {
        fun Saver(
            density: Density
        ): Saver<PlayerSheetState, PlayerSheetStateType> = Saver(
            save = { it.currentValue },
            restore = {
                PlayerSheetState(
                    initialValue = it,
                    density = density
                )
            }
        )
    }
}