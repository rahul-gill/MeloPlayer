package meloplayer.app.ui.comps.nowplaying

import android.content.res.Configuration
import android.graphics.Bitmap.Config
import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowWidthSizeClass
import meloplayer.core.ui.components.nowplaying.NowPlayingAlbumArtTransitionStyle
import meloplayer.core.ui.components.nowplaying.PlayerSheetScaffoldDefaults


@Composable
fun NowPlayingPanel(
    playItem: (Long) -> Unit,
    playingQueueAlbumArtUris: List<Long>,
    currentItemIndex: Int?,
    currentPlaybackProgress: Float,
    setPlaybackProgress: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    if(playingQueueAlbumArtUris.isEmpty() || currentItemIndex == null || playingQueueAlbumArtUris.getOrNull(currentItemIndex) == null){
        return
    }
    //TODO: from preferences
    var albumArtStyle by remember {
        mutableStateOf(NowPlayingAlbumArtStyle.RoundedCard)
    }
    var transitionStyle by remember {
        mutableStateOf(NowPlayingAlbumArtTransitionStyle.CubeOutRotation)
    }

    val layoutTypeHorizontal =  PlayerSheetScaffoldDefaults.calculateIsNavBarType(currentWindowAdaptiveInfo())
    if(!layoutTypeHorizontal){
        Row(modifier = modifier) {
            NowPlayingAlbumArtCard(
                currentItemIndex,
                playItem,
                playingQueueAlbumArtUris,
                Modifier.fillMaxHeight().aspectRatio(1f).displayCutoutPadding(),
                albumArtStyle,
                transitionStyle,
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                NowPlayingControls(
                    currentPlaybackProgress = currentPlaybackProgress,
                    setPlaybackProgress = setPlaybackProgress
                )
                Row(Modifier.fillMaxWidth()) {
                    Column(
                        Modifier
                            .verticalScroll(rememberScrollState())
                            .weight(1f)
                    ) {
                        NowPlayingAlbumArtStyle.entries.forEach { s ->
                            TextButton(onClick = { albumArtStyle = s }) {
                                Text(text = s.name)
                            }
                        }
                    }
                    Column(
                        Modifier
                            .verticalScroll(rememberScrollState())
                            .weight(1f)
                    ) {
                        NowPlayingAlbumArtTransitionStyle.entries.forEach { s ->
                            TextButton(onClick = { transitionStyle = s }) {
                                Text(text = s.name)
                            }
                        }
                    }
                }
            }
        }
    }
    else Column(modifier = modifier) {
        NowPlayingAlbumArtCard(
            currentItemIndex,
            playItem,
            playingQueueAlbumArtUris,
            Modifier,
            albumArtStyle,
            transitionStyle
        )
        Spacer(modifier = Modifier.height(8.dp))
        NowPlayingControls(
            currentPlaybackProgress = currentPlaybackProgress,
            setPlaybackProgress = setPlaybackProgress
        )
        Row(Modifier.fillMaxWidth()) {
            Column(
                Modifier
                    .verticalScroll(rememberScrollState())
                    .weight(1f)
            ) {
                NowPlayingAlbumArtStyle.entries.forEach { s ->
                    TextButton(onClick = { albumArtStyle = s }) {
                        Text(text = s.name)
                    }
                }
            }
            Column(
                Modifier
                    .verticalScroll(rememberScrollState())
                    .weight(1f)
            ) {
                NowPlayingAlbumArtTransitionStyle.entries.forEach { s ->
                    TextButton(onClick = { transitionStyle = s }) {
                        Text(text = s.name)
                    }
                }
            }
        }
    }
}

