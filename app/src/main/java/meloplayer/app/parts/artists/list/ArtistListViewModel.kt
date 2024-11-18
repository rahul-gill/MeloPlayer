package meloplayer.app.parts.artists.list

import androidx.lifecycle.ViewModel
import meloplayer.app.db.dao.ArtistsDao

class ArtistListViewModel(
    artistsDao: ArtistsDao
): ViewModel() {

    val artists = artistsDao.getAllArtists()
}