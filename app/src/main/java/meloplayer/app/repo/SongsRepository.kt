package meloplayer.app.repo

import meloplayer.app.db.entities.Song
import java.lang.RuntimeException


interface SongsRepository {
    fun songs(): Result<List<Song>>
    fun songs(query: String): Result<List<Song>>
    fun songsByFilePathRecursive(
        filePath: String,
        ignoreBlackLists: Boolean = true
    ): Result<List<Song>>

    fun songById(id: Long): Result<Song>


}