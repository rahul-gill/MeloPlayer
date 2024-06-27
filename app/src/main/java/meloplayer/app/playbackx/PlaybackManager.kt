package meloplayer.app.playbackx

import kotlinx.coroutines.CoroutineScope
import java.time.LocalDateTime


sealed class PlaybackState {
    data object Stopped : PlaybackState()

    data class Ongoing(
        val mediaItem: MediaItem,
        val position: PlaybackTimeline,
        val isPlaying: Boolean,
        val sleepTimer: SleepTimer? = null
    ) : PlaybackState()

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

sealed class PlaybackQueueState {
    data object Empty : PlaybackQueueState()
    data class NonEmpty(
        val queue: List<Long>,
        val currentItemIndex: Int
    ) : PlaybackQueueState()
}


sealed class PlaybackCommand {
    data object Stop : PlaybackCommand()

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
    StartPlaying,
    StopPlaying,
    ResumePlaying,
    PausePlaying,

    PlayingSongChanged,
    SeekByUser,

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


enum class RepeatMode { Off, One, All }
data class MediaItem(val id: Long)
data class PlaybackTimeline(val currentMillis: Long, val totalMills: Long)
sealed class SongTransitionType {
    data object Simple : SongTransitionType()
    data object Gapless : SongTransitionType()
    data class CrossFade(val fadeInDurationMillis: Long, val fadeOutDurationMillis: Long) :
        SongTransitionType()
}

data class SleepTimer(
    val durationMillis: Long,
    val sleepAt: LocalDateTime,
    val shouldCompleteLastSong: Boolean
)


interface PlaybackManagerX : EventSource<PlaybackEvents> {
    val playbackState: PlaybackState
    val playbackParamsState: PlaybackParamsState
    val playbackQueueState: PlaybackQueueState

    suspend fun handleCommand(command: PlaybackCommand)

    fun startWithRestore(parentScope: CoroutineScope)
    fun release()
}













