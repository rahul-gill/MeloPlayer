package meloplayer.app.ui.screen

import androidx.lifecycle.ViewModel
import meloplayer.app.store.db.MediaMetadataDB

class AlbumListViewModel(
    db: MediaMetadataDB
): ViewModel() {
    val albumsList = db.albumDao().getAllAlbums()
}