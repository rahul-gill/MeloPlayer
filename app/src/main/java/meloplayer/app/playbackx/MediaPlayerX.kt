package meloplayer.app.playbackx

import android.content.Context
import android.net.Uri
import android.os.Handler
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
import java.util.UUID


interface MediaPlayerX {
    suspend fun getContentDuration(): Long
    suspend fun isPlaying(): Boolean
    suspend fun currentItemId(): Uri?
    fun pause()
    fun play()
    fun seekTo(pos: Long)
    fun setMediaItemUriAndPrepare(uri: Uri, onPrepared: () -> Unit = {})
    fun release()
    fun setVolumeGraduallyTo(
        to: Float,
        fadeDuration: Long,
        startingVolume: Float? = null,
        onFadeComplete: (endedBecauseOfTimerEnd: Boolean) -> Unit = {}
    )

    companion object {
        suspend fun build(
            context: Context,
            onError: () -> Unit = {},
            onAboutToEnd: () -> Unit = {}
        ): MediaPlayerX = withContext(Dispatchers.Main) {
            MediaPlayerXImpl(context, onError, onAboutToEnd)
        }
    }
}

/**
 * - Cohesion of player and fader, one fader for each player
 * - Everything on Main thread
 * - TODO: handle speed pitch
 */

private class MediaPlayerXImpl @MainThread constructor(
    private val context: Context,
    private val onError: () -> Unit = {},
    private val onAboutToEnd: () -> Unit = {}
) : MediaPlayerX {
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

    override suspend fun getContentDuration(): Long {
        return withContext(Dispatchers.Main) {
            _player.contentDuration
        }
    }

    override suspend fun isPlaying(): Boolean {
        return withContext(Dispatchers.Main) {
            _player.isPlaying
        }
    }


    override suspend fun currentItemId(): Uri? {
        return withContext(Dispatchers.Main) {
            _player.currentMediaItem?.requestMetadata?.mediaUri
        }
    }


    override fun pause() {
        handler.post {
            _player.pause()
        }
    }


    override fun play() {
        handler.post {
            _player.play()
        }
    }

    override fun seekTo(pos: Long) {
        handler.post {
            _player.seekTo(pos)
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
                exo.addListener(playerListener)
            }


    override fun release() {
        handler.post {
            handler.removeCallbacksAndMessages(null)
            _player.release()
        }
    }


    override fun setVolumeGraduallyTo(
        to: Float,
        fadeDuration: Long,
        startingVolume: Float?,
        onFadeComplete: (endedBecauseOfTimerEnd: Boolean) -> Unit
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

    private var currentPosUpdater = UUID.randomUUID()

    override fun setMediaItemUriAndPrepare(uri: Uri, onPrepared: () -> Unit) {
        handler.post {
            _player.run {
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
                currentPosUpdater = UUID.randomUUID()
                handler.postDelayed(object : Runnable {
                    override fun run() {
                        //TODO: pref
                        if (_player.currentPosition + 1050 > _player.duration) {
                            onAboutToEnd()
                        } else {
                            handler.postDelayed(this, 1000)
                        }
                    }

                }, 1000)
                addListener(listener)
            }
        }
    }
}