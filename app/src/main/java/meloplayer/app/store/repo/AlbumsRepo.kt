package meloplayer.app.store.repo

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import meloplayer.app.db.MeloDatabase
import meloplayer.app.store.models.AlbumListItem
import meloplayer.app.store.models.SongFilter
import meloplayer.app.store.models.SongListItem

interface AlbumsRepo {
    //if some album has no song that matches that song filter, then it won't be included in list
    fun getAlbums(
        songFilter: List<SongFilter>
    ): Flow<List<AlbumListItem>>
}

//class AlbumsRepoImpl(
//    private val db: MeloDatabase,
//    private val songRepo: SongRepo
//): AlbumsRepo {
//    override fun getAlbums(songFilter: List<SongFilter>): Flow<List<AlbumListItem>> {



//        return db.schemaQueries.albumList(mapper = ::AlbumListItem)
//            .asFlow().mapToList(Dispatchers.IO)
//    }
//}