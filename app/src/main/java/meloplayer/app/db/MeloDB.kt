package meloplayer.app.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import meloplayer.app.db.dao.AlbumsDao
import meloplayer.app.db.dao.ArtistsDao
import meloplayer.app.db.dao.GenresDao
import meloplayer.app.db.dao.SongsDao
import meloplayer.app.db.dao.SyncDao
import meloplayer.app.db.entities.Album
import meloplayer.app.db.entities.Artist
import meloplayer.app.db.entities.Genre
import meloplayer.app.db.entities.Song
import meloplayer.app.db.entities.SongArtist
import meloplayer.app.db.entities.SongGenre
import meloplayer.app.db.entities.SongTag
import meloplayer.app.db.entities.Tag
import meloplayer.app.db.entities.derived.ArtistWithSongOrAlbumTypeDetail


@Database(
    entities = [
        Album::class, Song::class, Artist::class, Genre::class,
        Tag::class, SongArtist::class, SongGenre::class, SongTag::class],
    views = [
        ArtistWithSongOrAlbumTypeDetail::class
    ],
    version = 1
)
abstract class MeloDB : RoomDatabase() {
    abstract fun songsDao(): SongsDao
    abstract fun syncDao(): SyncDao
    abstract fun albumsDao(): AlbumsDao
    abstract fun artistsDao(): ArtistsDao
    abstract fun genresDao(): GenresDao

    companion object {
        fun buildInstance(context: Context) = Room.databaseBuilder(
            context.applicationContext,
            MeloDB::class.java, "music-database"
        ).build()
    }
}