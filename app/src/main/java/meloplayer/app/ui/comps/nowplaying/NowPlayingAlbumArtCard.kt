package meloplayer.app.ui.comps.nowplaying

import android.net.Uri
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import meloplayer.core.store.MediaStoreUtils
import meloplayer.core.ui.components.nowplaying.NowPlayingAlbumArtTransitionStyle
import meloplayer.core.ui.components.nowplaying.applyTransitionStyle


@Composable
@OptIn(ExperimentalFoundationApi::class)
fun NowPlayingAlbumArtCard(
    currentItemIndex: Int,
    playItem: (index: Int) -> Unit,
    playingQueue: List<Long>,
    modifier: Modifier = Modifier,
        nowPlayingAlbumArtStyle: NowPlayingAlbumArtStyle = NowPlayingAlbumArtStyle.Circular,
        transitionStyle: NowPlayingAlbumArtTransitionStyle = NowPlayingAlbumArtTransitionStyle.Fade
) {
    val pagerState =
        rememberPagerState(pageCount = { playingQueue.size }, initialPage = currentItemIndex)

    LaunchedEffect(pagerState.currentPage) {
        if (pagerState.currentPage != currentItemIndex &&  pagerState.currentPage < playingQueue.size) {
            playItem(pagerState.currentPage)
        }
    }

    HorizontalPager(
        modifier = Modifier.statusBarsPadding().then(modifier),
        state = pagerState
    ) { page ->
        val transitionModifier = Modifier.applyTransitionStyle(transitionStyle, pagerState, page)
        val innerModifier = remember(nowPlayingAlbumArtStyle) {
            Modifier.getAlbumArtPagerModifier(nowPlayingAlbumArtStyle)
        }
        AsyncImage(
            model = MediaStoreUtils.getArtworkUriForSong(playingQueue[page]),
            null,
            modifier = Modifier
                .then(transitionModifier)
                .then(innerModifier)
        )
    }
}

enum class NowPlayingAlbumArtStyle {
    RoundedCardWithGradientOpacity,
    RoundedCard,
    FlatCard,
    Circular,
    FullWithoutPaddings,
    RoundedCardFullWithoutPaddings
}

private fun Modifier.getAlbumArtPagerModifier(
    nowPlayingAlbumArtStyle: NowPlayingAlbumArtStyle
): Modifier {
    var mod: Modifier = this
    if (nowPlayingAlbumArtStyle == NowPlayingAlbumArtStyle.Circular) {
        mod = mod.padding(horizontal = 32.dp)
    } else if (nowPlayingAlbumArtStyle != NowPlayingAlbumArtStyle.FullWithoutPaddings &&
        nowPlayingAlbumArtStyle != NowPlayingAlbumArtStyle.RoundedCardFullWithoutPaddings
    ) {
        mod = mod.padding(horizontal = 16.dp)
    }
    mod = mod
        .fillMaxWidth()
        .aspectRatio(1f)

    val roundedCorners = nowPlayingAlbumArtStyle == NowPlayingAlbumArtStyle.RoundedCard ||
            nowPlayingAlbumArtStyle == NowPlayingAlbumArtStyle.RoundedCardWithGradientOpacity ||
            nowPlayingAlbumArtStyle == NowPlayingAlbumArtStyle.RoundedCardFullWithoutPaddings



    if (roundedCorners) {
        mod = mod.clip(RoundedCornerShape(12.dp))
    } else if (nowPlayingAlbumArtStyle == NowPlayingAlbumArtStyle.Circular) {
        mod = mod.clip(CircleShape)
    }
    if (nowPlayingAlbumArtStyle == NowPlayingAlbumArtStyle.RoundedCardWithGradientOpacity) {
        mod = mod.drawWithCache {
            val gradient = Brush.verticalGradient(
                colors = listOf(
                    Color.Transparent,
                    Color.Black.copy(alpha = 0.2f),
                    Color.Black
                ),
                startY = 0f,
                endY = size.height
            )
            onDrawWithContent {
                drawContent()
                drawRect(gradient, blendMode = BlendMode.Multiply)
            }
        }
    }
    return mod
}

@Composable
@Preview
private fun NowPlayingAlbumArtCardPreview() {
    Surface(
        modifier = Modifier
            .fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(Modifier.fillMaxSize()) {
            NowPlayingAlbumArtCard(
                playingQueue = listOf(),
                playItem = {},
                currentItemIndex = 0
            )
        }
    }
}
