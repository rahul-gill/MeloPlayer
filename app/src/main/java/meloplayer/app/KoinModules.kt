package meloplayer.app

import android.content.Context
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.room.Room
import app.cash.sqldelight.ColumnAdapter
import meloplayer.app.store.LibrarySyncManger
import meloplayer.app.store.db.MediaMetadataDB
import meloplayer.app.store.repo.SongRepo
import meloplayer.app.store.repo.SongRepoImpl
import meloplayer.app.ui.screen.AlbumListViewModel
import meloplayer.app.ui.screen.ArtistListViewModel
import meloplayer.app.ui.screen.songs.SongListViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import java.time.Instant
import kotlin.math.sin

val appModule = module {
    single<MediaMetadataDB> {
        MediaMetadataDB.buildInstance(androidContext())
    }

    factory<LibrarySyncManger> {
        LibrarySyncManger(androidContext(), get())
    }

    viewModel {
        SongListViewModel(get())
    }
    viewModel {
        ArtistListViewModel(get())
    }
    viewModel {
        AlbumListViewModel(get())
    }

    single<SongRepo> {
        SongRepoImpl(get())
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