package meloplayer.app

import org.junit.Test
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.test.KoinTest
import org.koin.test.verify.verify

class CheckKoinModulesTest : KoinTest {

    @Test
    fun checkAllModules() {
        appModule.verify()
    }
}