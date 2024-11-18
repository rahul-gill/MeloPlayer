package meloplayer.app

import app.cash.sqldelight.ColumnAdapter
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import io.requery.android.database.sqlite.RequerySQLiteOpenHelperFactory
import meloplayer.app.db.Albums
import meloplayer.app.db.MeloDatabase
import meloplayer.app.db.Songs
import meloplayer.app.parts.songs.list.SongRepo
import meloplayer.app.parts.songs.list.SongRepoImpl
import meloplayer.app.db.MeloDB
import meloplayer.app.db.SyncManager
import meloplayer.app.db.dao.AlbumsDao
import meloplayer.app.db.dao.ArtistsDao
import meloplayer.app.db.dao.GenresDao
import meloplayer.app.db.dao.SongsDao
import meloplayer.app.parts.albums.list.AlbumListViewModel
import meloplayer.app.parts.artists.list.ArtistListViewModel
import meloplayer.app.parts.genres.list.GenresListViewModel
import meloplayer.app.parts.playlists.list.PlaylistsScreenViewModel
import meloplayer.app.parts.songs.list.SongListViewModel
import meloplayer.app.parts.tags.list.TagsListVIewModel
import meloplayer.core.startup.applicationContextGlobal
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import java.time.Instant


val appModule = module {
    //viewmodels
    viewModel<SongListViewModel> { SongListViewModel(get()) }
    viewModel<AlbumListViewModel> { AlbumListViewModel(get()) }
    viewModel<GenresListViewModel> { GenresListViewModel(get()) }
    viewModel<ArtistListViewModel> { ArtistListViewModel(get()) }
    viewModel<PlaylistsScreenViewModel> { PlaylistsScreenViewModel() }
    viewModel<TagsListVIewModel> { TagsListVIewModel() }



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
    //daos
    single<SongsDao> {
        get<MeloDB>().songsDao()
    }
    single<AlbumsDao> {
        get<MeloDB>().albumsDao()
    }
    single<ArtistsDao> {
        get<MeloDB>().artistsDao()
    }
    single<GenresDao> {
        get<MeloDB>().genresDao()
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