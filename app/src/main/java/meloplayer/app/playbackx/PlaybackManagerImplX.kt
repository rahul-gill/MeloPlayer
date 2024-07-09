package meloplayer.app.playbackx

import android.content.ContentUris
import android.content.Context
import android.os.Handler
import android.os.HandlerThread
import android.provider.MediaStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.android.asCoroutineDispatcher
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import meloplayer.app.prefs.PreferenceManager
import java.util.ArrayDeque
import java.util.Queue

class PlaybackManagerImplX(
    private val context: Context
) : PlaybackManagerX {

    private val handlerThread: HandlerThread = HandlerThread("PlaybackThread").apply {
        start()
    }
    private val handler = Handler(handlerThread.looper)
    private var scope =
        CoroutineScope(SupervisorJob() + handler.asCoroutineDispatcher("PlaybackThreadDispatcher"))
    private lateinit var playerWrapper: MediaPlayerX
    private val randomNextIndexesHistory: Queue<Int> = ArrayDeque()
    override var playbackStateX = MutableStateFlow<PlaybackStateX>(PlaybackStateX.Empty)
    private var isStarted = false

    //State functions ==============================================================================
    override fun startWithRestore(parentScope: CoroutineScope) {
        scope.cancel()
        scope =
            CoroutineScope(SupervisorJob() + handler.asCoroutineDispatcher("PlaybackThreadDispatcher"))
        scope.launch {
            run {
                isStarted = true
                withContext(Dispatchers.Main) {
                    playerWrapper = MediaPlayerX.build(context = context, onAboutToEnd = {
                        scope.launch {
                            skipToNext(playbackStateX.value)
                        }
                    })
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
        scope.launch {
            run {
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
            run {
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
                        playbackStateX.value, command.pos
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
        if (currentQueueState is PlaybackStateX.OnGoing && indexToSet in 0 until currentQueueState.queue.size) {
            val toSetUri = currentQueueState.queue[indexToSet]
            currentQueueState = currentQueueState.copy(
                currentItemIndex = indexToSet, timeline = PlaybackTimeline.Unprepared
            )
            playbackStateX.value = currentQueueState
            if (!currentQueueState.isPlaying) {
                transitionToSongId(currentQueueState.currentMediaItemId)
            } else {
                playerWrapper.setMediaItemUriAndPrepare(uri = songIdToUri(toSetUri), onPrepared = {
                    scope.launch {
                        val here = playbackStateX.value
                        if (here is PlaybackStateX.OnGoing) playbackStateX.value = here.copy(
                            timeline = PlaybackTimeline.Prepared(
                                0, totalMills = playerWrapper.getContentDuration()
                            )
                        )
                    }
                })
            }
        }
    }

    private suspend fun onSkipToPreviousCommand(
        currentPlayback: PlaybackStateX,
    ) {
        val durationToSkipPreviousMillis =
            PreferenceManager.Playback.durationToSkipPreviousSongMillis.value
        if (currentPlayback is PlaybackStateX.OnGoing) {
            if (currentPlayback.timeline is PlaybackTimeline.Prepared && currentPlayback.timeline.currentMillis > durationToSkipPreviousMillis) {
                onSetPositionCommand(currentPlayback, 0)
            } else {
                val songToPlayIndex = when {
                    PreferenceManager.Playback.isShuffleOn.value && randomNextIndexesHistory.isNotEmpty() -> {
                        randomNextIndexesHistory.poll()!!
                    }

                    currentPlayback.currentItemIndex > 0 -> {
                        currentPlayback.currentItemIndex - 1
                    }

                    PreferenceManager.Playback.loopMode.value == RepeatMode.All -> {
                        currentPlayback.queue.size - 1
                    }

                    else -> 0
                }
                transitionToSongId(currentPlayback.queue[songToPlayIndex])
            }
        }
    }


    private fun onSetPositionCommand(currentPlaybackState: PlaybackStateX, pos: Long) {
        if (currentPlaybackState is PlaybackStateX.OnGoing && currentPlaybackState.timeline is PlaybackTimeline.Prepared) {
            val coercedPos = pos.coerceIn(0..currentPlaybackState.timeline.totalMills)
            playbackStateX.value = currentPlaybackState.copy(
                timeline = currentPlaybackState.timeline.copy(currentMillis = coercedPos)
            )
            playerWrapper.seekTo(coercedPos)
        }
    }

    private suspend fun skipToNext(currentQueueState: PlaybackStateX) {
        if (currentQueueState is PlaybackStateX.OnGoing) {
            val nextSongIndex = when {
                PreferenceManager.Playback.isShuffleOn.value -> {
                    randomNextIndexesHistory.add(currentQueueState.currentItemIndex)
                    val nextRand = (0 until currentQueueState.queue.size).random()
                    if (nextRand == currentQueueState.currentItemIndex) (nextRand + 1) % currentQueueState.queue.size
                    else nextRand
                }

                currentQueueState.currentItemIndex + 1 < currentQueueState.queue.size -> {
                    currentQueueState.currentItemIndex + 1
                }

                PreferenceManager.Playback.loopMode.value == RepeatMode.All -> {
                    (currentQueueState.currentItemIndex + 1) % currentQueueState.queue.size
                }

                else -> null
            }
            if (nextSongIndex != null) {
                this.playbackStateX.value = currentQueueState.copy(
                    isPlaying = true,
                    currentItemIndex = nextSongIndex,
                    timeline = PlaybackTimeline.Unprepared
                )
                transitionToSongId(currentQueueState.queue[nextSongIndex])
            } else {
                onPauseCommand(currentQueueState)
            }
        }
    }


    private suspend fun transitionToSongId(songId: Long) {
        when (val type = PreferenceManager.Playback.songTransitionType.value) {
            is SongTransitionType.CrossFade -> {
                val (fadeInMills, fadeOutMillis) = type
                val thisPlayer = playerWrapper
                val nextPlayer = withContext(Dispatchers.Main) {
                    MediaPlayerX.build(context, onAboutToEnd = {
                        scope.launch {
                            skipToNext(playbackStateX.value)
                        }
                    })
                }
                playerWrapper = nextPlayer
                run stopCurrentPlayback@{
                    thisPlayer.setVolumeGraduallyTo(to = 0f,
                        fadeDuration = fadeOutMillis,
                        onFadeComplete = { thisPlayer.release() })
                }
                nextPlayer.setMediaItemUriAndPrepare(uri = songIdToUri(songId), onPrepared = {
                    scope.launch {
                        nextPlayer.setVolumeGraduallyTo(
                            startingVolume = 0f, to = 1f, fadeDuration = fadeInMills
                        )
                        val state = playbackStateX.value
                        if (state is PlaybackStateX.OnGoing) {
                            playbackStateX.value = state.copy(
                                timeline = PlaybackTimeline.Prepared(
                                    currentMillis = 0, totalMills = nextPlayer.getContentDuration()
                                )
                            )
                        }
                        nextPlayer.play()
                    }
                })
            }

            SongTransitionType.Simple -> {
                playerWrapper.setMediaItemUriAndPrepare(uri = songIdToUri(songId), onPrepared = {
                    scope.launch {

                        val state = playbackStateX.value
                        if (state is PlaybackStateX.OnGoing) playbackStateX.value = state.copy(
                            timeline = PlaybackTimeline.Prepared(
                                currentMillis = 0, totalMills = playerWrapper.getContentDuration()
                            )
                        )
                        playerWrapper.play()
                    }
                })
            }
        }
    }

    private fun moveItemInQueue(fromIndex: Int, toIndex: Int) {
        val state = playbackStateX.value
        if (state is PlaybackStateX.OnGoing && fromIndex in 0 until state.queue.size && toIndex in 0 until state.queue.size) {
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
            playbackStateX.value = state.copy(
                queue = queueNew, currentItemIndex = newCurrentIndex
            )
        }
    }

    private suspend fun onPlayCommand(currentPlaybackState: PlaybackStateX) {
        val thisWrapper = playerWrapper
        if (currentPlaybackState !is PlaybackStateX.OnGoing) {
            return
        }
        val playerItemUri = thisWrapper.currentItemId()
        val stateItemUri = songIdToUri(currentPlaybackState.currentMediaItemId)
        if (stateItemUri != playerItemUri) {
            thisWrapper.setMediaItemUriAndPrepare(uri = stateItemUri, onPrepared = {
                scope.launch {
                    thisWrapper.setVolumeGraduallyTo(
                        startingVolume = 0f,
                        to = 1f,
                        fadeDuration = PreferenceManager.Playback.playFadeInDurationMillis.value
                    )
                    val state = playbackStateX.value
                    if (state is PlaybackStateX.OnGoing) playbackStateX.value = state.copy(
                        timeline = PlaybackTimeline.Prepared(
                            currentMillis = 0, totalMills = thisWrapper.getContentDuration()
                        )
                    )
                    thisWrapper.play()
                }
            })
        } else if (!thisWrapper.isPlaying()) {
            thisWrapper.setVolumeGraduallyTo(
                startingVolume = 0f,
                to = 1f,
                fadeDuration = PreferenceManager.Playback.playFadeInDurationMillis.value,
            )
            thisWrapper.play()
        }
        playbackStateX.value = currentPlaybackState.copy(isPlaying = true)
    }


    private fun onPauseCommand(currentPlaybackState: PlaybackStateX) {
        val thisWrapper = playerWrapper
        if (currentPlaybackState is PlaybackStateX.OnGoing) {
            thisWrapper.setVolumeGraduallyTo(to = 0f,
                fadeDuration = PreferenceManager.Playback.pauseFadeOutDurationMillis.value,
                onFadeComplete = { thisWrapper.pause() })
            playbackStateX.value = currentPlaybackState.copy(isPlaying = false)
        }
    }

    private fun onClearQueue() {
        val currentPlaybackState = playbackStateX.value
        val thisWrapper = playerWrapper
        if (currentPlaybackState is PlaybackStateX.OnGoing) {
            thisWrapper.setVolumeGraduallyTo(to = 0f,
                fadeDuration = PreferenceManager.Playback.pauseFadeOutDurationMillis.value,
                onFadeComplete = { thisWrapper.pause() })
        }
        playbackStateX.value = PlaybackStateX.Empty
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
                val firstItemIndex =
                    if (PreferenceManager.Playback.isShuffleOn.value) items.indices.random()
                    else 0
                playbackStateX.value = PlaybackStateX.OnGoing(
                    isPlaying = false,
                    queue = items,
                    currentItemIndex = firstItemIndex,
                    timeline = PlaybackTimeline.Unprepared
                )
            }
            //append items to end of queue
            atIndex == null && currentQueueState is PlaybackStateX.OnGoing -> {
                playbackStateX.value = currentQueueState.copy(
                    queue = currentQueueState.queue + items
                )
            }
            //append in between the queue might disturb current playing item
            atIndex != null && currentQueueState is PlaybackStateX.OnGoing -> {
                if (atIndex >= currentQueueState.queue.size) {
                    playbackStateX.value = currentQueueState.copy(
                        queue = currentQueueState.queue + items
                    )
                } else if (atIndex > currentQueueState.currentItemIndex) {
                    playbackStateX.value = currentQueueState.copy(
                        //O(n) copy
                        queue = currentQueueState.queue.toMutableList().apply {
                            addAll(atIndex, items)
                        })
                } else {
                    val coercedAtIndex = atIndex.coerceAtLeast(0)
                    playbackStateX.value = currentQueueState.copy(
                        //O(n) copy
                        queue = currentQueueState.queue.toMutableList().apply {
                            addAll(coercedAtIndex, items)
                        }, currentItemIndex = currentQueueState.currentItemIndex + items.size
                    )
                }
            }
        }
    }
}


private fun songIdToUri(songId: Long) = ContentUris.withAppendedId(
    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, songId
)