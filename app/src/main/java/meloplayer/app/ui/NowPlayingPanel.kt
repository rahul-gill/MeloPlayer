package meloplayer.app.ui

import android.content.ContentUris
import android.net.Uri
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import coil.compose.AsyncImage
import kotlin.math.absoluteValue


/**
 *
 */
@OptIn(ExperimentalFoundationApi::class)
fun PagerState.pageOffsetCoerced(page: Int) =
    ((currentPage - page) + currentPageOffsetFraction).absoluteValue.coerceIn(0f, 1f)

@OptIn(ExperimentalFoundationApi::class)
fun calculateScale(pagerState: PagerState, page: Int): Float {
    val distanceFromCenter = pagerState.pageOffsetCoerced(page)
    val offset = 0.4f // Adjust for desired scale difference
    return 1f - (distanceFromCenter * offset)
}


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NowPlayingPanel(
    albumId: Long?,
    playItemAtIndex: (Int) -> Unit,
    playingQueue: List<Painter>,
    currentItemIndex: Int,
    modifier: Modifier = Modifier
) {
    val pagerState =
        rememberPagerState(pageCount = { playingQueue.size }, initialPage = currentItemIndex)

    LaunchedEffect(pagerState.currentPage) {
        if (pagerState.currentPage != currentItemIndex) {
            playItemAtIndex(pagerState.currentPage)
        }
    }

    HorizontalPager(
        modifier = modifier,
        state = pagerState,
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp),
    ) { page ->

        val scaleFactor = calculateScale(pagerState, page)
        Box(
            Modifier
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.Center,

            ) {

            if(albumId != null){
                AsyncImage(
                    model = ContentUris.withAppendedId("content://media/external/audio/albumart".toUri(),
                        albumId
                    ),
                    null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .scale(scaleFactor)
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(12.dp))
                )
            } else {
                Text(text = "null")
            }
        }
    }
}