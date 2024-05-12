package meloplayer.core.ui.components.base.sheet

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PlayerSheetScaffold(
    sheetState: PlayerSheetState,
    fullPlayerContent: @Composable ColumnScope.() -> Unit,
    miniPlayerContent: @Composable () -> Unit,
    navBarContent: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    var layoutHeight by remember { mutableIntStateOf(0) }
    var miniPlayerHeight by remember { mutableIntStateOf(0) }
    var tabBarHeight by remember { mutableIntStateOf(0) }


    /**
     * No padding applied, content composables have to handle system bars padding
     */
    Box(
        modifier = modifier
            .fillMaxSize()
            .navigationBarsPadding()
            .onSizeChanged {
                layoutHeight = it.height
                if (layoutHeight > 0 && miniPlayerHeight > 0) {
                    sheetState.updateAnchors(layoutHeight, miniPlayerHeight)
                }
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = with(LocalDensity.current) { (miniPlayerHeight ).toDp() })
        ) {
            content()
        }

        val alpha = remember(sheetState.sheetExpansionRatio) {
            derivedStateOf {
                if (sheetState.sheetExpansionRatio >= 0.5f) {
                    0f
                } else {
                    1 - sheetState.sheetExpansionRatio * 2f
                }
            }
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .align(Alignment.BottomCenter)
                .offset {
                    val yOffset = sheetState
                        .requireOffset()
                        .roundToInt()
                    IntOffset(x = 0, y = yOffset)
                }
                .anchoredDraggable(
                    state = sheetState.draggableState,
                    orientation = Orientation.Vertical
                )
                .background(MaterialTheme.colorScheme.surface)
        ) {
            AnimatedVisibility(
                visible = sheetState.targetValue == PlayerSheetStateType.FullPlayer || alpha.value < 0.5f,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            this.alpha =
                                if (sheetState.targetValue == PlayerSheetStateType.FullPlayer) 1f
                                else (0.5f - alpha.value) * 2
                        }

                ) {
                    fullPlayerContent()
                }
            }


            AnimatedVisibility(
                visible = sheetState.targetValue == PlayerSheetStateType.MiniPlayer,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                val density = LocalDensity.current
                Column(
                    modifier = Modifier
                        .onSizeChanged {
                            miniPlayerHeight = it.height
                            if (layoutHeight > 0 && miniPlayerHeight > 0) {
                                sheetState.updateAnchors(layoutHeight, miniPlayerHeight)
                            }
                        }
                        .padding(bottom = with(density) { tabBarHeight.toDp() })
                        .fillMaxWidth()
                        .graphicsLayer {
                            this.alpha = alpha.value
                        }
                ) {
                    miniPlayerContent()
                }
            }
        }

        AnimatedVisibility(
            visible = sheetState.targetValue == PlayerSheetStateType.MiniPlayer,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Column(
                modifier = Modifier
                    .onSizeChanged {
                        tabBarHeight = it.height
                    }
                    .offset {
                        val yOffset = ((1 - alpha.value) * tabBarHeight).roundToInt()
                        IntOffset(x = 0, y = yOffset)
                    }
                    .graphicsLayer {
                        this.alpha = alpha.value
                    }

            ) {
                navBarContent()
            }
        }
    }
}
