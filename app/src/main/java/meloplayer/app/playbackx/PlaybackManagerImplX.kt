package meloplayer.app.playbackx

import android.content.ContentUris
import android.content.Context
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.provider.MediaStore
import androidx.compose.runtime.MutableState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.android.HandlerDispatcher
import kotlinx.coroutines.android.asCoroutineDispatcher
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.ArrayDeque
import java.util.Queue
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.ThreadFactory
import java.util.concurrent.ThreadPoolExecutor
import kotlin.jvm.Throws

class PlaybackManagerImplX(
    private val context: Context,
    private val onEvents: (PlaybackEvents, PlaybackStateX) -> Unit,
) : PlaybackManagerX {

    private val handlerThread: HandlerThread = HandlerThread("PlaybackThread")

    init {
        handlerThread.start()
    }

    private val handler = Handler(handlerThread.looper)
    private var scope =
        CoroutineScope(SupervisorJob() + handler.asCoroutineDispatcher("PlaybackThreadDispatcher"))
    private val mutex = Mutex()


    private val posUpdate: (pos: Long) -> Unit = { pos ->
        scope.launch {
            mutex.withLock {
                val currentPos = playbackStateX.value
                val params = playbackParamsState.songTransitionType

                if (currentPos is PlaybackStateX.OnGoing && currentPos.isPlaying) {
                    val dur = when (params) {
                        is SongTransitionType.CrossFade -> params.fadeOutDurationMillis
                        SongTransitionType.Simple -> 10
                    }
                    println("posUpdate timeline:${currentPos.timeline} dur:$dur")
                    if (currentPos.timeline is PlaybackTimeline.Prepared) {
                        val contentDuration = playerWrapper.getContentDuration()
                        if (currentPos.timeline.currentMillis + dur >= contentDuration) {
                            skipToNext(currentPos)
                        } else {
                            onSetPositionCommand(currentPos, pos)
                        }
                    }
                }
            }
        }
    }

    private lateinit var playerWrapper: MediaPlayerX
    private val randomNextIndexesHistory: Queue<Int> = ArrayDeque()
    override var playbackParamsState: PlaybackParamsState = PlaybackParamsState()
    override var playbackStateX = MutableStateFlow<PlaybackStateX>(PlaybackStateX.Empty)

    private var isStarted = false

    //State functions ==============================================================================
    override fun startWithRestore(parentScope: CoroutineScope) {
        scope.cancel()
        scope =
            CoroutineScope(SupervisorJob() + handler.asCoroutineDispatcher("PlaybackThreadDispatcher"))

        println("PlaybackManagerStart")
        scope.launch {
            mutex.withLock {
                isStarted = true
                withContext(Dispatchers.Main) {
                    playerWrapper = MediaPlayerX(
                        context = context,
                        onPosUpdate = posUpdate
                    )
                }

//        if (::scope.isInitialized && scope.isActive) {
//            scope.cancel()
//        }
//        scope = parentScope + SupervisorJob(parentScope.coroutineContext.job) + Dispatchers.Main
                //TODO: restore queue list, queue current item index, playback position millis

            }
        }
    }

    override fun release() {
        println("PlaybackManagerRelease")
        scope.launch {
            mutex.withLock {
                if (playbackStateX.value is PlaybackStateX.OnGoing) {
                    onPauseCommand(playbackStateX.value)
                }
                isStarted = false
            }
            //Clear playback and fade out stop
//        if (::scope.isInitialized) {
//            scope.cancel()
//        }
        }
    }
    //State functions end ==========================================================================

    override fun handleCommand(command: PlaybackCommand) {

        scope.launch {
            mutex.withLock {
                println("Received command: $command")
                when (command) {
                    is PlaybackCommand.AddItemsToQueue -> {
                        val (items, atIndex) = command
                        onAddItemsToQueue(items, atIndex)
                    }

                    PlaybackCommand.ClearQueue -> {
                        onClearQueue()
                    }

                    is PlaybackCommand.MoveItemInQueue -> {
                        val (fromIndex, toIndex) = command
                        moveItemInQueue(fromIndex, toIndex)
                    }

                    PlaybackCommand.Pause -> onPauseCommand(playbackStateX.value)
                    PlaybackCommand.Play -> onPlayCommand(playbackStateX.value)
                    PlaybackCommand.SwitchPlaying -> {
                        val state = playbackStateX.value
                        if (state is PlaybackStateX.OnGoing) {
                            if (state.isPlaying) onPauseCommand(state) else onPlayCommand(state)
                        }
                    }

                    PlaybackCommand.SkipNext -> skipToNext(playbackStateX.value)

                    PlaybackCommand.SkipPrevious -> {
                        val currentPlayback = playbackStateX.value
                        onSkipToPreviousCommand(currentPlayback)
                    }

                    is PlaybackCommand.SetPosition -> onSetPositionCommand(
                        playbackStateX.value,
                        command.pos
                    )

                    is PlaybackCommand.RemoveAtIndexes -> TODO()
                    is PlaybackCommand.SetCurrentQueueItemIndex -> {
                        val indexToSet = command.index
                        onSetCurrentIndex(indexToSet)
                    }

                    is PlaybackCommand.SetPauseFadeOutDuration -> TODO()
                    is PlaybackCommand.SetPitch -> TODO()
                    is PlaybackCommand.SetPlayFadeInDuration -> TODO()
                    is PlaybackCommand.SetRepeatMode -> TODO()
                    is PlaybackCommand.SetShuffleMode -> TODO()
                    is PlaybackCommand.SetSongTransitionType -> TODO()
                    is PlaybackCommand.SetSpeed -> TODO()

                }
            }
        }
    }

    private suspend fun onSetCurrentIndex(indexToSet: Int) {
        var currentQueueState = playbackStateX.value
        if (currentQueueState is PlaybackStateX.OnGoing
            && indexToSet in 0 until currentQueueState.queue.size
        ) {
            val toSetUri = currentQueueState.queue[indexToSet]
            currentQueueState = currentQueueState.copy(
                currentItemIndex = indexToSet,
                timeline = PlaybackTimeline.Unprepared
            )
            println("1StartUpdate")
            playbackStateX.value = currentQueueState
            println("2EndUpdate")
            if (!currentQueueState.isPlaying) {
                transitionToSongId(currentQueueState.currentMediaItemId)
            } else {
                playerWrapper.setMediaItemUriAndPrepare(
                    uri = songIdToUri(toSetUri),
                    onPrepared = {
                        val here = playbackStateX.value
                        if (here is PlaybackStateX.OnGoing)
                            playbackStateX.value =
                                here.copy(
                                    timeline = PlaybackTimeline.Prepared(
                                        0,
                                        totalMills = playerWrapper.getContentDuration()
                                    )
                                )
                        if (isStarted) onEvents(
                            PlaybackEvents.PlayingPositionChanged,
                            playbackStateX.value
                        )

                    }
                )
            }
            println("dispatch(PlayingSongChanged) from set current queue index")
            if (isStarted) onEvents(PlaybackEvents.PlayingSongChanged, playbackStateX.value)
        }
    }

    private suspend fun onSkipToPreviousCommand(
        currentPlayback: PlaybackStateX,
    ) {
        val durationToSkipPreviousMillis =
            playbackParamsState.durationToSkipPreviousSongMillis
        if (currentPlayback is PlaybackStateX.OnGoing) {
            if (currentPlayback.timeline is PlaybackTimeline.Prepared && currentPlayback.timeline.currentMillis > durationToSkipPreviousMillis) {
                onSetPositionCommand(currentPlayback, 0)
            } else {

                val songToPlayIndex = when {
                    playbackParamsState.shuffleModeOn && randomNextIndexesHistory.isNotEmpty() -> {
                        randomNextIndexesHistory.poll()!!
                    }

                    currentPlayback.currentItemIndex > 0 -> {
                        currentPlayback.currentItemIndex - 1
                    }

                    playbackParamsState.repeatMode == RepeatMode.All -> {
                        currentPlayback.queue.size - 1
                    }

                    else -> 0
                }
                transitionToSongId(currentPlayback.queue[songToPlayIndex])
                if (isStarted) onEvents(PlaybackEvents.PlayingSongChanged, playbackStateX.value)
            }
        }
    }


    private fun onSetPositionCommand(currentPlaybackState: PlaybackStateX, pos: Long) {
        if (currentPlaybackState is PlaybackStateX.OnGoing && currentPlaybackState.timeline is PlaybackTimeline.Prepared) {
            val coercedPos =
                pos.coerceIn(0..currentPlaybackState.timeline.totalMills)

            println("1StartUpdate")

            playbackStateX.value = currentPlaybackState.copy(
                timeline = currentPlaybackState.timeline.copy(currentMillis = coercedPos)
            )
            println("2EndUpdate")
            playerWrapper.seekTo(coercedPos)
        }
        if (isStarted) onEvents(PlaybackEvents.PlayingPositionChanged, playbackStateX.value)
    }

    private suspend fun skipToNext(currentQueueState: PlaybackStateX) {
        if (currentQueueState is PlaybackStateX.OnGoing) {
            val nextSongIndex = when {
                playbackParamsState.shuffleModeOn -> {
                    randomNextIndexesHistory.add(currentQueueState.currentItemIndex)
                    val nextRand = (0 until currentQueueState.queue.size).random()
                    if (nextRand == currentQueueState.currentItemIndex)
                        (nextRand + 1) % currentQueueState.queue.size
                    else
                        nextRand
                }

                currentQueueState.currentItemIndex + 1 < currentQueueState.queue.size -> {
                    currentQueueState.currentItemIndex + 1
                }

                playbackParamsState.repeatMode == RepeatMode.All -> {
                    (currentQueueState.currentItemIndex + 1) % currentQueueState.queue.size
                }

                else -> null
            }
            if (nextSongIndex != null) {
                println("3StartUpdate")

                this.playbackStateX.value = currentQueueState.copy(
                    isPlaying = true,
                    currentItemIndex = nextSongIndex,
                    timeline = PlaybackTimeline.Unprepared
                )

                println("3EndUpdate")
                if (isStarted) onEvents(PlaybackEvents.PlayingSongChanged, playbackStateX.value)
                transitionToSongId(currentQueueState.queue[nextSongIndex])
            } else {
                onPauseCommand(currentQueueState)
            }
        }
    }


    private suspend fun transitionToSongId(songId: Long) {
        val params = playbackParamsState
        when (params.songTransitionType) {
            is SongTransitionType.CrossFade -> {
                val (fadeInMills, fadeOutMillis) = params.songTransitionType
                val thisPlayer = playerWrapper

                val nextPlayer = withContext(Dispatchers.Main){
                    MediaPlayerX(
                        context,
                        onPosUpdate = posUpdate
                    )
                }
                thisPlayer.removeListener()

                playerWrapper = nextPlayer

                run stopCurrentPlayback@{
                    thisPlayer.setVolumeGraduallyTo(to = 0f,
                        fadeDuration = fadeOutMillis,
                        onFadeComplete = { thisPlayer.release() })
                }
                nextPlayer.setMediaItemUriAndPrepare(
                    uri = songIdToUri(songId),
                    onPrepared = {
                        thisPlayer.cancelPosUpdate()
                        nextPlayer.setVolumeGraduallyTo(
                            startingVolume = 0f,
                            to = 1f,
                            fadeDuration = fadeInMills
                        )

                        println("4StartUpdate")
                        val state = playbackStateX.value
                        if (state is PlaybackStateX.OnGoing) {
                            playbackStateX.value = state.copy(
                                timeline = PlaybackTimeline.Prepared(
                                    currentMillis = 0,
                                    totalMills = nextPlayer.getContentDuration()
                                )
                            )
                        }

                        println("4EndUpdate")
                        if (isStarted) onEvents(
                            PlaybackEvents.PlayingPositionChanged,
                            playbackStateX.value
                        )
                        nextPlayer.play()
                    })
            }

            SongTransitionType.Simple -> {
                playerWrapper.setMediaItemUriAndPrepare(
                    uri = songIdToUri(songId),
                    onPrepared = {

                        println("5StartUpdate")
                        val state = playbackStateX.value
                        if (state is PlaybackStateX.OnGoing)
                            playbackStateX.value = state.copy(
                                timeline = PlaybackTimeline.Prepared(
                                    currentMillis = 0,
                                    totalMills = playerWrapper.getContentDuration()
                                )
                            )

                        println("5EndUpdate")

                        if (isStarted) onEvents(
                            PlaybackEvents.PlayingPositionChanged,
                            playbackStateX.value
                        )
                        playerWrapper.play()
                    })
            }
        }
    }

    private fun moveItemInQueue(fromIndex: Int, toIndex: Int) {
        val state = playbackStateX.value
        if (state is PlaybackStateX.OnGoing
            && fromIndex in 0 until state.queue.size
            && toIndex in 0 until state.queue.size
        ) {
            val queueNew = state.queue.toMutableList()
            //O(n) remove and and
            val track = queueNew.removeAt(fromIndex)
            queueNew.add(toIndex, track)
            val newCurrentIndex = when (val prev = state.currentItemIndex) {
                fromIndex -> toIndex
                in (fromIndex + 1)..toIndex -> prev - 1
                in toIndex until fromIndex -> prev + 1
                else -> prev
            }

            println("6StartUpdate")
            playbackStateX.value = state.copy(
                queue = queueNew,
                currentItemIndex = newCurrentIndex
            )

            println("6EndUpdate")
        }
    }

    private suspend fun onPlayCommand(currentPlaybackState: PlaybackStateX) {
        try {
            val thisWrapper = playerWrapper
            if (currentPlaybackState !is PlaybackStateX.OnGoing) {
                return
            }
            val playerItemUri = thisWrapper.currentItemId()
            println("x")
            val stateItemUri = songIdToUri(currentPlaybackState.currentMediaItemId)
            if (stateItemUri != playerItemUri) {
                thisWrapper.setMediaItemUriAndPrepare(
                    uri = stateItemUri,
                    onPrepared = {
                        thisWrapper.setVolumeGraduallyTo(
                            startingVolume = 0f,
                            to = 1f,
                            fadeDuration = playbackParamsState.playFadeInDurationMillis
                        )

                        println("7StartUpdate")
                        val state = playbackStateX.value
                        if (state is PlaybackStateX.OnGoing)
                            playbackStateX.value = state.copy(
                                timeline = PlaybackTimeline.Prepared(
                                    currentMillis = 0,
                                    totalMills = thisWrapper.getContentDuration()
                                )
                            )

                        println("7EndUpdate")
                        thisWrapper.play()
                        if (isStarted) onEvents(
                            PlaybackEvents.PlayingSongChanged,
                            playbackStateX.value
                        )

                        if (isStarted) onEvents(
                            PlaybackEvents.PlayingPositionChanged,
                            playbackStateX.value
                        )
                    })
            } else if (!thisWrapper.isPlaying()) {
                thisWrapper.setVolumeGraduallyTo(
                    startingVolume = 0f,
                    to = 1f,
                    fadeDuration = playbackParamsState.playFadeInDurationMillis,
                )
                thisWrapper.play()
            }

            println("8StartUpdate")
            playbackStateX.value = currentPlaybackState.copy(isPlaying = true)

            println("8EndUpdate")
            if (isStarted) onEvents(PlaybackEvents.ResumePlaying, playbackStateX.value)
        } catch (e: Throwable) {
            println(e)
        }
    }


    private fun onPauseCommand(currentPlaybackState: PlaybackStateX) {
        val thisWrapper = playerWrapper
        if (currentPlaybackState is PlaybackStateX.OnGoing) {
            thisWrapper.setVolumeGraduallyTo(
                to = 0f,
                fadeDuration = playbackParamsState.pauseFadeOutDurationMillis,
                onFadeComplete = { thisWrapper.pause() }
            )

            println("9StartUpdate")
            playbackStateX.value = currentPlaybackState.copy(isPlaying = false)

            println("9EndUpdate")
            if (isStarted) onEvents(PlaybackEvents.PausePlaying, playbackStateX.value)
        }
    }

    private fun onClearQueue() {
        val currentPlaybackState = playbackStateX.value
        val thisWrapper = playerWrapper
        if (currentPlaybackState is PlaybackStateX.OnGoing) {
            thisWrapper.setVolumeGraduallyTo(
                to = 0f,
                fadeDuration = playbackParamsState.pauseFadeOutDurationMillis,
                onFadeComplete = { thisWrapper.pause() }
            )
        }

        println("10StartUpdate")
        playbackStateX.value = PlaybackStateX.Empty

        println("10EndUpdate")
        if (isStarted) onEvents(PlaybackEvents.PlaybackCleared, playbackStateX.value)
    }

    private fun onAddItemsToQueue(
        items: List<Long>, atIndex: Int?
    ) {
        val currentQueueState = playbackStateX.value
        when {
            items.isEmpty() -> {
                //do nothing
            }

            //Start from scratch case
            currentQueueState == PlaybackStateX.Empty -> {
                val firstItemIndex = if (playbackParamsState.shuffleModeOn) items.indices.random()
                else 0

                println("11StartUpdate")
                playbackStateX.value = PlaybackStateX.OnGoing(
                    isPlaying = false,
                    queue = items,
                    currentItemIndex = firstItemIndex,
                    timeline = PlaybackTimeline.Unprepared
                )

                println("11EndUpdate")
                if (isStarted) onEvents(PlaybackEvents.PlayingSongChanged, playbackStateX.value)
            }

            //append items to end of queue
            atIndex == null && currentQueueState is PlaybackStateX.OnGoing -> {

                println("12StartUpdate")
                playbackStateX.value = currentQueueState.copy(
                    queue = currentQueueState.queue + items
                )

                println("12EndUpdate")
            }

            //append in between the queue might disturb current playing item
            atIndex != null && currentQueueState is PlaybackStateX.OnGoing -> {
                if (atIndex >= currentQueueState.queue.size) {

                    println("13StartUpdate")
                    playbackStateX.value = currentQueueState.copy(
                        queue = currentQueueState.queue + items
                    )

                    println("13EndUpdate")
                } else if (atIndex > currentQueueState.currentItemIndex) {

                    println("14StartUpdate")
                    playbackStateX.value = currentQueueState.copy(
                        //O(n) copy
                        queue = currentQueueState.queue.toMutableList().apply {
                            addAll(atIndex, items)
                        })

                    println("14EndUpdate")
                } else {
                    val coercedAtIndex = atIndex.coerceAtLeast(0)

                    println("15StartUpdate")
                    playbackStateX.value = currentQueueState.copy(
                        //O(n) copy
                        queue = currentQueueState.queue.toMutableList().apply {
                            addAll(coercedAtIndex, items)
                        }, currentItemIndex = currentQueueState.currentItemIndex + items.size
                    )

                    println("15EndtUpdate")
                }
            }

        }
        if (items.isNotEmpty()) {
            if (isStarted) onEvents(PlaybackEvents.QueueModified, playbackStateX.value)
        }
    }

}


private fun songIdToUri(songId: Long) = ContentUris.withAppendedId(
    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
    songId
)