package meloplayer.core.ui.components.base.sheet

import androidx.compose.animation.core.AnimationConstants
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
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
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp

enum class PlayerSheetStateType {
    MiniPlayer,
    FullPlayer
}


@Composable
fun rememberPlayerSheetState(): PlayerSheetState {
    val density = LocalDensity.current
    return rememberSaveable(
        saver = PlayerSheetState.Saver(
            density = density
        ),
        init = { PlayerSheetState(PlayerSheetStateType.MiniPlayer, density) }
    )
}


@OptIn(ExperimentalFoundationApi::class)
@Stable
class PlayerSheetState(
    initialValue: PlayerSheetStateType,
    density: Density,
) {

    val draggableState = AnchoredDraggableState(
        initialValue = initialValue,
        animationSpec = tween(easing = LinearEasing, durationMillis = AnimationConstants.DefaultDurationMillis),
        positionalThreshold = { distance: Float -> distance * 0.5f },
        velocityThreshold = { with(density) { 125.dp.toPx() } },
        confirmValueChange = { true }
    )

    ///////////////////////PUBLIC
    val currentValue: PlayerSheetStateType
        get() = draggableState.currentValue
    val targetValue: PlayerSheetStateType
        get() = draggableState.targetValue

    val sheetExpansionRatio: Float
        get() {
            val miniPlayerPos = draggableState.anchors.positionOf(PlayerSheetStateType.MiniPlayer).run {
                if(isNaN()) 0f else this
            }
            val fullPlayerPos = draggableState.anchors.positionOf(PlayerSheetStateType.FullPlayer).run {
                if(isNaN()) 0f else this
            }
            val currentPos = requireOffset()

            return if(fullPlayerPos - miniPlayerPos == 0f) 0f else (currentPos - miniPlayerPos) / (fullPlayerPos - miniPlayerPos)
        }

    suspend fun expandToFullPlayer() {
        draggableState.animateTo(PlayerSheetStateType.FullPlayer)//TODO, draggableState.lastVelocity)
    }

    suspend fun shrinkToMiniPlayer() {
        draggableState.animateTo(PlayerSheetStateType.MiniPlayer)//TODO, draggableState.lastVelocity)
    }

    fun requireOffset() = if (draggableState.offset.isNaN()) 0f else draggableState.offset

    /**
     * @param maxPossibleFullPlayerHeightPx full player will take the maximum amount of height
     * possible, this is that height
     * @param miniPlayerHeightPx miniPlayer will only take limited height, this is that value
     */
    fun updateAnchors(maxPossibleFullPlayerHeightPx: Int, miniPlayerHeightPx: Int) {
        val newAnchors = DraggableAnchors {
            PlayerSheetStateType.MiniPlayer at maxPossibleFullPlayerHeightPx - miniPlayerHeightPx.toFloat()
            PlayerSheetStateType.FullPlayer at 0f
        }
        draggableState.updateAnchors(newAnchors)
    }
    ////////////////////////PUBLIC END


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