package meloplayer.app.storex

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import meloplayer.app.storex.dao.SongsDao
import meloplayer.app.storex.dao.SyncDao
import meloplayer.app.storex.entities.Album
import meloplayer.app.storex.entities.Artist
import meloplayer.app.storex.entities.Genre
import meloplayer.app.storex.entities.Song
import meloplayer.app.storex.entities.SongArtist
import meloplayer.app.storex.entities.SongGenre
import meloplayer.app.storex.entities.SongTag
import meloplayer.app.storex.entities.Tag


@Database(
    entities = [
        Album::class, Song::class, Artist::class, Genre::class,
        Tag::class, SongArtist::class, SongGenre::class, SongTag::class],
    version = 1
)
abstract class MeloDB : RoomDatabase() {
    abstract fun songsDao(): SongsDao
    abstract fun syncDao(): SyncDao

    companion object {
        fun buildInstance(context: Context) = Room.databaseBuilder(
            context.applicationContext,
            MeloDB::class.java, "music-database"
        ).build()
    }
}