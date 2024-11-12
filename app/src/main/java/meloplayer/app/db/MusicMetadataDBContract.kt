package meloplayer.app.db

import android.provider.BaseColumns

object MusicMetadataDBContract {

    // Albums table definition
    object AlbumsEntry : BaseColumns {
        const val TABLE_NAME = "albums"
        const val COLUMN_NAME_TITLE = "title"
        const val COLUMN_NAME_RELEASE_DATE = "release_date"
        const val COLUMN_NAME_COVER_IMAGE_URI = "cover_image_uri"
    }

    private const val SQL_CREATE_ALBUMS = """
        CREATE TABLE ${AlbumsEntry.TABLE_NAME} (
            ${BaseColumns._ID} INTEGER PRIMARY KEY AUTOINCREMENT,
            ${AlbumsEntry.COLUMN_NAME_TITLE} TEXT NOT NULL,
            ${AlbumsEntry.COLUMN_NAME_RELEASE_DATE} INTEGER,
            ${AlbumsEntry.COLUMN_NAME_COVER_IMAGE_URI} TEXT
        )
    """

    private const val SQL_DELETE_ALBUMS = "DROP TABLE IF EXISTS ${AlbumsEntry.TABLE_NAME}"

    // Songs table definition
    object SongsEntry : BaseColumns {
        const val TABLE_NAME = "songs"
        const val COLUMN_NAME_TITLE = "title"
        const val COLUMN_NAME_FILE_SYSTEM_PATH = "file_system_path"
        const val COLUMN_NAME_LENGTH_MS = "length_ms"
        const val COLUMN_NAME_BIT_RATE_KBPS = "bit_rate_kbps"
        const val COLUMN_NAME_SAMPLE_RATE_HZ = "sample_rate_hz"
        const val COLUMN_NAME_CHANNELS_COUNT = "channels_count"
        const val COLUMN_NAME_COVER_IMAGE_URI = "cover_image_uri"
        const val COLUMN_NAME_TRACK_NUMBER = "track_number"
        const val COLUMN_NAME_CD_NUMBER = "cd_number"
        const val COLUMN_NAME_ALBUM_ID = "album_id"
        const val COLUMN_NAME_SUBTITLE = "subtitle"
        const val COLUMN_NAME_DATE_MODIFIED = "date_modified"
    }

    private const val SQL_CREATE_SONGS = """
        CREATE TABLE ${SongsEntry.TABLE_NAME} (
            ${BaseColumns._ID} INTEGER PRIMARY KEY,
            ${SongsEntry.COLUMN_NAME_TITLE} TEXT NOT NULL,
            ${SongsEntry.COLUMN_NAME_FILE_SYSTEM_PATH} TEXT NOT NULL,
            ${SongsEntry.COLUMN_NAME_LENGTH_MS} INTEGER NOT NULL,
            ${SongsEntry.COLUMN_NAME_BIT_RATE_KBPS} INTEGER NOT NULL,
            ${SongsEntry.COLUMN_NAME_SAMPLE_RATE_HZ} INTEGER NOT NULL,
            ${SongsEntry.COLUMN_NAME_CHANNELS_COUNT} INTEGER NOT NULL,
            ${SongsEntry.COLUMN_NAME_COVER_IMAGE_URI} TEXT,
            ${SongsEntry.COLUMN_NAME_TRACK_NUMBER} INTEGER,
            ${SongsEntry.COLUMN_NAME_CD_NUMBER} INTEGER,
            ${SongsEntry.COLUMN_NAME_ALBUM_ID} INTEGER,
            ${SongsEntry.COLUMN_NAME_SUBTITLE} TEXT,
            ${SongsEntry.COLUMN_NAME_DATE_MODIFIED} INTEGER NOT NULL,
            FOREIGN KEY (${SongsEntry.COLUMN_NAME_ALBUM_ID}) REFERENCES ${AlbumsEntry.TABLE_NAME}(${BaseColumns._ID})
        )
    """

    private const val SQL_DELETE_SONGS = "DROP TABLE IF EXISTS ${SongsEntry.TABLE_NAME}"

    // Artists table definition
    object ArtistsEntry : BaseColumns {
        const val TABLE_NAME = "artists"
        const val COLUMN_NAME_NAME = "name"
        const val COLUMN_NAME_IS_SONG_ARTIST = "is_song_artist"
        const val COLUMN_NAME_IS_ALBUM_ARTIST = "is_album_artist"
        const val COLUMN_NAME_BIO = "bio"
        const val COLUMN_NAME_IMAGE_URI = "image_uri"
    }

