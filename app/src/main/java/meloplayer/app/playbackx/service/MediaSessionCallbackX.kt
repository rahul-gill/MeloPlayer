package meloplayer.app.playbackx.service

import android.content.Intent
import android.os.Build
import android.support.v4.media.session.MediaSessionCompat
import android.view.KeyEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import meloplayer.app.playbackx.PlaybackCommand
import meloplayer.app.playbackx.PlaybackManagerX

class MediaSessionCallbackX(
    coroutineScope: CoroutineScope,
    private val playbackManger: PlaybackManagerX
) : MediaSessionCompat.Callback() {
    private val scope = coroutineScope + SupervisorJob(coroutineScope.coroutineContext.job)
    override fun onPlay() {
        super.onPlay()
        scope.launch {
            playbackManger.handleCommand(PlaybackCommand.Play)
        }
    }

    override fun onPause() {
        super.onPause()
        scope.launch {
            playbackManger.handleCommand(PlaybackCommand.Pause)
        }
    }

    override fun onSkipToPrevious() {
        super.onSkipToPrevious()
        scope.launch {
            playbackManger.handleCommand(PlaybackCommand.SkipPrevious)
        }
    }

    override fun onSkipToNext() {
        super.onSkipToNext()
        scope.launch {
            playbackManger.handleCommand(PlaybackCommand.SkipNext)
        }
    }

    override fun onStop() {
        super.onStop()
        scope.launch {
            playbackManger.handleCommand(PlaybackCommand.Pause)
        }
    }

    override fun onSeekTo(pos: Long) {
        super.onSeekTo(pos)
        scope.launch {
            playbackManger.handleCommand(PlaybackCommand.SetPosition(pos))
        }
    }

    override fun onRewind() {
        super.onRewind()
    }

    private fun onRewindImpl() {
//        playbackManger.player.playbackPosition.value?.currentDurationMillis?.let { currentPos ->
//            val seekAt = (currentPos - PreferenceManager.rewindBackDuration.value)
//                .coerceAtLeast(0L)
//            playbackManger.player.seekTo(seekAt)
//        }
    }

    override fun onFastForward() {
        super.onFastForward()
//        playbackManger.player.playbackPosition.value?.let { pos ->
//            val seekAt = (pos.currentDurationMillis - PreferenceManager.rewindBackDuration.value)
//                .coerceAtMost(pos.totalDurationMillis)
//            playbackManger.player.seekTo(seekAt)
//        }
    }

    override fun onMediaButtonEvent(intent: Intent?): Boolean {
        val handled = super.onMediaButtonEvent(intent)
        if (handled) return true
        val keyEvent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent?.getParcelableExtra(
                Intent.EXTRA_KEY_EVENT,
                KeyEvent::class.java,
            )
        } else {
            @Suppress("DEPRECATION")
            intent?.getParcelableExtra(Intent.EXTRA_KEY_EVENT)
        }
        return when (keyEvent?.keyCode) {
            KeyEvent.KEYCODE_MEDIA_PREVIOUS -> {
                scope.launch {
                    playbackManger.handleCommand(PlaybackCommand.SkipPrevious)
                }
                true
            }

            KeyEvent.KEYCODE_MEDIA_REWIND -> {
                onRewindImpl()
                true
            }

            KeyEvent.KEYCODE_MEDIA_NEXT -> {
                scope.launch {
                    playbackManger.handleCommand(PlaybackCommand.SkipNext)
                }
                true
            }

            KeyEvent.KEYCODE_MEDIA_CLOSE,
            KeyEvent.KEYCODE_MEDIA_STOP,
            -> {
                scope.launch {
                    playbackManger.handleCommand(PlaybackCommand.Pause)
                }
                true
            }

            else -> false
        }
    }
}
