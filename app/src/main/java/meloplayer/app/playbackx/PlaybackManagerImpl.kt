//package meloplayer.app.playbackx
//
//import android.content.ContentUris
//import android.content.Context
//import android.provider.MediaStore
//import androidx.media3.common.PlaybackParameters
//import kotlinx.coroutines.CoroutineScope
//import kotlinx.coroutines.Dispatchers
//import kotlinx.coroutines.SupervisorJob
//import kotlinx.coroutines.cancel
//import kotlinx.coroutines.job
//import kotlinx.coroutines.launch
//import kotlinx.coroutines.plus
//import kotlinx.coroutines.sync.Mutex
//import kotlinx.coroutines.sync.withLock
//
//class PlaybackManagerImpl(
//    private val context: Context,
//) : PlaybackManagerX, EventSourceImpl<PlaybackEvents>() {
//
//    private val mutex = Mutex()
//    private lateinit var scope: CoroutineScope
//
//    override var playbackState: PlaybackState = PlaybackState.Stopped
//    override var playbackParamsState: PlaybackParamsState = PlaybackParamsState()
//    override var playbackStateX: PlaybackStateX = PlaybackStateX.Empty
//
//    private var inProgSkipNext = false
//    private val posUpdate: (Long) -> Unit = { pos ->
//        scope.launch {
//            val currentPos = playbackState
//            if (currentPos is PlaybackState.Ongoing && currentPos.isPlaying) {
//                val params = playbackParamsState.songTransitionType
//                val dur = when (params) {
//                    is SongTransitionType.CrossFade -> params.fadeOutDurationMillis
//                    SongTransitionType.Gapless -> 10
//                    SongTransitionType.Simple -> 10
//                }
//                if (currentPos.position.currentMillis + dur >= playerWrapper.player.contentDuration) {
//                    println("Current song close to end so")
//                    if (!inProgSkipNext) {
//                        inProgSkipNext = true
//                        handleCommand(PlaybackCommand.SkipNext)
//                        inProgSkipNext = false
//                    }
//                } else {
//                    playbackState = currentPos.copy(
//                        position = currentPos.position.copy(
//                            currentMillis = pos,
//                            totalMills = playerWrapper.player.contentDuration
//                        )
//                    )
//                    dispatchEvent(PlaybackEvents.PlayingPositionChanged)
//                }
//            }
//        }
//    }
//
//    private var playerWrapper = MediaPlayerX(context, onPosUpdate = posUpdate)
//
//
//    override fun startWithRestore(parentScope: CoroutineScope) {
//        scope = parentScope + SupervisorJob(parentScope.coroutineContext.job) + Dispatchers.Main
//        //TODO: restore queue list, queue current item index, playback position millis
//    }
//
//    override fun release() {
//        if (::scope.isInitialized) {
//            scope.cancel()
//        }
//    }
//
//
//    override suspend fun handleCommand(command: PlaybackCommand): Unit = mutex.withLock {
//        println("Received command: $command")
//        when (command) {
//            is PlaybackCommand.AddItemsToQueue -> {
//                val (items, atIndex) = command
//                onAddItemsToQueue(items, atIndex)
//            }
//
//            PlaybackCommand.ClearQueue -> {
//                onClearQueue()
//            }
//
//            is PlaybackCommand.MoveItemInQueue -> {
//                val currentQueueState = playbackStateX
//                val (fromIndex, toIndex) = command
//                if (currentQueueState is PlaybackStateX.NonEmpty
//                    && fromIndex in 0 until currentQueueState.queue.size
//                    && toIndex in 0 until currentQueueState.queue.size
//                ) {
//                    val queueNew = currentQueueState.queue.toMutableList()
//                    val track = queueNew.removeAt(fromIndex)
//                    queueNew.add(toIndex, track)
//                    val newCurrentIndex = when (val prev = currentQueueState.currentItemIndex) {
//                        fromIndex -> toIndex
//                        in (fromIndex + 1)..toIndex -> prev - 1
//                        in toIndex until fromIndex -> prev + 1
//                        else -> prev
//                    }
//                    playbackStateX = PlaybackStateX.NonEmpty(
//                        queue = queueNew,
//                        currentItemIndex = newCurrentIndex
//                    )
//                }
//            }
//
//            PlaybackCommand.Pause -> {
//                onPauseCommand()
//            }
//
//            PlaybackCommand.Play -> {
//                onPlayCommand()
//            }
//
//            is PlaybackCommand.RemoveAtIndexes -> {
//                val indexesToRemove = command.indexes
//                onRemoveAtIndexes(indexesToRemove)
//            }
//
//            is PlaybackCommand.SetCurrentQueueItemIndex -> {
//                val currentQueueState = playbackStateX
//                val currentPlaybackState = playbackState
//                val indexToSet = command.index
//                if (currentQueueState is PlaybackStateX.NonEmpty
//                    && currentPlaybackState is PlaybackState.Ongoing
//                    && indexToSet in 0 until currentQueueState.queue.size
//                ) {
//                    val toSetUri = currentQueueState.queue[indexToSet]
//                    playbackState = PlaybackState.Ongoing(
//                        mediaItem = MediaItem(toSetUri),
//                        position = PlaybackTimeline(
//                            currentMillis = 0,
//                            totalMills = 0
//                        ),
//                        isPlaying = currentPlaybackState.isPlaying
//                    )
//                    if (!currentPlaybackState.isPlaying) {
//                        transitionToSongId(currentQueueState.queue[indexToSet])
//                    } else {
//                        playerWrapper.setMediaItemUriAndPrepare(
//                            uri = songIdToUri(toSetUri),
//                            onPrepared = {
//                                val state = playbackState
//                                if(state is PlaybackState.Ongoing){
//                                    playbackState = state.copy(position = state.position.copy(totalMills = playerWrapper.player.contentDuration))
//                                }
//                            }
//                        )
//                    }
//                    println("dispatch(PlayingSongChanged) from set current queue index")
//                    dispatchEvent(PlaybackEvents.PlayingSongChanged)
//                }
//            }
//
//            is PlaybackCommand.SetPauseFadeOutDuration -> {
//                playbackParamsState = playbackParamsState.copy(
//                    pauseFadeOutDurationMillis = command.duration
//                )
//            }
//
//            is PlaybackCommand.SetPitch -> {
//                playbackParamsState = playbackParamsState.copy(
//                    pitch = command.newPitch
//                )
//                playerWrapper.player.playbackParameters =
//                    playerWrapper.player.playbackParameters.withSpeed(command.newPitch)
//            }
//
//            is PlaybackCommand.SetPlayFadeInDuration -> {
//                playbackParamsState = playbackParamsState.copy(
//                    playFadeInDurationMillis = command.duration
//                )
//            }
//
//            is PlaybackCommand.SetPosition -> {
//                onSetPositionCommand(command.pos)
//            }
//
//            is PlaybackCommand.SetRepeatMode -> {
//                playbackParamsState = playbackParamsState.copy(
//                    repeatMode = command.mode
//                )
//            }
//
//            is PlaybackCommand.SetShuffleMode -> {
//                playbackParamsState = playbackParamsState.copy(
//                    shuffleModeOn = command.on
//                )
//            }
//
//            is PlaybackCommand.SetSongTransitionType -> {
//                playbackParamsState = playbackParamsState.copy(
//                    songTransitionType = command.type
//                )
//            }
//
//            is PlaybackCommand.SetSpeed -> {
//                playbackParamsState = playbackParamsState.copy(
//                    speed = command.newSpeed
//                )
//                playerWrapper.player.playbackParameters =
//                    PlaybackParameters(
//                        playerWrapper.player.playbackParameters.speed,
//                        command.newSpeed
//                    )
//
//            }
//
//            PlaybackCommand.SkipNext -> {
//                val currentQueueState = playbackStateX
//                if (currentQueueState is PlaybackStateX.NonEmpty) {
//                    val nextSongIndex = when {
//                        playbackParamsState.shuffleModeOn -> {
//                            val nextRand = (0 until currentQueueState.queue.size).random()
//                            if (nextRand == currentQueueState.currentItemIndex)
//                                (nextRand + 1) % currentQueueState.queue.size
//                            else
//                                nextRand
//                        }
//
//                        currentQueueState.currentItemIndex + 1 < currentQueueState.queue.size -> {
//                            currentQueueState.currentItemIndex + 1
//                        }
//
//                        playbackParamsState.repeatMode == RepeatMode.All -> {
//                            (currentQueueState.currentItemIndex + 1) % currentQueueState.queue.size
//                        }
//
//                        else -> null
//                    }
//                    if (nextSongIndex != null) {
//                        transitionToSongId(currentQueueState.queue[nextSongIndex])
//                        playbackState = PlaybackState.Ongoing(
//                            mediaItem = MediaItem(currentQueueState.queue[nextSongIndex]),
//                            position = PlaybackTimeline(
//                                currentMillis = 0,
//                                totalMills = 0
//                            ),
//                            isPlaying = true
//                        )
//                    } else {
//                        onPauseCommand()
//                    }
//                }
//            }
//
//            PlaybackCommand.SkipPrevious -> {
//                val currentQueue = playbackStateX
//                val currentPlayback = playbackState
//                val durationToSkipPreviousMillis =
//                    playbackParamsState.durationToSkipPreviousSongMillis
//                if (currentPlayback is PlaybackState.Ongoing && currentQueue is PlaybackStateX.NonEmpty) {
//                    if (currentPlayback.position.currentMillis > durationToSkipPreviousMillis) {
//                        onSetPositionCommand(0)
//                    } else {
//                        val songToPlayIndex = when {
//                            playbackParamsState.shuffleModeOn -> {
//                                //TODO: we gotta remember previously played songs
//                                0
//                            }
//
//                            currentQueue.currentItemIndex > 0 -> {
//                                currentQueue.currentItemIndex - 1
//                            }
//
//                            playbackParamsState.repeatMode == RepeatMode.All -> {
//                                currentQueue.queue.size - 1
//                            }
//
//                            else -> 0
//                        }
//                        transitionToSongId(currentQueue.queue[songToPlayIndex])
//                    }
//                }
//            }
//
//            PlaybackCommand.Stop -> {
//                //TODO: maybe remove this command
//            }
//
//            PlaybackCommand.SwitchPlaying -> {
//                val state = playbackState
//                if (state is PlaybackState.Ongoing) {
//                    handleCommand(if (state.isPlaying) PlaybackCommand.Pause else PlaybackCommand.Play)
//                }
//            }
//        }
//    }
//
//    private fun onSetPositionCommand(pos: Long) {
//        val currentPlaybackState = playbackState
//        if (currentPlaybackState is PlaybackState.Ongoing) {
//            val coercedPos =
//                pos.coerceIn(0..currentPlaybackState.position.totalMills)
//            playbackState = currentPlaybackState.copy(
//                position = currentPlaybackState.position.copy(currentMillis = coercedPos)
//            )
//            playerWrapper.player.seekTo(coercedPos)
//        }
//        dispatchEvent(PlaybackEvents.SeekByUser)
//        dispatchEvent(PlaybackEvents.PlayingPositionChanged)
//        //TODO: should we handle moving to next song here?
//    }
//
//    private fun onPlayCommand() {
//        val currentPlaybackState = playbackState
//        if (currentPlaybackState is PlaybackState.Ongoing) {
//            val playerItemUri = playerWrapper.player.currentMediaItem?.requestMetadata?.mediaUri
//            val stateItemUri = songIdToUri(currentPlaybackState.mediaItem.id)
//            if (stateItemUri != playerItemUri) {
//                playerWrapper.setMediaItemUriAndPrepare(
//                    uri = stateItemUri,
//                    onPrepared = {
//                        playerWrapper.setVolumeGraduallyTo(
//                            startingVolume = 0f,
//                            to = 1f,
//                            fadeDuration = playbackParamsState.playFadeInDurationMillis
//                        )
//                        val state = playbackState
//                        if(state is PlaybackState.Ongoing){
//                            playbackState = state.copy(position = state.position.copy(totalMills = playerWrapper.player.contentDuration))
//                        }
//                        playerWrapper.player.play()
//                        dispatchEvent(PlaybackEvents.PlayingSongChanged)
//                    })
//            } else {
//                playerWrapper.setVolumeGraduallyTo(
//                    startingVolume = 0f,
//                    to = 1f,
//                    fadeDuration = playbackParamsState.playFadeInDurationMillis,
//                )
//                playerWrapper.player.play()
//            }
//            playbackState = currentPlaybackState.copy(isPlaying = true)
//            println("dispatch PlaybackEvents.ResumePlaying")
//            dispatchEvent(PlaybackEvents.ResumePlaying)
//        }
//
//    }
//
//    private fun onPauseCommand() {
//        val currentPlaybackState = playbackState
//        if (currentPlaybackState is PlaybackState.Ongoing) {
//            playerWrapper.setVolumeGraduallyTo(
//                to = 0f,
//                fadeDuration = playbackParamsState.pauseFadeOutDurationMillis,
//                onFadeComplete = { playerWrapper.player.pause() }
//            )
//            playbackState = currentPlaybackState.copy(isPlaying = false)
//            dispatchEvent(PlaybackEvents.PausePlaying)
//        }
//    }
//
//
//    private fun onRemoveAtIndexes(indexesToRemove: List<Int>) {
//        val currQueueState = playbackStateX
//        if (currQueueState is PlaybackStateX.NonEmpty) {
//            val songIdsToRemove = indexesToRemove
//                .filter { it in 0 until currQueueState.queue.size }
//                .map { currQueueState.queue[it] }
//                .toSet()
//            val currentSongId = currQueueState.queue[currQueueState.currentItemIndex]
//            if (songIdsToRemove.contains(currentSongId)) {
//                val nextSongToSet =
//                    (currQueueState.currentItemIndex + 1 until currQueueState.queue.size).firstOrNull {
//                        !songIdsToRemove.contains(currQueueState.queue[it])
//                    } ?: (currQueueState.currentItemIndex - 1 downTo 0).firstOrNull {
//                        !songIdsToRemove.contains(currQueueState.queue[it])
//                    }
//                if (nextSongToSet == null) {
//                    onClearQueue()
//                } else {
//                    val nextSongId = currQueueState.queue[nextSongToSet]
//                    val newQueue = currQueueState.queue.toMutableList()
//                    newQueue.removeIf { songIdsToRemove.contains(it) }
//                    playbackStateX = PlaybackStateX.NonEmpty(
//                        queue = newQueue,
//                        currentItemIndex = newQueue.indexOf(nextSongId)
//                    )
//                    transitionToSongId(nextSongId)
//                }
//            } else {
//                val newQueue = currQueueState.queue.toMutableList()
//                newQueue.removeIf { songIdsToRemove.contains(it) }
//                playbackStateX = PlaybackStateX.NonEmpty(
//                    queue = newQueue,
//                    currentItemIndex = newQueue.indexOf(currentSongId)
//                )
//            }
//        }
//    }
//
//    private fun onClearQueue() {
//        val currentPlaybackState = playbackState
//        if (currentPlaybackState is PlaybackState.Ongoing && currentPlaybackState.isPlaying) {
//            onPauseCommand()
//        }
//        playbackStateX = PlaybackStateX.Empty
//        playbackState = PlaybackState.Stopped
//        println("dispatch(PlayingSongChanged) from clear queue")
//        dispatchEvent(PlaybackEvents.PlayingSongChanged)
//    }
//
//    private fun onAddItemsToQueue(items: List<Long>, atIndex: Int?) {
//        val currentQueueState = playbackStateX
//        when {
//            items.isEmpty() -> {
//                //do nothing
//            }
//
//            currentQueueState == PlaybackStateX.Empty -> {
//                val firstItemIndex = items.indices.random()
//                playbackStateX = PlaybackStateX.NonEmpty(items, firstItemIndex)
//                playbackState = PlaybackState.Ongoing(
//                    mediaItem = MediaItem(items[firstItemIndex]),
//                    position = PlaybackTimeline(
//                        currentMillis = 0,
//                        totalMills = 0
//                    ),
//                    isPlaying = false
//                )
//                println("dispatch(PlayingSongChanged) from add items to queue")
//                dispatchEvent(PlaybackEvents.PlayingSongChanged)
//            }
//
//            atIndex == null && currentQueueState is PlaybackStateX.NonEmpty -> {
//                playbackStateX = currentQueueState.copy(
//                    queue = currentQueueState.queue + items
//                )
//            }
//
//            atIndex != null && currentQueueState is PlaybackStateX.NonEmpty -> {
//                if (atIndex >= currentQueueState.queue.size) {
//                    playbackStateX = currentQueueState.copy(
//                        queue = currentQueueState.queue + items
//                    )
//                } else if (atIndex > currentQueueState.currentItemIndex) {
//                    playbackStateX = currentQueueState.copy(
//                        queue = currentQueueState.queue.toMutableList().apply {
//                            addAll(atIndex, items)
//                        }
//                    )
//                } else {
//                    val coercedAtIndex = atIndex.coerceAtLeast(0)
//                    playbackStateX = currentQueueState.copy(
//                        queue = currentQueueState.queue.toMutableList().apply {
//                            addAll(coercedAtIndex, items)
//                        },
//                        currentItemIndex = currentQueueState.currentItemIndex + items.size
//                    )
//                }
//            }
//
//        }
//    }
//
//
//    /**
//     * Just player thing, no change in state
//     */
//    private fun transitionToSongId(songId: Long) {
//        val params = playbackParamsState
//        when (params.songTransitionType) {
//            is SongTransitionType.CrossFade -> {
//                val (fadeInMills, fadeOutMillis) = params.songTransitionType
//                val thisPlayer = playerWrapper
//
//                val nextPlayer = MediaPlayerX(context, onPosUpdate = posUpdate)
//                thisPlayer.removeListener()
//
//                playerWrapper = nextPlayer
//
//                run stopCurrentPlayback@{
//                    thisPlayer.setVolumeGraduallyTo(to = 0f,
//                        fadeDuration = fadeOutMillis,
//                        onFadeComplete = { thisPlayer.release() })
//                }
//                nextPlayer.setMediaItemUriAndPrepare(
//                    uri = songIdToUri(songId),
//                    onPrepared = {
//                        thisPlayer.cancelPosUpdate()
//                        nextPlayer.setVolumeGraduallyTo(
//                            startingVolume = 0f,
//                            to = 1f,
//                            fadeDuration = fadeInMills
//                        )
//                        nextPlayer.player.play()
//                        val state = playbackState
//                        if(state is PlaybackState.Ongoing){
//                            playbackState = state.copy(position = state.position.copy(totalMills = playerWrapper.player.contentDuration))
//                            dispatchEvent(PlaybackEvents.PlayingSongChanged)
//                        }
//                    })
//            }
//
//            SongTransitionType.Gapless -> {
//                //TODO: gapless impl
//                playerWrapper.setMediaItemUriAndPrepare(
//                    uri = songIdToUri(songId),
//                    onPrepared = {
//                        playerWrapper.player.play()
//
//                        val state = playbackState
//                        if(state is PlaybackState.Ongoing){
//                            playbackState = state.copy(position = state.position.copy(totalMills = playerWrapper.player.contentDuration))
//                            dispatchEvent(PlaybackEvents.PlayingSongChanged)
//                        }
//                    })
//            }
//
//            SongTransitionType.Simple -> {
//                playerWrapper.setMediaItemUriAndPrepare(
//                    uri = songIdToUri(songId),
//                    onPrepared = {
//                        playerWrapper.player.play()
//
//                        val state = playbackState
//                        if(state is PlaybackState.Ongoing){
//                            playbackState = state.copy(position = state.position.copy(totalMills = playerWrapper.player.contentDuration))
//                            dispatchEvent(PlaybackEvents.PlayingSongChanged)
//                        }
//                    })
//            }
//        }
//    }
//}
//
//
//private fun songIdToUri(songId: Long) = ContentUris.withAppendedId(
//    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
//    songId
//)
//
