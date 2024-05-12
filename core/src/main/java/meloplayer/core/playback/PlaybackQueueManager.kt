package meloplayer.core.playback

import android.content.Context


enum class LoopMode {
    None, One, All
}

class PlaybackQueueManager(
    private val context: Context
) {
    val originalQueue = mutableListOf<Long>()
    val currentQueue = mutableListOf<Long>()

    //current position index
    //current shuffle mode
    //current loop mode
    //songAt(index)
    //reset
    //add(single) add(multiple)
    //remove(single) remove(multiple)
    //toggle loop mode, toggle shuffle mode
    //save(serialize) and restore(deserialize)
}