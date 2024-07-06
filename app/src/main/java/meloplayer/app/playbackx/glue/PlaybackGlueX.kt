package meloplayer.app.playbackx.glue

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import meloplayer.app.playbackx.PlaybackManagerImplX
import meloplayer.app.playbackx.PlaybackManagerX
import meloplayer.app.playbackx.PlaybackStateX
import meloplayer.app.playbackx.service.PlaybackServiceX
import meloplayer.app.playbackx.session.NotificationSessionManager
import meloplayer.app.prefs.PreferenceManager
import meloplayer.core.startup.applicationContextGlobal
import kotlin.coroutines.CoroutineContext

class PlaybackGlue(private val context: Context = applicationContextGlobal) {


    private val job = SupervisorJob()
    private val errorHandler = CoroutineExceptionHandler { _, throwable ->
        Log.e("PlaybackService", throwable.toString())
    }
    private val coroutineScope = object : CoroutineScope {
        override val coroutineContext: CoroutineContext
            get() = job + errorHandler + Dispatchers.Main

    }

    val playbackManagerX: PlaybackManagerX = PlaybackManagerImplX(
        context = context,
        onEvents = { event, state ->
            println("Received event $event $state")
        },
    )
    private val sessionManager by lazy {
        NotificationSessionManager.getImpl(
            context = context,
            coroutineScope = coroutineScope,
            commandHandler = playbackManagerX::handleCommand,
        )
    }


    fun onStartImpl() {
        playbackManagerX.startWithRestore(coroutineScope)
        sessionManager.start()

        val shuffleMode = PreferenceManager.isShuffleOn.observableValue.apply {
            onStart { emit(PreferenceManager.isShuffleOn.value) }
        }
        val loopMode = PreferenceManager.loopMode.observableValue.apply {
            onStart { emit(PreferenceManager.loopMode.value) }
        }
        playbackManagerX.playbackStateX
            .onEach { state ->
                if (state is PlaybackStateX.OnGoing) {
                    println("Updating session with state:$state")
                    sessionManager.update(
                        isPlaying = state.isPlaying,
                        position = state.timeline,
                        currentSong = state.currentMediaItemId
                    )
                } else {
                    sessionManager.cancel()
                }
            }
            .launchIn(scope = coroutineScope)
    }

    private fun onStopImpl() {
        PlaybackServiceX.destroy()
        sessionManager.destroy()
    }


    companion object {
        val instance by lazy {
            PlaybackGlue()
        }
    }
}
