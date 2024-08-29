package meloplayer.app

import app.cash.sqldelight.ColumnAdapter
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import meloplayer.app.db.Albums
import meloplayer.app.db.MeloDatabase
import meloplayer.app.db.Songs
import meloplayer.app.store.repo.SongRepo
import meloplayer.app.store.repo.SongRepoImpl
import meloplayer.app.ui.screen.songs.SongListViewModel
import meloplayer.core.startup.applicationContextGlobal
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import java.time.Instant

val appModule = module {
    viewModel { SongListViewModel(get()) }
    single<SongRepo> { SongRepoImpl(get()) }
    single<MeloDatabase> {
        MeloDatabase(
            AndroidSqliteDriver(MeloDatabase.Schema, applicationContextGlobal, "melo-meta-database.db"),
            albumsAdapter = Albums.Adapter(InstantLongAdapter),
            songsAdapter = Songs.Adapter(InstantLongAdapter)
        )
    }

}

object InstantLongAdapter : ColumnAdapter<Instant, Long> {
    override fun decode(databaseValue: Long): Instant {
        return Instant.ofEpochMilli(databaseValue)
    }

    override fun encode(value: Instant): Long {
        return value.toEpochMilli()
    }
}