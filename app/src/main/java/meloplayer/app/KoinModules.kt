package meloplayer.app

import app.cash.sqldelight.ColumnAdapter
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import io.requery.android.database.sqlite.RequerySQLiteOpenHelperFactory
import meloplayer.app.db.Albums
import meloplayer.app.db.MeloDatabase
import meloplayer.app.db.Songs
import meloplayer.app.store.repo.SongRepo
import meloplayer.app.store.repo.SongRepoImpl
import meloplayer.app.storex.MeloDB
import meloplayer.app.storex.SyncManager
import meloplayer.app.storex.dao.SongsDao
import meloplayer.app.ui.screen.songs.SongListViewModel
import meloplayer.core.startup.applicationContextGlobal
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import java.time.Instant


val appModule = module {
    viewModel<SongListViewModel> { SongListViewModel(get()) }
    single<SongRepo> { SongRepoImpl(get()) }
    single<MeloDatabase> {
        MeloDatabase(
            AndroidSqliteDriver(
                MeloDatabase.Schema,
                applicationContextGlobal,
                "melo-meta-database.db",
                RequerySQLiteOpenHelperFactory()),
            albumsAdapter = Albums.Adapter(InstantLongAdapter),
            songsAdapter = Songs.Adapter(InstantLongAdapter)
        )
    }

    single<SyncManager> {
        SyncManager(androidContext(), get())
    }
    single<MeloDB> {
        MeloDB.buildInstance(androidContext())
    }
    single<SongsDao> {
        get<MeloDB>().songsDao()
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