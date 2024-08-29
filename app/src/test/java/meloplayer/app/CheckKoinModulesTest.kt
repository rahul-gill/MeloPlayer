package meloplayer.app

import org.junit.Test
import org.koin.test.KoinTest
import org.koin.test.verify.verify

class CheckKoinModulesTest : KoinTest {

    @Test
    fun checkAllModules() {
        appModule.verify()
    }
}