package meloplayer.app.store.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Delete
import androidx.room.RawQuery
import androidx.room.Transaction
import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.sqlite.db.SupportSQLiteQuery
import kotlinx.coroutines.flow.Flow
import meloplayer.app.store.db.entities.Album
import meloplayer.app.store.db.entities.Artist
import meloplayer.app.store.db.entities.Genre
import meloplayer.app.store.db.entities.Song
import meloplayer.app.store.db.entities.SongArtist
import meloplayer.app.store.db.entities.SongGenre
import meloplayer.app.store.models.SongListItem

@Dao
interface SongDao {

    fun getAllSongsSummary(): Flow<List<SongListItem>> {
        val query = """
            WITH album_q AS (
                SELECT albums.title AS album_name, albums.albumId
                FROM albums
                UNION ALL
                SELECT 'EMPTY', 0
            ),
            artists_q AS (
                SELECT GROUP_CONCAT(artists.name, ', ') AS artists_name,
                       GROUP_CONCAT(artists.artistId, ', ') AS artistId,
                       songId
                FROM artists
                JOIN song_artists ON artists.artistId = song_artists.artistId
                WHERE artists.isSongArtist = 1
                GROUP BY songId
                UNION ALL
                SELECT 'EMPTY', 0, 0
            ),
            genre_q AS (
                SELECT GROUP_CONCAT(genres.name, ', ') AS genre_names, song_genres.songId
                FROM genres
                JOIN song_genres ON genres.genreId = song_genres.genreId
                GROUP BY song_genres.songId
                UNION ALL
                SELECT 'EMPTY', 0
            )
            SELECT s.songId,
                   s.title,
                   s.coverImageUri,
                   s.dateModified,
                   s.lengthMs,
                   s.fileSystemPath,
                   s.albumId,
                   COALESCE((SELECT album_name FROM album_q WHERE albumId = s.albumId OR albumId = 0 ORDER BY albumId DESC LIMIT 1), 'EMPTY') AS album,
                   COALESCE((SELECT artists_name FROM artists_q WHERE songId = s.songId OR songId = 0 ORDER BY songId DESC LIMIT 1), 'EMPTY') AS artists,
                   COALESCE((SELECT artistId FROM artists_q WHERE songId = s.songId OR songId = 0 ORDER BY songId DESC LIMIT 1), '0') AS artistsIds,
                   COALESCE((SELECT artists_name FROM artists_q WHERE songId = s.songId OR songId = 0 ORDER BY songId DESC LIMIT 1), 'EMPTY') AS albumArtists,
                   COALESCE((SELECT artistId FROM artists_q WHERE songId = s.songId OR songId = 0 ORDER BY songId DESC LIMIT 1), '0') AS albumArtistsIds,
                   COALESCE((SELECT genre_names FROM genre_q WHERE songId = s.songId OR songId = 0 ORDER BY songId DESC LIMIT 1), 'EMPTY') AS genres
            FROM songs s
        """.trimIndent()
        val simpleSQLiteQuery = SimpleSQLiteQuery(query, arrayOf<SongListItem>())
        return getSongsDetails(simpleSQLiteQuery)
    }

    @RawQuery(observedEntities = [
        Album::class,
        Song::class,
        Artist::class,
        Genre::class,
        SongArtist::class,
        SongGenre::class
    ])
    fun getSongsDetails(q: SupportSQLiteQuery): Flow<List<SongListItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSong(song: Song)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSongs(song: List<Song>)

    @Query("SELECT * FROM songs WHERE songId = :songId")
    suspend fun getSongById(songId: Long): Song?

    @Query("SELECT * FROM songs WHERE fileSystemPath = :filePath")
    suspend fun getSongByFilePath(filePath: String): Song?

    @Query("SELECT * FROM songs WHERE albumId = :albumId")
    suspend fun getSongsByAlbumId(albumId: Long): List<Song>

    @Query("SELECT * FROM songs WHERE songId IN (SELECT songId FROM song_artists WHERE artistId = :artistId)")
    suspend fun getSongsByArtistId(artistId: Long): List<Song>

    @Query("SELECT * FROM songs WHERE songId IN (SELECT songId FROM song_genres WHERE genreId = :genreId)")
    suspend fun getSongsByGenreId(genreId: Long): List<Song>

    @Query("SELECT * FROM songs")
    suspend fun getAllSongs(): List<Song>

    @Delete
    suspend fun deleteSongs(song: List<Song>)
    @Delete
    suspend fun deleteSong(song: Song)
}
