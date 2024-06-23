package meloplayer.app.playback.session

import android.graphics.Bitmap
import android.net.Uri
import meloplayer.app.playback.LoopMode
import meloplayer.app.playback.PlaybackPosition
import meloplayer.core.store.model.MediaStoreSong

data class RadioSessionUpdateRequest(
    val song: MediaStoreSong,
    val artworkBitmap: Bitmap,
    val artworkUri: Uri,
    val playbackPosition: PlaybackPosition,
    val isPlaying: Boolean,
    val isShuffleOn: Boolean,
    val loopMode: LoopMode
)