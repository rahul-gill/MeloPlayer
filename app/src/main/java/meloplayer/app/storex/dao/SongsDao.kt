package meloplayer.app.storex.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow
import meloplayer.app.storex.entities.derived.SongDetailsMinimal
import meloplayer.app.storex.entities.derived.SongWithAlbumAndArtists
import meloplayer.app.storex.entities.derived.SongWithAllDetails

@Dao
interface SongsDao {

    @Query("""
    select s.song_id AS songId,
        s.title as songName,
        s.length_ms as lengthMs,
        a.title AS albumName,
        group_concat(ar.name , ' | ') AS artistNames,
        group_concat(ar_b.name , ' | ') AS albumArtistNames,
        group_concat(g.name , ' | ') AS genres
    from songs AS s left join albums AS a ON  s.album_id = a.album_id 
        left join song_artists s_ar ON s_ar.song_id = s.song_id
        left join artists ar ON s_ar.artist_id = ar.artist_id AND ar.is_song_artist = 1

        left join song_artists s_ar_b ON s_ar_b.song_id = s.song_id
        left join artists ar_b ON s_ar_b.artist_id = ar_b.artist_id AND ar_b.is_album_artist = 1
        
        left join song_genres s_g ON s_g.song_id = s.song_id
        left join genres g ON s_g.genre_id = g.genre_id
        group by s.song_id
    """)
    fun getSongDetailsMinimal(): Flow<List<SongDetailsMinimal>>


    @Transaction
    @Query("SELECT * FROM songs")
    fun getSongsWithAlbumAndArtists(): List<SongWithAlbumAndArtists>


    @Transaction
    @Query("SELECT * FROM songs")
    fun getSongWithAllDetails(): Flow<List<SongWithAllDetails>>

    @Transaction
    @Query("SELECT * FROM songs WHERE song_id = :id")
    fun getSongWithAllDetailsOne(id: Long): Flow<SongWithAllDetails?>
}