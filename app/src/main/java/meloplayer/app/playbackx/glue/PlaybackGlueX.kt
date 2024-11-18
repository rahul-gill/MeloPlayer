//package meloplayer.app.playbackx.glue
//
//import android.content.Context
//import android.util.Log
//import kotlinx.coroutines.CoroutineExceptionHandler
//import kotlinx.coroutines.CoroutineScope
//import kotlinx.coroutines.Dispatchers
//import kotlinx.coroutines.SupervisorJob
//import kotlinx.coroutines.flow.combine
//import kotlinx.coroutines.flow.launchIn
//import kotlinx.coroutines.flow.onStart
//import meloplayer.app.playbackx.PlaybackManagerImplX
//import meloplayer.app.playbackx.PlaybackManagerX
//import meloplayer.app.playbackx.PlaybackStateX
//import meloplayer.app.playbackx.service.PlaybackServiceX
////import meloplayer.app.playbackx.session.NotificationSessionManager
//import meloplayer.app.prefs.PreferenceManager
//import meloplayer.core.startup.applicationContextGlobal
//import kotlin.coroutines.CoroutineContext
//
//class PlaybackGlue(private val context: Context = applicationContextGlobal) {
//
//
//    private val job = SupervisorJob()
//    private val errorHandler = CoroutineExceptionHandler { _, throwable ->
//        Log.e("PlaybackService", throwable.toString())
//    }
//    private val coroutineScope = object : CoroutineScope {
//        override val coroutineContext: CoroutineContext
//            get() = job + errorHandler + Dispatchers.Main
//
//    }
//
//    val playbackManagerX: PlaybackManagerX = PlaybackManagerImplX(
//        context = context
//    )
//    private val sessionManager by lazy {
//        NotificationSessionManager.getImpl(
//            context = context,
//            coroutineScope = coroutineScope,
//            commandHandler = playbackManagerX::handleCommand,
//        )
//    }
//
//
//    fun onStartImpl() {
//        playbackManagerX.startWithRestore(coroutineScope)
//        sessionManager.start()
//
//        val shuffleMode = PreferenceManager.Playback.isShuffleOn.observableValue.apply {
//            onStart { emit(PreferenceManager.Playback.isShuffleOn.value) }
//        }
//        val loopMode = PreferenceManager.Playback.loopMode.observableValue.apply {
//            onStart { emit(PreferenceManager.Playback.loopMode.value) }
//        }
//        combine(
//            playbackManagerX.playbackStateX,
//            shuffleMode,
//            loopMode
//        ) { state, shuffleVal, loopModeVal ->
//            if (state is PlaybackStateX.OnGoing) {
//                sessionManager.update(
//                    isPlaying = state.isPlaying,
//                    position = state.timeline,
//                    currentSong = state.currentMediaItemId,
//                    isShuffleOn = shuffleVal,
//                    loopMode = loopModeVal
//                )
//            } else {
//                sessionManager.cancel()
//            }
//        }
//            .launchIn(scope = coroutineScope)
//    }
//
//    private fun onStopImpl() {
//        PlaybackServiceX.destroy()
//        sessionManager.destroy()
//    }
//
//
//    companion object {
//        val instance by lazy {
//            PlaybackGlue()
//        }
//    }
//}