    private const val SQL_CREATE_ARTISTS = """
        CREATE TABLE ${ArtistsEntry.TABLE_NAME} (
            ${BaseColumns._ID} INTEGER PRIMARY KEY AUTOINCREMENT,
            ${ArtistsEntry.COLUMN_NAME_NAME} TEXT NOT NULL,
            ${ArtistsEntry.COLUMN_NAME_IS_SONG_ARTIST} INTEGER NOT NULL,
            ${ArtistsEntry.COLUMN_NAME_IS_ALBUM_ARTIST} INTEGER NOT NULL,
            ${ArtistsEntry.COLUMN_NAME_BIO} TEXT,
            ${ArtistsEntry.COLUMN_NAME_IMAGE_URI} TEXT
        )
    """

    private const val SQL_DELETE_ARTISTS = "DROP TABLE IF EXISTS ${ArtistsEntry.TABLE_NAME}"

    // Genres table definition
    object GenresEntry : BaseColumns {
        const val TABLE_NAME = "genres"
        const val COLUMN_NAME_NAME = "name"
        const val COLUMN_NAME_IMAGE_URI = "image_uri"
    }

    private const val SQL_CREATE_GENRES = """
        CREATE TABLE ${GenresEntry.TABLE_NAME} (
            ${BaseColumns._ID} INTEGER PRIMARY KEY AUTOINCREMENT,
            ${GenresEntry.COLUMN_NAME_NAME} TEXT NOT NULL,
            ${GenresEntry.COLUMN_NAME_IMAGE_URI} TEXT
        )
    """

    private const val SQL_DELETE_GENRES = "DROP TABLE IF EXISTS ${GenresEntry.TABLE_NAME}"

    // Song Artists table definition (many-to-many)
    object SongArtistsEntry : BaseColumns {
        const val TABLE_NAME = "song_artists"
        const val COLUMN_NAME_SONG_ID = "song_id"
        const val COLUMN_NAME_ARTIST_ID = "artist_id"
    }

    private const val SQL_CREATE_SONG_ARTISTS = """
        CREATE TABLE ${SongArtistsEntry.TABLE_NAME} (
            ${SongArtistsEntry.COLUMN_NAME_SONG_ID} INTEGER,
            ${SongArtistsEntry.COLUMN_NAME_ARTIST_ID} INTEGER,
            PRIMARY KEY (${SongArtistsEntry.COLUMN_NAME_SONG_ID}, ${SongArtistsEntry.COLUMN_NAME_ARTIST_ID}),
            FOREIGN KEY (${SongArtistsEntry.COLUMN_NAME_SONG_ID}) REFERENCES ${SongsEntry.TABLE_NAME}(${BaseColumns._ID}),
            FOREIGN KEY (${SongArtistsEntry.COLUMN_NAME_ARTIST_ID}) REFERENCES ${ArtistsEntry.TABLE_NAME}(${BaseColumns._ID})
        )
    """

    private const val SQL_DELETE_SONG_ARTISTS = "DROP TABLE IF EXISTS ${SongArtistsEntry.TABLE_NAME}"

    // Song Genres table definition (many-to-many)
    object SongGenresEntry : BaseColumns {
        const val TABLE_NAME = "song_genres"
        const val COLUMN_NAME_SONG_ID = "song_id"
        const val COLUMN_NAME_GENRE_ID = "genre_id"
    }

    private const val SQL_CREATE_SONG_GENRES = """
        CREATE TABLE ${SongGenresEntry.TABLE_NAME} (
            ${SongGenresEntry.COLUMN_NAME_SONG_ID} INTEGER,
            ${SongGenresEntry.COLUMN_NAME_GENRE_ID} INTEGER,
            PRIMARY KEY (${SongGenresEntry.COLUMN_NAME_SONG_ID}, ${SongGenresEntry.COLUMN_NAME_GENRE_ID}),
            FOREIGN KEY (${SongGenresEntry.COLUMN_NAME_SONG_ID}) REFERENCES ${SongsEntry.TABLE_NAME}(${BaseColumns._ID}),
            FOREIGN KEY (${SongGenresEntry.COLUMN_NAME_GENRE_ID}) REFERENCES ${GenresEntry.TABLE_NAME}(${BaseColumns._ID})
        )
    """

    private const val SQL_DELETE_SONG_GENRES = "DROP TABLE IF EXISTS ${SongGenresEntry.TABLE_NAME}"

    // SQL creation and deletion arrays
    val SQL_CREATE_ENTRIES = arrayOf(
        SQL_CREATE_ALBUMS,
        SQL_CREATE_SONGS,
        SQL_CREATE_ARTISTS,
        SQL_CREATE_GENRES,
        SQL_CREATE_SONG_ARTISTS,
        SQL_CREATE_SONG_GENRES
    )

    val SQL_DELETE_ENTRIES = arrayOf(
        SQL_DELETE_ALBUMS,
        SQL_DELETE_SONGS,
        SQL_DELETE_ARTISTS,
        SQL_DELETE_GENRES,
        SQL_DELETE_SONG_ARTISTS,
        SQL_DELETE_SONG_GENRES
    )
}
