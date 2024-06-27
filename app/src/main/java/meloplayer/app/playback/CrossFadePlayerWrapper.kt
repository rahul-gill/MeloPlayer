//package meloplayer.app.playback
//
//import android.content.Context
//import android.net.Uri
//import android.os.Handler
//import androidx.annotation.OptIn
//import androidx.media3.common.C
//import androidx.media3.common.MediaItem
//import androidx.media3.common.PlaybackParameters
//import androidx.media3.common.Player
//import androidx.media3.common.util.UnstableApi
//import androidx.media3.exoplayer.DefaultRenderersFactory
//import androidx.media3.exoplayer.ExoPlayer
//import kotlinx.coroutines.CoroutineScope
//import kotlinx.coroutines.Dispatchers
//import kotlinx.coroutines.SupervisorJob
//import kotlinx.coroutines.cancel
//import kotlinx.coroutines.delay
//import kotlinx.coroutines.flow.MutableStateFlow
//import kotlinx.coroutines.flow.StateFlow
//import kotlinx.coroutines.flow.flow
//import kotlinx.coroutines.flow.flowOn
//import kotlinx.coroutines.flow.launchIn
//import kotlinx.coroutines.flow.onEach
//import kotlinx.coroutines.flow.update
//import kotlinx.coroutines.job
//import kotlinx.coroutines.launch
//import kotlinx.coroutines.plus
//import meloplayer.app.playback.utils.Fader
//import meloplayer.core.prefs.Preference
//
//
//data class PlaybackPosition(
//    val totalDurationMillis: Long, val currentDurationMillis: Long
//)
//
//interface MeloPlayer {
//    val isPlaying: StateFlow<Boolean>
//    val playbackPosition: StateFlow<PlaybackPosition?>
//    fun switchIsPlaying()
//    fun seekTo(millis: Long)
//    fun setSpeed(value: Float)
//    fun setPitch(value: Float)
//    fun setNextUri(uri: Uri)
//    fun start(coroutineScope: CoroutineScope)
//    fun release()
//
//    companion object {
//        fun getImpl(
//            context: Context,
//            playbackSpeedPref: Preference<Float>,
//            playbackPitchPref: Preference<Float>
//        ): MeloPlayer = CrossFadePlayerWrapper(
//            context, playbackSpeedPref, playbackPitchPref
//        )
//    }
//}
//
//private class CrossFadePlayerWrapper(
//    private val context: Context,
//    private val playbackSpeedPref: Preference<Float>,
//    private val playbackPitchPref: Preference<Float>,
//    //private val onPlayerEvent: (MeloPlayerEvent) -> Unit = {},
//) : MeloPlayer {
//    private lateinit var scope: CoroutineScope
//    private val _isPlaying = MutableStateFlow(false)
//
//    var dontSetPosition = false
//    override val isPlaying
//        get() = _isPlaying
//    private val onPlayPauseChange: () -> Unit =
//        {
//            _isPlaying.update { player.player.isPlaying }
//        }
//    private var player = ExoPlayerFaderWrapper(
//        context, playbackSpeedPref.value, playbackPitchPref.value,
//        onPlayPauseChange = onPlayPauseChange
//    )
//    private val _playbackPosition = MutableStateFlow<PlaybackPosition?>(null)
//
//
//    //TODO this seems to be no good
//    override val playbackPosition
//        get() = _playbackPosition
//
//    override fun switchIsPlaying() {
//        if (player.player.isPlaying)
//            player.player.pause()
//        else
//            player.player.play()
//    }
//
//    override fun seekTo(millis: Long) {
//        scope.launch {
//            val currentPos = _playbackPosition.value
//
//            player.player.seekTo(millis)
//            if (millis < player.player.contentDuration && currentPos != null) {
//                _playbackPosition.update {
//                    currentPos.copy(currentDurationMillis = millis)
//                }
//                dontSetPosition = true
//                delay(1500)
//                dontSetPosition = false
//            }
//        }
//    }
//
//    override fun setSpeed(value: Float) {
//        playbackSpeedPref.setValue(value)
//    }
//
//    override fun setPitch(value: Float) {
//        playbackPitchPref.setValue(value)
//    }
//
//    override fun setNextUri(uri: Uri) {
//        val thisPlayer = player
//
//        val nextPlayer =
//            ExoPlayerFaderWrapper(
//                context, playbackSpeedPref.value, playbackPitchPref.value,
//                onPlayPauseChange = onPlayPauseChange
//            )
//        thisPlayer.removeListener()
//        player = nextPlayer
//        run stopCurrentPlayback@{
//            thisPlayer.setVolumeGraduallyTo(to = 0f,
//                onFadeComplete = { thisPlayer.player.release() })
//        }
//        nextPlayer.setMediaItemUriAndPrepare(uri, onPrepared = {
//            nextPlayer.setVolumeGraduallyTo(
//                startingVolume = 0f, to = 1f
//            )
//            nextPlayer.player.play()
//        })
//    }
//
//    override fun start(coroutineScope: CoroutineScope) {
//        scope = coroutineScope + SupervisorJob(coroutineScope.coroutineContext.job)
//        playbackSpeedPref.observableValue
//            .onEach {
//                player.player.playbackParameters = player.player.playbackParameters.withSpeed(it)
//            }
//            .flowOn(Dispatchers.Main)
//            .launchIn(scope)
//
//        playbackPitchPref.observableValue
//            .onEach {
//                player.player.playbackParameters =
//                    PlaybackParameters(player.player.playbackParameters.speed, it)
//            }
//            .flowOn(Dispatchers.Main)
//            .launchIn(scope)
//
//        flow {
//            while (true) {
//                val pos = getCurrentPlaybackPosition()
//                emit(pos)
//                delay(1000)
//            }
//        }
//            .flowOn(Dispatchers.Main)
//            .onEach { new ->
//                if (!dontSetPosition) {
//                    _playbackPosition.update { new }
//                }
//            }
//            .launchIn(scope)
//
//    }
//
//    override fun release() {
//        scope.cancel()
//        player.release()
//    }
//
//
//    private fun getCurrentPlaybackPosition(): PlaybackPosition? {
//        return if (player.player.contentDuration == C.TIME_UNSET) {
//            null
//        } else {
//            (PlaybackPosition(player.player.contentDuration, player.player.currentPosition))
//        }
//    }
//}
//
//
///**
// * Cohesion of player and fader, one fader for each player
// */
//private class ExoPlayerFaderWrapper(
//    private val context: Context,
//    private val speed: Float,
//    private val pitch: Float,
//    private val onError: () -> Unit = {},
//    private val onPlayPauseChange: () -> Unit = {}
//) {
//
//    var fader: Fader? = null
//
//    private val playerListener = @UnstableApi object : Player.Listener {
//        override fun onPlaybackStateChanged(playbackState: Int) {
//            onPlayPauseChange()
//        }
//
//        override fun onEvents(player: Player, events: Player.Events) {
//            if (events.contains(Player.EVENT_IS_PLAYING_CHANGED)) {
//                onPlayPauseChange()
//            }
//            if (events.contains(Player.EVENT_PLAYER_ERROR)) {
//                onError()
//            }
//        }
//    }
//
//    val player = createExoPlayerInstance()
//
//    fun removeListener() {
//        player.removeListener(playerListener)
//    }
//
//
//    @OptIn(UnstableApi::class)
//    private fun createExoPlayerInstance() =
//        ExoPlayer.Builder(context)
//            .setHandleAudioBecomingNoisy(true)
//            .setRenderersFactory(DefaultRenderersFactory(context).apply {
//                setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON)
//            })
//            .build().also { exo ->
//                exo.playWhenReady = false
//                exo.playbackParameters = PlaybackParameters(speed, pitch)
//                exo.addListener(playerListener)
//            }
//
//
//    fun release() {
//        player.release()
//    }
//
//
//    fun setVolumeGraduallyTo(
//        to: Float,
//        startingVolume: Float? = null,
//        onFadeComplete: (endedBecauseOfTimerEnd: Boolean) -> Unit = {}
//    ) {
//        if (startingVolume != null) player.volume = startingVolume
//        fader?.stop()
//        fader = Fader(startingVolume ?: player.volume,
//            to, /*TODO fadePlaybackDuration = */
//            1000,
//            onUpdate = {
//                Handler(context.mainLooper).post {
//                    player.volume = it
//                }
//            },
//            onFinish = {
//                Handler(context.mainLooper).post {
//                    onFadeComplete(it)
//                }
//                fader = null
//            }
//
//        )
//        fader?.start()
//    }
//
//    fun setMediaItemUriAndPrepare(uri: Uri, onPrepared: () -> Unit = {}) {
//        player.run {
//            setMediaItem(MediaItem.fromUri(uri))
//            prepare()
//            val listener = @UnstableApi object : Player.Listener {
//                override fun onPlaybackStateChanged(playbackState: Int) {
//                    if (playbackState == Player.STATE_READY) {
//                        onPrepared()
//                        this@run.removeListener(this)
//                    }
//                }
//            }
//            addListener(listener)
//        }
//    }
//}