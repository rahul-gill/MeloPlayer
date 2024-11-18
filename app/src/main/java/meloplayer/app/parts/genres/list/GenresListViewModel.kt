package meloplayer.app.parts.genres.list

import androidx.lifecycle.ViewModel
import meloplayer.app.db.dao.GenresDao

class GenresListViewModel(
    val genresDao: GenresDao
) : ViewModel() {
    val genres = genresDao.getAllGenres()
}