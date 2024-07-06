//package meloplayer.app.playback
//
//import kotlinx.coroutines.CoroutineScope
//import kotlinx.coroutines.Dispatchers
//import kotlinx.coroutines.Job
//import kotlinx.coroutines.cancel
//import kotlinx.coroutines.delay
//import kotlinx.coroutines.flow.first
//import kotlinx.coroutines.runBlocking
//import meloplayer.core.prefs.Preference
//import meloplayer.core.prefs.PreferenceMock
//import org.hamcrest.MatcherAssert.assertThat
//import org.junit.After
//import org.junit.Assert.assertEquals
//import org.junit.Assert.assertNotEquals
//import org.junit.Before
//import org.junit.Test
//
//class PlaybackQueueManagerTest {
//
//    private lateinit var scope: CoroutineScope
//    private lateinit var queueManager: PlaybackQueueManagerX

//    private lateinit var loopMode: Preference<LoopMode>
//    private lateinit var shuffleEnabled: Preference<Boolean>
//
//    @Before
//    fun setup() {
//        scope = CoroutineScope(Job())
//        loopMode = PreferenceMock(LoopMode.None)
//        shuffleEnabled = PreferenceMock(false)
//        queueManager =  PlaybackQueueManager.getImpl(
//            scope = scope,
//            loopMode = loopMode,
//            shuffleEnabled = shuffleEnabled,
//            onPlaybackQueueEvent = { _, _ -> }
//        )
//    }
//
//    @After
//    fun tearDown(){
//        scope.cancel()
//    }
//
//    @Test
//    fun testInitialState() {
//        //initial state
//        runBlocking {
//            assert(queueManager.currentQueue.first().isEmpty())
//            assert(queueManager.currentSongIndex.first() == null)
//        }
//
//    }
//
//    @Test
//    fun `shuffle mode change works correctly`(){
//        runBlocking {
//            // Add some songs
//            println("Before set true block")
//            shuffleEnabled.setValue(true); delay(10)
//            println("After set true block")
//            queueManager.addSongs(listOf(1L, 2L, 3L, 4L, 5L))
//            //when adding new songs, their order is not shuffled
//            assertEquals(queueManager.currentQueue.first(), listOf(1L, 2L, 3L, 4L, 5L))
//
//            // Disable shuffle
//            println("Before set false block")
//            shuffleEnabled.setValue(false); delay(10)
//            println("After set false block")
//
//            assertEquals(queueManager.currentQueue.first(), listOf(1L, 2L, 3L, 4L, 5L))
//
//            // Enable shuffle, when we re-enable shuffle, queue is shuffled
//            println("Before set true block")
//            shuffleEnabled.setValue(true); delay(10)
//            println("After set true block")
//            assertNotEquals(queueManager.currentQueue.first(), listOf(1L, 2L, 3L, 4L, 5L))
//
//            // Disable shuffle
//            println("Before set false block")
//            shuffleEnabled.setValue(false); delay(10)
//            println("After set false block")
//            assertEquals(queueManager.currentQueue.first(), listOf(1L, 2L, 3L, 4L, 5L))
//        }
//    }
//
//    @Test
//    fun `adding songs works correctly`(){
//        runBlocking {
//            queueManager.addSongs(listOf(1L, 2L, 3L))
//            assertEquals(queueManager.currentQueue.first(), listOf(1L, 2L, 3L))
//
//            queueManager.addSongs(listOf(4L, 5L), index = 1)
//            assertEquals(queueManager.currentQueue.first(), listOf(1L, 4L, 5L, 2L, 3L))
//
//            queueManager.addSongs(listOf(6L))
//            assertEquals(queueManager.currentQueue.first(), listOf(1L, 4L, 5L, 2L, 3L, 6L))
//        }
//    }
//
//    @Test
//    fun `removing songs works correctly`(){
//        runBlocking {
//            queueManager.addSongs(listOf(1L, 2L, 3L, 4L, 5L))
//
//            queueManager.removeAtIndex(2)
//            assertEquals(queueManager.currentQueue.first(), listOf(1L, 2L, 4L, 5L))
//
//            queueManager.removeAtIndex(listOf(0, 2))
//            assertEquals(queueManager.currentQueue.first(), listOf(2L, 5L))
//
//            queueManager.removeAtIndex(5)  // out of bounds
//            assertEquals(queueManager.currentQueue.first(), listOf(2L, 5L))
//        }
//    }
//
//    @Test
//    fun `resetting works correctly`(){
//        runBlocking {
//            queueManager.addSongs(listOf(1L, 2L, 3L))
//            queueManager.reset()
//            assert(queueManager.currentQueue.first().isEmpty())
//            assert(queueManager.currentSongIndex.first() == null)
//            assert(queueManager.currentItem.first() == null)
//        }
//    }
//
//    @Test
//    fun `moving songs works correctly`(){
//        runBlocking {
//            queueManager.addSongs(listOf(1L, 2L, 3L, 4L, 5L))
//
//            queueManager.moveTrack(fromIndex = 1, toIndex = 3)
//            assertEquals(queueManager.currentQueue.first(), listOf(1L, 3L, 4L, 2L, 5L))
//
//            queueManager.moveTrack(fromIndex = 0, toIndex = 4)
//            assertEquals(queueManager.currentQueue.first(), listOf(3L, 4L, 2L, 5L, 1L))
//
//            queueManager.moveTrack(fromIndex = 4, toIndex = 0)
//            assertEquals(queueManager.currentQueue.first(), listOf(1L, 3L, 4L, 2L, 5L))
//        }
//    }
//
//
//    @Test
//    fun `moving songs works correctly with current song index`(){
//        runBlocking {
//            queueManager.addSongs(listOf(1L, 2L, 3L, 4L, 5L)); delay(10)
//            println("currentSongIndex: ${queueManager.currentSongIndex.value}")
//            assertEquals(1L, queueManager.currentItem.first())
//
//            queueManager.moveTrack(fromIndex = 1, toIndex = 3)
//            assertEquals(queueManager.currentQueue.first(), listOf(1L, 3L, 4L, 2L, 5L))
//            assertEquals(1L, queueManager.currentItem.first())
//
//            queueManager.moveTrack(fromIndex = 0, toIndex = 4)
//            assertEquals(queueManager.currentQueue.first(), listOf(3L, 4L, 2L, 5L, 1L))
//            assertEquals(1L, queueManager.currentItem.first())
//
//            queueManager.moveTrack(fromIndex = 4, toIndex = 0)
//            assertEquals(queueManager.currentQueue.first(), listOf(1L, 3L, 4L, 2L, 5L))
//            assertEquals(1L, queueManager.currentItem.first())
//        }
//    }
//
//
//    @Test
//    fun `handling shuffle mode transition with current song`() {
//        runBlocking {
//            shuffleEnabled.setValue(false); delay(10)
//            queueManager.addSongs(listOf(1L, 2L, 3L, 4L, 5L))
//            queueManager.setCurrentSongIndex(2)  // current song is 3
//
//            shuffleEnabled.setValue(true); delay(10)
//            val shuffledQueue = queueManager.currentQueue.first()
//            println("shuffledQueue:$shuffledQueue currentIndex:${queueManager.currentSongIndex.value}")
//            assertNotEquals(shuffledQueue, listOf(1L, 2L, 3L, 4L, 5L))
//            assertEquals(queueManager.currentItem.first(), 3L)
//
//
//            shuffleEnabled.setValue(false); delay(10)
//            val restoredQueue = queueManager.currentQueue.first()
//            assertEquals(restoredQueue, listOf(1L, 2L, 3L, 4L, 5L))
//            assertEquals(queueManager.currentItem.first(), 3L)
//        }
//    }
//
//
//}