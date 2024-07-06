package meloplayer.app.playbackx

import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import androidx.annotation.MainThread
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import meloplayer.app.playbackx.utils.Fader


/**
 * Cohesion of player and fader, one fader for each player
 */

class MediaPlayerX @MainThread constructor(
    private val context: Context,
    private val speed: Float = 1f,
    private val pitch: Float = 1f,
    private val onError: () -> Unit = {},
    private val onPosUpdate: (Long) -> Unit = {}
) {
    private var fader: Fader? = null
    private val handler = Handler(context.mainLooper)

    private val playerListener: Player.Listener = @UnstableApi object : Player.Listener {


        override fun onEvents(player: Player, events: Player.Events) {
            if (events.contains(Player.EVENT_PLAYER_ERROR)) {
                onError()
            }
        }
    }

    private val _player by lazy { createExoPlayerInstance() }

    suspend fun getContentDuration(): Long {
        return withContext(Dispatchers.Main){
            _player.contentDuration
        }
    }

    suspend fun isPlaying(): Boolean{
        return withContext(Dispatchers.Main){
            _player.isPlaying
        }
    }


    suspend fun currentItemId(): Uri?{
        return withContext(Dispatchers.Main){
            _player.currentMediaItem?.requestMetadata?.mediaUri
        }
    }



    fun pause() {
        handler.post {
            _player.pause()
        }
    }


    fun play() {
        handler.post {
            _player.play()
        }
    }

    fun seekTo(pos: Long) {
        handler.post {
            _player.seekTo(pos)
        }
    }

    fun removeListener() {
        handler.post {
            _player.removeListener(playerListener)
        }

    }


    @OptIn(UnstableApi::class)
    private fun createExoPlayerInstance() =
        ExoPlayer.Builder(context)
            .setHandleAudioBecomingNoisy(true)
            .setRenderersFactory(DefaultRenderersFactory(context).apply {
                setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON)
            })
            .setLooper(context.mainLooper)
            .build().also { exo ->
                exo.playWhenReady = false
                exo.playbackParameters = PlaybackParameters(speed, pitch)
                exo.addListener(playerListener)
            }

    fun cancelPosUpdate() {
        handler.removeCallbacksAndMessages(null)
    }

    fun release() {
        _player.release()
    }


    fun setVolumeGraduallyTo(
        to: Float,
        fadeDuration: Long,
        startingVolume: Float? = null,
        onFadeComplete: (endedBecauseOfTimerEnd: Boolean) -> Unit = {}
    ) {
        handler.post {
            if (startingVolume != null) _player.volume = startingVolume
            fader?.stop()
            fader = Fader(startingVolume ?: _player.volume,
                to,
                fadeDuration.toInt(),
                onUpdate = {
                    handler.post {
                        _player.volume = it
                    }
                },
                onFinish = {
                    handler.post {
                        onFadeComplete(it)
                    }
                    fader = null
                }

            )
            fader?.start()
        }
    }

    private fun posUpdate() {
        val pos = if (_player.isPlaying) {
            _player.currentPosition
        } else {
            null
        }
        if (pos != null) {
            onPosUpdate(pos)
        }
        handler.postDelayed(::posUpdate, 1000)
    }

    fun setMediaItemUriAndPrepare(uri: Uri, onPrepared: suspend () -> Unit = {}) {
        handler.post {
            _player.run {
                setMediaItem(MediaItem.fromUri(uri))
                prepare()
                val listener = @UnstableApi object : Player.Listener {
                    override fun onPlaybackStateChanged(playbackState: Int) {
                        if (playbackState == Player.STATE_READY) {
                            runBlocking {
                                onPrepared()
                            }
                            this@run.removeListener(this)
                        }
                    }
                }
                handler.postDelayed(::posUpdate, 1000)
                addListener(listener)
            }
        }
    }
}