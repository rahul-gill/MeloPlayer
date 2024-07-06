package meloplayer.app.playbackx

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import java.time.LocalDateTime


sealed class PlaybackStateX {
    data object Empty : PlaybackStateX()

    /**
     * currentItemIndex must be in queue index range, otherwise use Empty state
     */
    data class OnGoing(
        val isPlaying: Boolean,
        val queue: List<Long>,
        val currentItemIndex: Int,
        val timeline: PlaybackTimeline,
        val sleepTimer: SleepTimer? = null,
    ) : PlaybackStateX() {
        val currentMediaItemId
            get() = queue[currentItemIndex]
    }
}

data class PlaybackParamsState(
    val speed: Float = 1f,
    val pitch: Float = 1f,
    val shuffleModeOn: Boolean = true,
    val repeatMode: RepeatMode = RepeatMode.All,
    val pauseFadeOutDurationMillis: Long = 1000,
    val playFadeInDurationMillis: Long = 1000,
    val songTransitionType: SongTransitionType = SongTransitionType.CrossFade(
        fadeInDurationMillis = 1000,
        fadeOutDurationMillis = 1000
    ),
    val shouldPauseOnZeroVolume: Boolean = false,
    val shouldResumeOnExternalDeviceConnect: Boolean = false,
    val shouldGoToPreviousSongOnExternalDeviceAction: Boolean = false,
    val durationToSkipPreviousSongMillis: Long = 4000
)


sealed class PlaybackCommand {
    data object SwitchPlaying : PlaybackCommand()
    data object Play : PlaybackCommand()
    data object Pause : PlaybackCommand()
    data object SkipPrevious : PlaybackCommand()
    data object SkipNext : PlaybackCommand()
    data class SetPosition(val pos: Long) : PlaybackCommand()

    data class SetShuffleMode(val on: Boolean) : PlaybackCommand()
    data class SetRepeatMode(val mode: RepeatMode) : PlaybackCommand()
    data class SetSpeed(val newSpeed: Float) : PlaybackCommand()
    data class SetPitch(val newPitch: Float) : PlaybackCommand()
    data class SetPauseFadeOutDuration(val duration: Long) : PlaybackCommand()
    data class SetPlayFadeInDuration(val duration: Long) : PlaybackCommand()
    data class SetSongTransitionType(val type: SongTransitionType) : PlaybackCommand()

    data class AddItemsToQueue(val items: List<Long>, val atIndex: Int? = null) : PlaybackCommand()
    data class RemoveAtIndexes(val indexes: List<Int>) : PlaybackCommand()
    data class MoveItemInQueue(val fromIndex: Int, val toIndex: Int) : PlaybackCommand()
    data object ClearQueue : PlaybackCommand()
    data class SetCurrentQueueItemIndex(val index: Int) : PlaybackCommand()
}

enum class PlaybackEvents {
    PlaybackCleared,

    ResumePlaying,
    PausePlaying,

    PlayingSongChanged,
    PlayingPositionChanged,

    QueueModified,
    QueueIndexChanged,

    SpeedChanged,
    PitchChanged,
    ShuffleModeChanged,
    RepeatModeChanged,
    PauseFadeOutDurationChanged,
    PlayFadeInDurationChanged,
    SongTransitionTypeChanged,
    ShouldPauseOnZeroVolumeChanged,
    ShouldResumeOnExternalDeviceConnectChanged,
    ExternalDeviceBackButtonBehaveChanged
}


enum class RepeatMode {
    Off, One, All;

    fun nextInShuffle() = when (this) {
        Off -> All
        One -> Off
        All -> One
    }
}

data class MediaItem(val id: Long)
sealed class PlaybackTimeline {
    data object Unprepared: PlaybackTimeline()
    data class Prepared(val currentMillis: Long, val totalMills: Long): PlaybackTimeline()
}
sealed class SongTransitionType {
    data object Simple : SongTransitionType()
    data class CrossFade(val fadeInDurationMillis: Long, val fadeOutDurationMillis: Long) :
        SongTransitionType()
}

data class SleepTimer(
    val durationMillis: Long,
    val sleepAt: LocalDateTime,
    val shouldCompleteLastSong: Boolean
)


interface PlaybackManagerX {
    val playbackParamsState: PlaybackParamsState
    val playbackStateX: StateFlow<PlaybackStateX>

    fun handleCommand(command: PlaybackCommand)

    fun startWithRestore(parentScope: CoroutineScope)
    fun release()
}













