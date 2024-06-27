package meloplayer.app.playbackx

import android.content.Context
import android.net.Uri
import android.os.Handler
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import meloplayer.app.playback.utils.Fader


/**
 * Cohesion of player and fader, one fader for each player
 */
class MediaPlayerX(
    private val context: Context,
    private val speed: Float = 1f,
    private val pitch: Float = 1f,
    private val onError: () -> Unit = {},
    private val onPlayPauseChange: () -> Unit = {}
) {
    private var fader: Fader? = null

    private val playerListener = @UnstableApi object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            onPlayPauseChange()
        }

        override fun onEvents(player: Player, events: Player.Events) {
            if (events.contains(Player.EVENT_IS_PLAYING_CHANGED)) {
                onPlayPauseChange()
            }
            if (events.contains(Player.EVENT_PLAYER_ERROR)) {
                onError()
            }
        }
    }

    val player = createExoPlayerInstance()

    fun removeListener() {
        player.removeListener(playerListener)
    }


    @OptIn(UnstableApi::class)
    private fun createExoPlayerInstance() =
        ExoPlayer.Builder(context)
            .setHandleAudioBecomingNoisy(true)
            .setRenderersFactory(DefaultRenderersFactory(context).apply {
                setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON)
            })
            .build().also { exo ->
                exo.playWhenReady = false
                exo.playbackParameters = PlaybackParameters(speed, pitch)
                exo.addListener(playerListener)
            }


    fun release() {
        player.release()
    }


    fun setVolumeGraduallyTo(
        to: Float,
        fadeDuration: Long,
        startingVolume: Float? = null,
        onFadeComplete: (endedBecauseOfTimerEnd: Boolean) -> Unit = {}
    ) {
        if (startingVolume != null) player.volume = startingVolume
        fader?.stop()
        fader = Fader(startingVolume ?: player.volume,
            to,
            fadeDuration.toInt(),
            onUpdate = {
                Handler(context.mainLooper).post {
                    player.volume = it
                }
            },
            onFinish = {
                Handler(context.mainLooper).post {
                    onFadeComplete(it)
                }
                fader = null
            }

        )
        fader?.start()
    }

    fun setMediaItemUriAndPrepare(uri: Uri, onPrepared: () -> Unit = {}) {
        player.run {
            setMediaItem(MediaItem.fromUri(uri))
            prepare()
            val listener = @UnstableApi object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (playbackState == Player.STATE_READY) {
                        onPrepared()
                        this@run.removeListener(this)
                    }
                }
            }
            addListener(listener)
        }
    }
}