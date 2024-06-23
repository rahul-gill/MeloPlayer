package meloplayer.core.ui.components.nowplaying

import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.unit.Density
import kotlin.math.abs
import kotlin.math.absoluteValue
import kotlin.math.min
import kotlin.math.sqrt

//from https://medium.com/androiddevelopers/customizing-compose-pager-with-fun-indicators-and-transitions-12b3b69af2cc
//and https://www.sinasamaki.com/pager-animations/

enum class NowPlayingAlbumArtTransitionStyle {
    Normal, Fade, Depth, CubeInRotation, CubeOutRotation, Hinge, CircularReveal,
    VerticalFlip, HorizontalFlip,
    StackedMove,
    RotatedMove
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Modifier.applyTransitionStyle(
    transitionStyle: NowPlayingAlbumArtTransitionStyle,
    pagerState: PagerState,
    page: Int
) = when (transitionStyle) {
    NowPlayingAlbumArtTransitionStyle.Normal ->
        Modifier

    NowPlayingAlbumArtTransitionStyle.Fade ->
        pagerTransitionFade(pagerState, page)

    NowPlayingAlbumArtTransitionStyle.Depth ->
        pagerTransitionDepth(pagerState, page)

    NowPlayingAlbumArtTransitionStyle.CubeInRotation ->
        pagerTransitionCubeInRotation(pagerState, page)

    NowPlayingAlbumArtTransitionStyle.CubeOutRotation ->
        pagerTransitionCubeOutRotate(pagerState, page)

    NowPlayingAlbumArtTransitionStyle.Hinge ->
        pagerTransitionHinge(pagerState, page)

    NowPlayingAlbumArtTransitionStyle.CircularReveal ->
        pagerTransitionCircularReveal(pagerState, page)

    NowPlayingAlbumArtTransitionStyle.VerticalFlip -> pagerTransitionVerticalFlip(pagerState, page)
    NowPlayingAlbumArtTransitionStyle.HorizontalFlip -> pagerTransitionHorizontalFlip(
        pagerState,
        page
    )

    NowPlayingAlbumArtTransitionStyle.RotatedMove -> pagerTransitionRotatedMove(pagerState, page)
    NowPlayingAlbumArtTransitionStyle.StackedMove ->
        pagerTransitionOneAboveOtherCarousal(pagerState, page)
}


@OptIn(ExperimentalFoundationApi::class)
private fun PagerState.startOffsetForPage(page: Int): Float {
    return calculateCurrentOffsetForPage(page).coerceAtLeast(0f)
}

@OptIn(ExperimentalFoundationApi::class)
private fun PagerState.endOffsetForPage(page: Int): Float {
    return calculateCurrentOffsetForPage(page).coerceAtMost(0f)
}

@OptIn(ExperimentalFoundationApi::class)
private fun PagerState.calculateCurrentOffsetForPage(page: Int): Float {
    return (currentPage - page) + currentPageOffsetFraction
}

@OptIn(ExperimentalFoundationApi::class)
private fun PagerState.pageOffsetCoerced(page: Int) =
    calculateCurrentOffsetForPage(page).absoluteValue.coerceIn(0f, 1f)

@OptIn(ExperimentalFoundationApi::class)
private fun calculateScale(pagerState: PagerState, page: Int) = derivedStateOf {
    val distanceFromCenter = pagerState.pageOffsetCoerced(page)

    val offset = 0.4f // Adjust for desired scale difference
    1f - (distanceFromCenter * offset)
}

@OptIn(ExperimentalFoundationApi::class)
private fun Modifier.pagerTransitionFade(
    pagerState: PagerState,
    page: Int
): Modifier {
    return graphicsLayer {
        val pageOffset = pagerState.getOffsetDistanceInPages(page)
        translationX = pageOffset * size.width
        alpha = 1 - pageOffset.absoluteValue
    }
}

@OptIn(ExperimentalFoundationApi::class)
private fun Modifier.pagerTransitionDepth(
    pagerState: PagerState,
    page: Int
): Modifier {
    val factor = calculateScale(pagerState, page)
    return scale(factor.value)
}


@OptIn(ExperimentalFoundationApi::class)
private fun Modifier.pagerTransitionCubeInRotation(
    pagerState: PagerState,
    page: Int
) = graphicsLayer {
    cameraDistance = 32f
    val pageOffset = pagerState.getOffsetDistanceInPages(page)

    when {
        (pageOffset < -1f) -> {
            // page is far off screen
            alpha = 0f
        }

        (pageOffset <= 0) -> {
            // page is to the right of the selected page or the selected page
            alpha = 1f
            transformOrigin = TransformOrigin(0f, 0.5f)
            rotationY = -90f * pageOffset.absoluteValue

        }

        (pageOffset <= 1) -> {
            // page is to the left of the selected page
            alpha = 1f
            transformOrigin = TransformOrigin(1f, 0.5f)
            rotationY = 90f * pageOffset.absoluteValue
        }

        else -> {
            alpha = 0f
        }
    }
}


@OptIn(ExperimentalFoundationApi::class)
private fun Modifier.pagerTransitionHinge(
    pagerState: PagerState,
    page: Int
) = graphicsLayer {
    // Calculate the absolute offset for the current page from the
    // scroll position.
    val pageOffset = pagerState.getOffsetDistanceInPages(page)
    translationX = pageOffset * size.width
    transformOrigin = TransformOrigin(0f, 0f)

    when {
        (pageOffset < -1f) -> {
            // page is far off screen
            alpha = 0f
        }

        (pageOffset <= 0) -> {
            // page is to the right of the selected page or the selected page
            alpha = 1f - pageOffset.absoluteValue
            rotationZ = 0f

        }

        (pageOffset <= 1) -> {
            // page is to the left of the selected page
            alpha = 1f - pageOffset.absoluteValue
            rotationZ = 90f * pageOffset.absoluteValue
        }

        else -> {
            alpha = 0f
        }
    }
}


@OptIn(ExperimentalFoundationApi::class)
private fun Modifier.pagerTransitionCubeOutRotate(
    pagerState: PagerState,
    page: Int
) = graphicsLayer {
    val pageOffset = pagerState.calculateCurrentOffsetForPage(page)
    val offScreenRight = pageOffset < 0f
    val deg = 105f
    val interpolated = FastOutLinearInEasing.transform(pageOffset.absoluteValue)
    rotationY = min(interpolated * if (offScreenRight) deg else -deg, 90f)

    transformOrigin = TransformOrigin(
        pivotFractionX = if (offScreenRight) 0f else 1f,
        pivotFractionY = .5f
    )
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalComposeUiApi::class)
@Composable
private fun Modifier.pagerTransitionCircularReveal(
    pagerState: PagerState,
    page: Int
): Modifier {
    var offsetY by remember { mutableFloatStateOf(0f) }
    return pointerInteropFilter {
        offsetY = it.y
        false
    }.graphicsLayer {
        // MAKE THE PAGE NOT MOVE
        val pageOffset = pagerState.calculateCurrentOffsetForPage(page)
        translationX = size.width * pageOffset

        // ADD THE CIRCULAR CLIPPING
        val endOffset = pagerState.endOffsetForPage(page)

        //shadowElevation = 20f
        shape = CirclePath(
            progress = 1f - endOffset.absoluteValue,
            origin = Offset(
                size.width,
                offsetY,
            )
        )
        clip = true

        // PARALLAX SCALING
        val absoluteOffset = pagerState.calculateCurrentOffsetForPage(page).absoluteValue
        val scale = 1f + (absoluteOffset.absoluteValue * .4f)

        scaleX = scale
        scaleY = scale

        // FADE AWAY
        val startOffset = pagerState.startOffsetForPage(page)
        alpha = (2f - startOffset) / 2f

    }

}

private class CirclePath(
    private val progress: Float,
    private val origin: Offset = Offset(0f, 0f)
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: androidx.compose.ui.unit.LayoutDirection,
        density: Density
    ): Outline {

        val center = Offset(
            x = size.center.x - ((size.center.x - origin.x) * (1f - progress)),
            y = size.center.y - ((size.center.y - origin.y) * (1f - progress)),
        )
        val radius = (sqrt(
            size.height * size.height + size.width * size.width
        ) * .5f) * progress

        return Outline.Generic(
            Path().apply {
                addOval(
                    Rect(
                        center = center,
                        radius = radius,
                    )
                )
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
private fun Modifier.pagerTransitionOneAboveOtherCarousal(
    pagerState: PagerState,
    page: Int
) = graphicsLayer {
    val startOffset = pagerState.startOffsetForPage(page)
    translationX = size.width * (startOffset * .99f)

    alpha = (2f - startOffset) / 2f
    val blur = (startOffset * 20f).coerceAtLeast(0.1f)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        renderEffect = RenderEffect
            .createBlurEffect(
                blur, blur, Shader.TileMode.DECAL
            ).asComposeRenderEffect()

    }

    val scale = 1f - (startOffset * .1f)
    scaleX = scale
    scaleY = scale
}


@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun Modifier.pagerTransitionRotatedMove(
    pagerState: PagerState,
    page: Int
) = graphicsLayer {
    val pageOffset = pagerState.calculateCurrentOffsetForPage(page)
    val startOffset = pagerState.startOffsetForPage(page)

    translationX = size.width * (startOffset * .99f)
    val offScreenLeft = pageOffset > 0f
    val deg = 105f
    val interpolated = FastOutLinearInEasing.transform(pageOffset.absoluteValue)
    rotationZ = min(interpolated * if (offScreenLeft) -deg else deg, 90f)
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun Modifier.pagerTransitionVerticalFlip(
    pagerState: PagerState,
    page: Int
) = graphicsLayer {

    val position = -pagerState.getOffsetDistanceInPages(page)
    translationX = -position * size.width
    cameraDistance = 2000f

    when {
        position < -1 -> {     // [-Infinity,-1)
            // This page is way off-screen to the left.
            alpha = 0f
        }

        position <= 0 -> {    // [-1,0]
            alpha = 1f
            rotationX = 180 * (1 - abs(position) + 1)
        }

        position <= 0.5 -> {    // (0,0.5]
            alpha = 1f
            rotationX = -180 * (1 - abs(position) + 1)
        }

        position <= 1 -> {    // (0.5,1]
            alpha = 0f
            rotationX = -180 * (1 - abs(position) + 1)
        }

        else -> {    // (1,+Infinity]
            // This page is way off-screen to the right.
            alpha = 0f
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun Modifier.pagerTransitionHorizontalFlip(
    pagerState: PagerState,
    page: Int
) = graphicsLayer {

    val position = -pagerState.getOffsetDistanceInPages(page)
    translationX = -position * size.width
    cameraDistance = 2000f

    when {
        position < -1 -> {     // [-Infinity,-1)
            // This page is way off-screen to the left.
            alpha = 0f
        }

        position <= 0 -> {    // [-1,0]
            alpha = 1f
            rotationY = 180 * (1 - abs(position) + 1)
        }

        position <= 0.5 -> {    // (0,0.5]
            alpha = 1f
            rotationY = -180 * (1 - abs(position) + 1)
        }

        position <= 1 -> {    // (0.5,1]
            alpha = 0f
            rotationY = -180 * (1 - abs(position) + 1)
        }

        else -> {    // (1,+Infinity]
            // This page is way off-screen to the right.
            alpha = 0f
        }
    }
}