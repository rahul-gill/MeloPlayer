package meloplayer.app.playback

import org.junit.Test

class MeloPlayerTest {
    @Test
    fun t(){
        var result: List<String>? = listOf<String>()
        println("when empty: " + (result?.isEmpty() != false))
        result = null

        println("when null: " + (result?.isEmpty() != false))
        result = listOf("dsada")
        println("when not empty: " + (result.isEmpty() != false))
    }
}