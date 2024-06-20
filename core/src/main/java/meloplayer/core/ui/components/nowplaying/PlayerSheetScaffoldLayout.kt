package meloplayer.core.ui.components.nowplaying

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastFirst
import kotlinx.coroutines.launch
import meloplayer.core.ui.conditional


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PlayerSheetScaffoldLayout(
    sheetState: PlayerSheetState,
    fullPlayerContent: @Composable ColumnScope.() -> Unit,
    miniPlayerContent: @Composable (shouldApplyNavPadding: Boolean) -> Unit,
    navigationSuite: @Composable () -> Unit,
    isLayoutNavBar: Boolean,
    content: @Composable () -> Unit = {}
) {
    val shouldShowMiniPlayer = remember(sheetState.sheetExpansionRatio) {
        derivedStateOf { sheetState.sheetExpansionRatio < 0.5f }
    }
    val shouldApplyBackHandler = remember(sheetState.sheetExpansionRatio) {
        derivedStateOf { sheetState.sheetExpansionRatio > 0.5f }
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

    Layout(
        modifier = Modifier.fillMaxSize(),
        content = {
            // Wrap the navigation suite and content composables each in a Box to not propagate the
            // parent's (Surface) min constraints to its children (see b/312664933).
            Box(
                modifier = Modifier
                    .layoutId(ContentLayoutIdTag)
            ) {
                content()
            }
            Box(
                modifier = Modifier
                    .layoutId(MiniPlayerContentLayoutTag)
                    .anchoredDraggable(
                        state = sheetState.draggableState,
                        orientation = Orientation.Vertical
                    )
                    .graphicsLayer {
                        this.alpha = alpha.value
                    }
            ) {
                if (shouldShowMiniPlayer.value)
                    miniPlayerContent(!isLayoutNavBar)
            }
            Column(
                Modifier
                    .layoutId(FullPlayerContentLayoutTag)
                    .anchoredDraggable(
                        state = sheetState.draggableState,
                        orientation = Orientation.Vertical
                    )
                    .background(MaterialTheme.colorScheme.surface)
                    .graphicsLayer {
                        this.alpha =
                            if (sheetState.targetValue == PlayerSheetStateType.FullPlayer) 1f
                            else (0.5f - alpha.value) * 2
                    }
                    .fillMaxWidth()
            ) {
                val scope = rememberCoroutineScope()
                BackHandler(enabled = shouldApplyBackHandler.value) {
                    scope.launch {
                        sheetState.shrinkToMiniPlayer()
                    }
                }
                fullPlayerContent()
            }
            Box(
                modifier = Modifier
                    .layoutId(NavigationSuiteLayoutIdTag)
            ) {
                navigationSuite()
            }
        },
        measurePolicy = { measurables, constraints ->
            val layoutHeight = constraints.maxHeight
            val layoutWidth = constraints.maxWidth
            val looseConstraints = constraints.copy(minWidth = 0, minHeight = 0)
            val miniPlayerPlaceable =
                measurables
                    .fastFirst { it.layoutId == MiniPlayerContentLayoutTag }
                    .measure(looseConstraints)

            val navigationPlaceable =
                measurables
                    .fastFirst { it.layoutId == NavigationSuiteLayoutIdTag }
                    .run {
                        if (isLayoutNavBar)
                            measure(looseConstraints)
                        else
                            measure(looseConstraints.copy(maxHeight = layoutHeight - miniPlayerPlaceable.height))
                    }


            val fullPlayerPlaceable =
                measurables
                    .fastFirst { it.layoutId == FullPlayerContentLayoutTag }
                    .measure(looseConstraints)


            sheetState.updateAnchors(
                layoutHeight,
                if (isLayoutNavBar) miniPlayerPlaceable.height + navigationPlaceable.height
                else miniPlayerPlaceable.height
            )

            val contentPlaceable =
                measurables
                    .fastFirst { it.layoutId == ContentLayoutIdTag }
                    .measure(
                        if (isLayoutNavBar) {
                            constraints.copy(
                                minHeight = layoutHeight - navigationPlaceable.height - miniPlayerPlaceable.height,
                                maxHeight = layoutHeight - navigationPlaceable.height - miniPlayerPlaceable.height
                            )
                        } else {
                            constraints.copy(
                                minWidth = layoutWidth - navigationPlaceable.width,
                                maxWidth = layoutWidth - navigationPlaceable.width,
                                minHeight = layoutHeight - miniPlayerPlaceable.height,
                                maxHeight = layoutHeight - miniPlayerPlaceable.height
                            )
                        }
                    )
            layout(layoutWidth, layoutHeight) {
                if (isLayoutNavBar) {

                    val navYOffset = (alpha.value * navigationPlaceable.height).toInt()
                    val playerYOffset =
                        (layoutHeight - miniPlayerPlaceable.height - navYOffset) * (1 - sheetState.sheetExpansionRatio)
                    // Place content above the navigation component.
                    contentPlaceable.placeRelative(0, 0)





                    fullPlayerPlaceable.placeRelative(
                        x = 0,
                        y = playerYOffset.toInt()
                    )
                    // Place the navigation component at the bottom of the screen
                    // While also taking care of player swipe
                    navigationPlaceable.placeRelative(
                        x = 0,
                        y = layoutHeight - navYOffset
                    )
                    //place mini player only when less than half expanded
                    if (sheetState.sheetExpansionRatio <= 0.5f) {
                        miniPlayerPlaceable.placeRelative(
                            x = 0,
                            y = playerYOffset.toInt()
                        )
                    }
                } else {


                    val playerYOffset =
                        ((layoutHeight - miniPlayerPlaceable.height) * (1 - sheetState.sheetExpansionRatio)).toInt()

                    // Place content to the side of the navigation component.
                    contentPlaceable.placeRelative(navigationPlaceable.width, 0)
                    // Place the navigation component at the start of the screen.
                    navigationPlaceable.placeRelative(0, 0)

                    // Place the player bottom of screen

                    //when full sheetExpansionRatio =  -> y = 0
                    //when mini -> y = layoutHeight - miniPlayerPlaceable.height


                    fullPlayerPlaceable.placeRelative(
                        x = 0,
                        y = playerYOffset
                    )

                    //place mini player only when less than half expanded
                    if (sheetState.sheetExpansionRatio <= 0.5f) {
                        miniPlayerPlaceable.placeRelative(
                            x = 0,
                            y = playerYOffset
                        )
                    }
                }
            }
        }
    )
}



private val NoWindowInsets = WindowInsets(0, 0, 0, 0)
private const val NavigationSuiteLayoutIdTag = "navigationSuite"
private const val ContentLayoutIdTag = "content"
private const val PlayerContentLayoutTag = "playerContent"
private const val MiniPlayerContentLayoutTag = "minuPlayerContent"
private const val FullPlayerContentLayoutTag = "fullPlayerContent"