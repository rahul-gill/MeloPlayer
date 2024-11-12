package meloplayer.app.store.db

import androidx.room.Dao
import androidx.room.Query

@Dao
interface ExportDao {

    @Query(
        """
            SELECT songId||','|| title||','|| fileSystemPath||','|| lengthMs||','|| bitRateKbps||','|| sampleRateHz||','
|| channelsCount||','|| trackNumber||','|| cdNumber||','|| albums.title 
            FROM songs, albums WHERE songs.albumId = albums.albumId
        """
    )
    fun songsLines(): List<String>

//    @Query()
//    fun songsLines(): List<String>
//    @Query()
//    fun songsLines(): List<String>
//    @Query()
//    fun songsLines(): List<String>
//
//    @Query()
//    fun songsLines(): List<String>
//    @Query()
//    fun songsLines(): List<String>
}