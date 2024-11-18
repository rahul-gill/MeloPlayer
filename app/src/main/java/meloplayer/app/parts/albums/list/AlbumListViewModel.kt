package meloplayer.app.parts.albums.list

import androidx.lifecycle.ViewModel
import meloplayer.app.db.dao.AlbumsDao

class AlbumListViewModel(
    private val dao: AlbumsDao
): ViewModel() {

    val albumsList = dao.getAllAlbums()
}