//package meloplayer.app.ui.comps.nowplaying
//
//import android.content.ContentUris
//import androidx.compose.animation.AnimatedContent
//import androidx.compose.foundation.background
//import androidx.compose.foundation.layout.Box
//import androidx.compose.foundation.layout.Column
//import androidx.compose.foundation.layout.Row
//import androidx.compose.foundation.layout.Spacer
//import androidx.compose.foundation.layout.WindowInsets
//import androidx.compose.foundation.layout.fillMaxHeight
//import androidx.compose.foundation.layout.fillMaxWidth
//import androidx.compose.foundation.layout.height
//import androidx.compose.foundation.layout.navigationBars
//import androidx.compose.foundation.layout.padding
//import androidx.compose.foundation.layout.size
//import androidx.compose.foundation.layout.width
//import androidx.compose.foundation.layout.windowInsetsPadding
//import androidx.compose.foundation.layout.wrapContentHeight
//import androidx.compose.foundation.shape.RoundedCornerShape
//import androidx.compose.material.icons.Icons
//import androidx.compose.material.icons.filled.FastForward
//import androidx.compose.material.icons.filled.FastRewind
//import androidx.compose.material.icons.filled.Pause
//import androidx.compose.material.icons.filled.PlayArrow
//import androidx.compose.material.icons.filled.SkipNext
//import androidx.compose.material.icons.filled.SkipPrevious
//import androidx.compose.material3.ElevatedCard
//import androidx.compose.material3.Icon
//import androidx.compose.material3.IconButton
//import androidx.compose.material3.MaterialTheme
//import androidx.compose.material3.Text
//import androidx.compose.runtime.Composable
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.draw.clip
//import androidx.compose.ui.graphics.RectangleShape
//import androidx.compose.ui.text.style.TextOverflow
//import androidx.compose.ui.unit.dp
//import androidx.core.net.toUri
//import coil.compose.AsyncImage
//import meloplayer.core.ui.conditional
//
//@Composable
//fun MiniPlayer(
//    modifier: Modifier = Modifier,
//    currentSong: MediaStoreSong,
//    playbackProgress: Float,
//    showTrackControls: Boolean = true,
//    showSeekControls: Boolean = false,
//    onSkipToPrevious: () -> Unit,
//    onSkipToNext: () -> Unit,
//    seekBack: () -> Unit = {},
//    seekForward: () -> Unit = {},
//    isPlaying: Boolean = false,
//    onClick: () -> Unit,
//    onSwitchPlayPause: () -> Unit,
//    insetPaddings: WindowInsets? = WindowInsets.navigationBars
//) {
//    ElevatedCard(
//        modifier = Modifier
//            .fillMaxWidth()
//            .wrapContentHeight()
//            .then(modifier),
//        onClick = onClick,
//        shape = RectangleShape,
//    ) {
//        Column(
//            modifier = Modifier
//                .conditional(insetPaddings != null) {
//                    windowInsetsPadding(insetPaddings!!)
//                }
//        ) {
//            Box(
//                modifier = Modifier
//                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
//                    .height(2.dp)
//                    .fillMaxWidth()
//            ) {
//                Box(
//                    modifier = Modifier
//                        .align(Alignment.CenterStart)
//                        .background(MaterialTheme.colorScheme.primary)
//                        .fillMaxHeight()
//                        .fillMaxWidth(playbackProgress)
//                )
//            }
//            Row(
//                verticalAlignment = Alignment.CenterVertically,
//                modifier = Modifier.padding(0.dp, 8.dp),
//            ) {
//                Spacer(modifier = Modifier.width(12.dp))
//                AnimatedContent(
//                    label = "now-playing-art",
//                    targetState = currentSong,
//                    contentKey = { it.id },
//                ) { song ->
//                    AsyncImage(
//                        ContentUris.withAppendedId(
//                            "content://media/external/audio/albumart".toUri(),
//                            song.albumId
//                        ),
//                        null,
//                        modifier = Modifier
//                            .size(45.dp)
//                            .clip(RoundedCornerShape(10.dp))
//                    )
//                }
//                Spacer(modifier = Modifier.width(15.dp))
//                AnimatedContent(
//                    label = "c-now-playing-card-content",
//                    modifier = Modifier.weight(1f),
//                    targetState = currentSong,
//                    contentKey = { it.id },
//                ) { song ->
//                    Column(modifier = Modifier.fillMaxWidth()) {
//                        Text(
//                            text = song.title,
//                            maxLines = 1,
//                            overflow = TextOverflow.Ellipsis,
//                            style = MaterialTheme.typography.bodyMedium,
//                        )
//                        if (song.artistNames.isNotEmpty()) {
//                            Text(
//                                song.artistNames.joinToString(", "),
//                                maxLines = 1,
//                                overflow = TextOverflow.Ellipsis,
//                                style = MaterialTheme.typography.bodySmall,
//                            )
//                        }
//                    }
//                }
//                Spacer(modifier = Modifier.width(15.dp))
//                if (showTrackControls) {
//                    IconButton(
//                        onClick = onSkipToPrevious
//                    ) {
//                        Icon(Icons.Filled.SkipPrevious, null)
//                    }
//                }
//                if (showSeekControls) {
//                    IconButton(
//                        onClick = seekBack
//                    ) {
//                        Icon(Icons.Filled.FastRewind, null)
//                    }
//                }
//                IconButton(
//                    onClick = onSwitchPlayPause
//                ) {
//                    Icon(
//                        when {
//                            !isPlaying -> Icons.Filled.PlayArrow
//                            else -> Icons.Filled.Pause
//                        },
//                        null
//                    )
//                }
//                if (showSeekControls) {
//                    IconButton(
//                        onClick = seekForward
//                    ) {
//                        Icon(Icons.Filled.FastForward, null)
//                    }
//                }
//                if (showTrackControls) {
//                    IconButton(
//                        onClick = onSkipToNext
//                    ) {
//                        Icon(Icons.Filled.SkipNext, null)
//                    }
//                }
//                Spacer(modifier = Modifier.width(8.dp))
//            }
//        }
//    }
//
//}