package meloplayer.app.ui.screen

import androidx.lifecycle.ViewModel
import meloplayer.app.store.db.MediaMetadataDB

class ArtistListViewModel(
    db: MediaMetadataDB
): ViewModel() {
    val artists = db.artistDao().getAllArtists()
}