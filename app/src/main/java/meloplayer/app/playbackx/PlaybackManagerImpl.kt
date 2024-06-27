package meloplayer.app.playbackx

import android.content.ContentUris
import android.content.Context
import android.media.MediaMetadataRetriever
import android.provider.MediaStore
import androidx.media3.common.PlaybackParameters
import androidx.media3.exoplayer.source.MediaSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.job
import kotlinx.coroutines.plus
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class PlaybackManagerImpl(
    private val context: Context
) : PlaybackManagerX, EventSourceImpl<PlaybackEvents>() {
    private val mutex = Mutex()
    private lateinit var scope: CoroutineScope
    private var playerWrapper = MediaPlayerX(context)


    override var playbackState: PlaybackState = PlaybackState.Stopped
    override var playbackParamsState: PlaybackParamsState = PlaybackParamsState()
    override var playbackQueueState: PlaybackQueueState = PlaybackQueueState.Empty


    override fun startWithRestore(parentScope: CoroutineScope) {
        scope = parentScope + SupervisorJob(parentScope.coroutineContext.job) + Dispatchers.Main
        //TODO: restore queue list, queue current item index, playback position millis
    }

    override fun release() {
        if (::scope.isInitialized) {
            scope.cancel()
        }
    }


    override suspend fun handleCommand(command: PlaybackCommand): Unit = mutex.withLock {
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
                val currentQueueState = playbackQueueState
                val (fromIndex, toIndex) = command
                if (currentQueueState is PlaybackQueueState.NonEmpty
                    && fromIndex in 0 until currentQueueState.queue.size
                    && toIndex in 0 until currentQueueState.queue.size
                ) {
                    val queueNew = currentQueueState.queue.toMutableList()
                    val track = queueNew.removeAt(fromIndex)
                    queueNew.add(toIndex, track)
                    val newCurrentIndex = when (val prev = currentQueueState.currentItemIndex) {
                        fromIndex -> toIndex
                        in (fromIndex + 1)..toIndex -> prev - 1
                        in toIndex until fromIndex -> prev + 1
                        else -> prev
                    }
                    playbackQueueState = PlaybackQueueState.NonEmpty(
                        queue = queueNew,
                        currentItemIndex = newCurrentIndex
                    )
                }
            }

            PlaybackCommand.Pause -> {
                onPauseCommand()
            }

            PlaybackCommand.Play -> {
                onPlayCommand()
            }

            is PlaybackCommand.RemoveAtIndexes -> {
                val indexesToRemove = command.indexes
                onRemoveAtIndexes(indexesToRemove)
            }

            is PlaybackCommand.SetCurrentQueueItemIndex -> {
                val currentQueueState = playbackQueueState
                val currentPlaybackState = playbackState
                val indexToSet = command.index
                if (currentQueueState is PlaybackQueueState.NonEmpty
                    && currentPlaybackState is PlaybackState.Ongoing
                    && indexToSet in 0 until currentQueueState.queue.size
                ) {
                    val toSetUri = currentQueueState.queue[indexToSet]
                    playbackState = PlaybackState.Ongoing(
                        mediaItem = MediaItem(toSetUri),
                        position = PlaybackTimeline(
                            currentMillis = 0,
                            totalMills = MediaMetadataRetriever().run {
                                setDataSource(
                                    context,
                                    songIdToUri(toSetUri)
                                )
                                extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)!!.toLong()
                            }
                        ),
                        isPlaying = currentPlaybackState.isPlaying
                    )
                    if (!currentPlaybackState.isPlaying) {
                        transitionToSongId(currentQueueState.queue[indexToSet])
                    } else {
                        playerWrapper.setMediaItemUriAndPrepare(uri = songIdToUri(toSetUri))
                    }
                }
            }

            is PlaybackCommand.SetPauseFadeOutDuration -> {
                playbackParamsState = playbackParamsState.copy(
                    pauseFadeOutDurationMillis = command.duration
                )
            }

            is PlaybackCommand.SetPitch -> {
                playbackParamsState = playbackParamsState.copy(
                    pitch = command.newPitch
                )
                playerWrapper.player.playbackParameters =
                    playerWrapper.player.playbackParameters.withSpeed(command.newPitch)
            }

            is PlaybackCommand.SetPlayFadeInDuration -> {
                playbackParamsState = playbackParamsState.copy(
                    playFadeInDurationMillis = command.duration
                )
            }

            is PlaybackCommand.SetPosition -> {
                onSetPositionCommand(command.pos)
            }

            is PlaybackCommand.SetRepeatMode -> {
                playbackParamsState = playbackParamsState.copy(
                    repeatMode = command.mode
                )
            }

            is PlaybackCommand.SetShuffleMode -> {
                playbackParamsState = playbackParamsState.copy(
                    shuffleModeOn = command.on
                )
            }

            is PlaybackCommand.SetSongTransitionType -> {
                playbackParamsState = playbackParamsState.copy(
                    songTransitionType = command.type
                )
            }

            is PlaybackCommand.SetSpeed -> {
                playbackParamsState = playbackParamsState.copy(
                    speed = command.newSpeed
                )
                playerWrapper.player.playbackParameters =
                    PlaybackParameters(
                        playerWrapper.player.playbackParameters.speed,
                        command.newSpeed
                    )

            }

            PlaybackCommand.SkipNext -> {
                val currentQueueState = playbackQueueState
                if (currentQueueState is PlaybackQueueState.NonEmpty) {
                    val nextSongIndex = when {
                        playbackParamsState.shuffleModeOn -> {
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
                        transitionToSongId(currentQueueState.queue[nextSongIndex])
                    } else {
                        onPauseCommand()
                    }
                }
            }

            PlaybackCommand.SkipPrevious -> {
                val currentQueue = playbackQueueState
                val currentPlayback = playbackState
                val durationToSkipPreviousMillis =
                    playbackParamsState.durationToSkipPreviousSongMillis
                if (currentPlayback is PlaybackState.Ongoing && currentQueue is PlaybackQueueState.NonEmpty) {
                    if (currentPlayback.position.currentMillis > durationToSkipPreviousMillis) {
                        onSetPositionCommand(0)
                    } else {
                        val songToPlayIndex = when {
                            playbackParamsState.shuffleModeOn -> {
                                //TODO: we gotta remember previously played songs
                                0
                            }

                            currentQueue.currentItemIndex > 0 -> {
                                currentQueue.currentItemIndex - 1
                            }

                            playbackParamsState.repeatMode == RepeatMode.All -> {
                                currentQueue.queue.size - 1
                            }

                            else -> 0
                        }
                        transitionToSongId(currentQueue.queue[songToPlayIndex])
                    }
                }
            }

            PlaybackCommand.Stop -> {
                //TODO: maybe remove this command
            }
        }
    }

    private fun onSetPositionCommand(pos: Long) {
        val currentPlaybackState = playbackState
        if (currentPlaybackState is PlaybackState.Ongoing) {
            val coercedPos =
                pos.coerceIn(0..currentPlaybackState.position.totalMills)
            playbackState = currentPlaybackState.copy(
                position = currentPlaybackState.position.copy(currentMillis = coercedPos)
            )
            playerWrapper.player.seekTo(coercedPos)
        }
        //TODO: should we handle moving to next song here?
    }

    private fun onPlayCommand() {
        val currentPlaybackState = playbackState
        if (currentPlaybackState is PlaybackState.Ongoing) {
            val playerItemUri = playerWrapper.player.currentMediaItem?.requestMetadata?.mediaUri
            val stateItemUri = songIdToUri(currentPlaybackState.mediaItem.id)
            if (stateItemUri != playerItemUri) {
                playerWrapper.setMediaItemUriAndPrepare(
                    uri = stateItemUri,
                    onPrepared = {
                        playerWrapper.setVolumeGraduallyTo(
                            startingVolume = 0f,
                            to = 1f,
                            fadeDuration = playbackParamsState.playFadeInDurationMillis
                        )
                        playerWrapper.player.play()
                    })
            } else {
                playerWrapper.setVolumeGraduallyTo(
                    startingVolume = 0f,
                    to = 1f,
                    fadeDuration = playbackParamsState.playFadeInDurationMillis,
                )
                playerWrapper.player.play()
            }
            playbackState = currentPlaybackState.copy(isPlaying = true)
        }
    }

    private fun onPauseCommand() {
        val currentPlaybackState = playbackState
        if (currentPlaybackState is PlaybackState.Ongoing) {
            playerWrapper.setVolumeGraduallyTo(
                to = 0f,
                fadeDuration = playbackParamsState.pauseFadeOutDurationMillis,
                onFadeComplete = { playerWrapper.player.pause() }
            )
            playbackState = currentPlaybackState.copy(isPlaying = false)
        }
    }


    private fun onRemoveAtIndexes(indexesToRemove: List<Int>) {
        val currQueueState = playbackQueueState
        if (currQueueState is PlaybackQueueState.NonEmpty) {
            val songIdsToRemove = indexesToRemove
                .filter { it in 0 until currQueueState.queue.size }
                .map { currQueueState.queue[it] }
                .toSet()
            val currentSongId = currQueueState.queue[currQueueState.currentItemIndex]
            if (songIdsToRemove.contains(currentSongId)) {
                val nextSongToSet =
                    (currQueueState.currentItemIndex + 1 until currQueueState.queue.size).firstOrNull {
                        !songIdsToRemove.contains(currQueueState.queue[it])
                    } ?: (currQueueState.currentItemIndex - 1 downTo 0).firstOrNull {
                        !songIdsToRemove.contains(currQueueState.queue[it])
                    }
                if (nextSongToSet == null) {
                    onClearQueue()
                } else {
                    val nextSongId = currQueueState.queue[nextSongToSet]
                    val newQueue = currQueueState.queue.toMutableList()
                    newQueue.removeIf { songIdsToRemove.contains(it) }
                    playbackQueueState = PlaybackQueueState.NonEmpty(
                        queue = newQueue,
                        currentItemIndex = newQueue.indexOf(nextSongId)
                    )
                    transitionToSongId(nextSongId)
                }
            } else {
                val newQueue = currQueueState.queue.toMutableList()
                newQueue.removeIf { songIdsToRemove.contains(it) }
                playbackQueueState = PlaybackQueueState.NonEmpty(
                    queue = newQueue,
                    currentItemIndex = newQueue.indexOf(currentSongId)
                )
            }
        }
    }

    private fun onClearQueue() {
        val currentPlaybackState = playbackState
        if (currentPlaybackState is PlaybackState.Ongoing && currentPlaybackState.isPlaying) {
            onPauseCommand()
        }
        playbackQueueState = PlaybackQueueState.Empty
        playbackState = PlaybackState.Stopped
    }

    private fun onAddItemsToQueue(items: List<Long>, atIndex: Int?) {
        val currentQueueState = playbackQueueState
        when {
            items.isEmpty() -> {
                //do nothing
            }

            currentQueueState == PlaybackQueueState.Empty -> {
                val firstItemIndex = items.indices.random()
                playbackQueueState = PlaybackQueueState.NonEmpty(items, firstItemIndex)
                playbackState = PlaybackState.Ongoing(
                    mediaItem = MediaItem(items[firstItemIndex]),
                    position = PlaybackTimeline(
                        currentMillis = 0,
                        totalMills = MediaMetadataRetriever().run {
                            setDataSource(
                                context,
                                songIdToUri(items[firstItemIndex])
                            )
                            extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)!!.toLong()
                        }
                    ),
                    isPlaying = false
                )
            }

            atIndex == null && currentQueueState is PlaybackQueueState.NonEmpty -> {
                playbackQueueState = currentQueueState.copy(
                    queue = currentQueueState.queue + items
                )
            }

            atIndex != null && currentQueueState is PlaybackQueueState.NonEmpty -> {
                if (atIndex >= currentQueueState.queue.size) {
                    playbackQueueState = currentQueueState.copy(
                        queue = currentQueueState.queue + items
                    )
                } else if (atIndex > currentQueueState.currentItemIndex) {
                    playbackQueueState = currentQueueState.copy(
                        queue = currentQueueState.queue.toMutableList().apply {
                            addAll(atIndex, items)
                        }
                    )
                } else {
                    val coercedAtIndex = atIndex.coerceAtLeast(0)
                    playbackQueueState = currentQueueState.copy(
                        queue = currentQueueState.queue.toMutableList().apply {
                            addAll(coercedAtIndex, items)
                        },
                        currentItemIndex = currentQueueState.currentItemIndex + items.size
                    )
                }
            }

        }
    }


    /**
     * Just player thing, no change in state
     */
    private fun transitionToSongId(songId: Long) {
        val params = playbackParamsState
        when (params.songTransitionType) {
            is SongTransitionType.CrossFade -> {
                val (fadeInMills, fadeOutMillis) = params.songTransitionType
                val thisPlayer = playerWrapper

                val nextPlayer = MediaPlayerX(context)
                thisPlayer.removeListener()

                playerWrapper = nextPlayer

                run stopCurrentPlayback@{
                    thisPlayer.setVolumeGraduallyTo(to = 0f,
                        fadeDuration = fadeOutMillis,
                        onFadeComplete = { thisPlayer.player.release() })
                }
                nextPlayer.setMediaItemUriAndPrepare(
                    uri = songIdToUri(songId),
                    onPrepared = {
                        nextPlayer.setVolumeGraduallyTo(
                            startingVolume = 0f,
                            to = 1f,
                            fadeDuration = fadeInMills
                        )
                        nextPlayer.player.play()
                    })
            }

            SongTransitionType.Gapless -> {
                //TODO: gapless impl
                playerWrapper.setMediaItemUriAndPrepare(
                    uri = songIdToUri(songId),
                    onPrepared = {
                        playerWrapper.player.play()
                    })
            }

            SongTransitionType.Simple -> {
                playerWrapper.setMediaItemUriAndPrepare(
                    uri = songIdToUri(songId),
                    onPrepared = {
                        playerWrapper.player.play()
                    })
            }
        }
    }
}


private fun songIdToUri(songId: Long) = ContentUris.withAppendedId(
    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
    songId
)

