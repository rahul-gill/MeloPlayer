package meloplayer.app.storex.entities.derived

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation
import meloplayer.app.storex.entities.Album
import meloplayer.app.storex.entities.Artist
import meloplayer.app.storex.entities.Genre
import meloplayer.app.storex.entities.Song
import meloplayer.app.storex.entities.SongArtist
import meloplayer.app.storex.entities.SongGenre
import meloplayer.app.storex.entities.SongTag
import meloplayer.app.storex.entities.Tag

class SongWithAllDetails(
    @Embedded val song: Song,
    @Relation(
        parentColumn = "album_id",
        entityColumn = "album_id"
    )
    val album: Album?,
    @Relation(
        parentColumn = "song_id",
        entityColumn = "artist_id",
        associateBy = Junction(SongArtist::class)
    )
    val artists: List<Artist>,
    @Relation(
        parentColumn = "song_id",
        entityColumn = "genre_id",
        associateBy = Junction(SongGenre::class)
    )
    val genres: List<Genre>,
    @Relation(
        parentColumn = "song_id",
        entityColumn = "tag_id",
        associateBy = Junction(SongTag::class)
    )
    val tags: List<Tag>


)