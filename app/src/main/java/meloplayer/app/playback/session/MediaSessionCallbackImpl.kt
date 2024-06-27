package meloplayer.app.playback.session

import android.content.Intent
import android.os.Build
import android.support.v4.media.session.MediaSessionCompat
import android.view.KeyEvent
import meloplayer.app.playback.PlaybackManger
import meloplayer.app.prefs.PreferenceManager

class MediaSessionCallbackImpl(
    private val playbackManger: PlaybackManger
) : MediaSessionCompat.Callback() {
    override fun onPlay() {
        super.onPlay()
        if (!playbackManger.player.isPlaying.value) {
            playbackManger.player.switchIsPlaying()
        }
    }

    override fun onPause() {
        super.onPause()
        if (playbackManger.player.isPlaying.value) {
            playbackManger.player.switchIsPlaying()
        }
    }

    override fun onSkipToPrevious() {
        super.onSkipToPrevious()
        playbackManger.skipToPrevious()
    }

    override fun onSkipToNext() {
        super.onSkipToNext()
        playbackManger.goToNextSong()
    }

    override fun onStop() {
        super.onStop()
        if(playbackManger.player.isPlaying.value){
            playbackManger.player.switchIsPlaying()
        }
    }

    override fun onSeekTo(pos: Long) {
        super.onSeekTo(pos)
        playbackManger.player.seekTo(pos)
    }

    override fun onRewind() {
        super.onRewind()
        onRewindImpl()
    }

    private fun onRewindImpl() {
        playbackManger.player.playbackPosition.value?.currentDurationMillis?.let { currentPos ->
            val seekAt = (currentPos - PreferenceManager.rewindBackDuration.value)
                .coerceAtLeast(0L)
            playbackManger.player.seekTo(seekAt)
        }
    }

    override fun onFastForward() {
        super.onFastForward()
        playbackManger.player.playbackPosition.value?.let { pos ->
            val seekAt = (pos.currentDurationMillis - PreferenceManager.rewindBackDuration.value)
                .coerceAtMost(pos.totalDurationMillis)
            playbackManger.player.seekTo(seekAt)
        }
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
                playbackManger.skipToPrevious()
                true
            }

            KeyEvent.KEYCODE_MEDIA_REWIND -> {
                onRewindImpl()
                true
            }

            KeyEvent.KEYCODE_MEDIA_NEXT -> {
                playbackManger.goToNextSong()
                true
            }

            KeyEvent.KEYCODE_MEDIA_CLOSE,
            KeyEvent.KEYCODE_MEDIA_STOP,
            -> {
                if (playbackManger.player.isPlaying.value) {
                    playbackManger.player.switchIsPlaying()
                }
                true
            }

            else -> false
        }
    }
}
