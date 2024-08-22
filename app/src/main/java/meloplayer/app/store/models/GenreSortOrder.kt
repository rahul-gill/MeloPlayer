

package meloplayer.app.store.models

sealed class GenreSortOrder(val isAscending: Boolean = true) {
    class SongCount(isAscending: Boolean = true) : GenreSortOrder(isAscending)
    class Name(isAscending: Boolean = true) : GenreSortOrder(isAscending)
}