package meloplayer.app.store.db

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import android.content.Context
import meloplayer.app.store.db.entities.Album
import meloplayer.app.store.db.entities.Song
import meloplayer.app.store.db.entities.Artist
import meloplayer.app.store.db.entities.Genre
import meloplayer.app.store.db.entities.SongArtist
import meloplayer.app.store.db.entities.SongGenre

@Database(
    entities = [
        Album::class,
        Song::class,
        Artist::class,
        Genre::class,
        SongArtist::class,
        SongGenre::class
    ],
    version = 1,
    exportSchema = true
)
abstract class MediaMetadataDB : RoomDatabase() {

    abstract fun albumDao(): AlbumDao
    abstract fun songDao(): SongDao
    abstract fun artistDao(): ArtistDao
    abstract fun genreDao(): GenreDao
    abstract fun relationsDao(): RelationsDao

    companion object {
        private const val DATABASE_NAME = "media_metadata"
        fun buildInstance(context: Context): MediaMetadataDB {
            return Room.databaseBuilder(
                context.applicationContext,
                MediaMetadataDB::class.java,
                DATABASE_NAME
            ).build()
        }
    }
}
