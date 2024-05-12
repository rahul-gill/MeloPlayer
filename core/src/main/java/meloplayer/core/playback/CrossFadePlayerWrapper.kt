package meloplayer.core.playback

import android.content.Context
import android.net.Uri
import android.os.Handler
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.flow
import meloplayer.core.playback.utils.Fader

class ExoPlayerFaderWrapper(
    private val context: Context
) {
    val player = createExoPlayerInstance()

    var fader: Fader? = null

    private fun createExoPlayerInstance() = ExoPlayer.Builder(context)
        .setHandleAudioBecomingNoisy(true)
        .build()
        .also {
            it.playWhenReady = false
//            TODO
//            it.addListener(@UnstableApi object : Player.Listener {
//                override fun onPlaybackStateChanged(playbackState: Int) {
//                    if (playbackState == Player.STATE_ENDED) {
//                        onPlaybackEnd()
//                    }
//                }
//
//                override fun onEvents(player: Player, events: Player.Events) {
//                    if (events.contains(Player.EVENT_PLAYER_ERROR)) {
//                        onPlayerError()
//                    }
//                }
//            })
        }

    val positionSecondsFlow
        get() = flow {
            while (true){
                emit(player.currentPosition)
                delay(1000)
            }
        }.conflate()

    fun release(){
        player.addListener(object : Player.Listener {
            override fun onTimelineChanged(timeline: Timeline, reason: Int) {
                super.onTimelineChanged(timeline, reason)
            }
        })
        player.release()
    }
}

class CrossFadePlayerWrapper(
    private val context: Context
) {
    var player = ExoPlayerFaderWrapper(context)
        private set


    fun startPlaying(nextUri: Uri) {
        val thisPlayer = player
        val nextPlayer = ExoPlayerFaderWrapper(context)

        run stopCurrentPlayback@{
            thisPlayer.setVolumeGraduallyTo(
                to = 0f,
                onFadeComplete = { thisPlayer.player.release() })
        }
        nextPlayer.player.setMediaItemUriAndPrepare(nextUri, onPrepared = {
            nextPlayer.setVolumeGraduallyTo(
                startingVolume = 0f,
                to = 1f
            )
            nextPlayer.player.play()
        })
        player = nextPlayer
    }


    private fun ExoPlayerFaderWrapper.setVolumeGraduallyTo(
        to: Float,
        startingVolume: Float? = null,
        onFadeComplete: (endedBecauseOfTimerEnd: Boolean) -> Unit = {}
    ) {
        if (startingVolume != null)
            player.volume = startingVolume
        fader?.stop()
        fader = Fader(
            startingVolume ?: player.volume, to, /*TODO fadePlaybackDuration = */1000,
            onUpdate = {
                Handler(context.mainLooper).post {
                    player.volume = it
                    println("SADSA set volume $player $it")
                }
            },
            onFinish = {
                Handler(context.mainLooper).post {
                    onFadeComplete(it)
                    println("SADSA fade cinoketd $player $it")
                }
                fader = null
            }

        )
        fader?.start()
    }

    private fun ExoPlayer.setMediaItemUriAndPrepare(uri: Uri, onPrepared: () -> Unit = {}) {
        setMediaItem(MediaItem.fromUri(uri))
        prepare()
        val listener = @UnstableApi object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) {
                    onPrepared()
                    this@setMediaItemUriAndPrepare.removeListener(this)
                }
            }
        }
        addListener(listener)
    }


}