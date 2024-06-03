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
import androidx.compose.foundation.layout.WindowInsets
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
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.util.fastFirst
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
                .padding(bottom = with(LocalDensity.current) { (miniPlayerHeight).toDp() })
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

//        AnimatedVisibility(
//            visible = sheetState.targetValue == PlayerSheetStateType.MiniPlayer,
//            modifier = Modifier.align(Alignment.BottomCenter),
//            enter = fadeIn(),
//            exit = fadeOut()
//        ) {
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)//new
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
//        }
    }
}


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SuitPlayerContent(
    sheetState: PlayerSheetState,
    fullPlayerContent: @Composable ColumnScope.() -> Unit,
    miniPlayerContent: @Composable () -> Unit,
) {
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
            //TODO ok to remove?
//            .offset {
//                val yOffset = sheetState
//                    .requireOffset()
//                    .roundToInt()
//                IntOffset(x = 0, y = yOffset)
//            }
            .anchoredDraggable(
                state = sheetState.draggableState,
                orientation = Orientation.Vertical
            )
            .background(MaterialTheme.colorScheme.surface)
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
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer {
                    this.alpha = alpha.value
                }
        ) {
            miniPlayerContent()
        }
    }
}

fun Modifier.conditional(condition: Boolean, modifier: Modifier.() -> Modifier): Modifier {
    return if (condition) {
        then(modifier(Modifier))
    } else {
        this
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NavigationSuiteScaffoldLayoutX(
    sheetState: PlayerSheetState,
    fullPlayerContent: @Composable ColumnScope.() -> Unit,
    miniPlayerContent: @Composable () -> Unit,
    navigationSuite: @Composable () -> Unit,
    layoutType: NavigationSuiteType =
        NavigationSuiteScaffoldDefaults.calculateFromAdaptiveInfo(WindowAdaptiveInfoDefault),
    content: @Composable () -> Unit = {}
) {
    val shouldShowFullPlayer = remember(sheetState.sheetExpansionRatio) {
        derivedStateOf { sheetState.sheetExpansionRatio > 0.1f }
    }
    val shouldShowMiniPlayer = remember(sheetState.sheetExpansionRatio) {
        derivedStateOf { sheetState.sheetExpansionRatio < 0.5f }
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
    println("sheetState.sheetExpansionRatio:${sheetState.sheetExpansionRatio}")
    Layout({
        // Wrap the navigation suite and content composables each in a Box to not propagate the
        // parent's (Surface) min constraints to its children (see b/312664933).
        Box(Modifier.layoutId(NavigationSuiteLayoutIdTag)) {
            navigationSuite()
        }
        Box(Modifier.layoutId(ContentLayoutIdTag)) {
            content()
        }
        Box(
            modifier = Modifier
                .layoutId(MiniPlayerContentLayoutTag)
                .anchoredDraggable(
                    state = sheetState.draggableState,
                    orientation = Orientation.Vertical
                )
                .conditional(layoutType != NavigationSuiteType.NavigationBar) {
                    navigationBarsPadding()
                }
        ) {
            if(shouldShowMiniPlayer.value)
            miniPlayerContent()
        }
        Column(
            Modifier
                .layoutId(FullPlayerContentLayoutTag)
                .anchoredDraggable(
                    state = sheetState.draggableState,
                    orientation = Orientation.Vertical
                )
        ) {
            if(shouldShowFullPlayer.value)
            fullPlayerContent()
        }
//        Box(Modifier.layoutId(PlayerContentLayoutTag)) {
//            SuitPlayerContent(
//                sheetState = sheetState,
//                fullPlayerContent = fullPlayerContent,
//                miniPlayerContent = miniPlayerContent
//            )
//        }
    }) { measurables, constraints ->
        val looseConstraints = constraints.copy(minWidth = 0, minHeight = 0)
        // Find the navigation suite composable through it's layoutId tag
        val navigationPlaceable =
            measurables
                .fastFirst { it.layoutId == NavigationSuiteLayoutIdTag }
                .measure(looseConstraints)

        //TODO: have to measure how much height should we give
        val miniPlayerPlaceable =
            measurables
                .fastFirst { it.layoutId == MiniPlayerContentLayoutTag }
                .measure(looseConstraints)


        val fullPlayerPlaceable =
            measurables
                .fastFirst { it.layoutId == FullPlayerContentLayoutTag }
                .measure(looseConstraints)


        val isNavigationBar = layoutType == NavigationSuiteType.NavigationBar
        val layoutHeight = constraints.maxHeight
        val layoutWidth = constraints.maxWidth
        sheetState.updateAnchors(layoutHeight, miniPlayerPlaceable.height)

        // Find the content composable through it's layoutId tag
        val contentPlaceable =
            measurables
                .fastFirst { it.layoutId == ContentLayoutIdTag }
                .measure(
                    if (isNavigationBar) {
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
            if (isNavigationBar) {
                // Place content above the navigation component.
                contentPlaceable.placeRelative(0, 0)
                // Place the navigation component at the bottom of the screen
                // While also taking care of player swipe
                val navYOffset = (alpha.value * navigationPlaceable.height).toInt()
                navigationPlaceable.placeRelative(
                    x = 0,
                    y = layoutHeight - navYOffset
                )


                val playerYOffset =
                    (layoutHeight - miniPlayerPlaceable.height - navYOffset) * (1 - sheetState.sheetExpansionRatio)
                fullPlayerPlaceable.placeRelative(
                    x = 0,
                    y = playerYOffset.toInt()
                )
                //place mini player only when less than half expanded
                if (sheetState.sheetExpansionRatio <= 0.5f) {
                    miniPlayerPlaceable.placeRelative(
                        x = 0,
                        y = playerYOffset.toInt()
                    )
                }

            } else {
                // Place content to the side of the navigation component.
                contentPlaceable.placeRelative(navigationPlaceable.width, 0)
                // Place the navigation component at the start of the screen.
                navigationPlaceable.placeRelative(0, 0)

                // Place the player bottom of screen

                //when full sheetExpansionRatio =  -> y = 0
                //when mini -> y = layoutHeight - miniPlayerPlaceable.height


                val playerYOffset =
                    ((layoutHeight - miniPlayerPlaceable.height) * (1 - sheetState.sheetExpansionRatio)).toInt()
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
}

private val NoWindowInsets = WindowInsets(0, 0, 0, 0)
private const val NavigationSuiteLayoutIdTag = "navigationSuite"
private const val ContentLayoutIdTag = "content"
private const val PlayerContentLayoutTag = "playerContent"
private const val MiniPlayerContentLayoutTag = "minuPlayerContent"
private const val FullPlayerContentLayoutTag = "fullPlayerContent"