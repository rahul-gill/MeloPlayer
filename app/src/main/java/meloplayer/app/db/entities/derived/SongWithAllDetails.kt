package meloplayer.app.db.entities.derived

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation
import meloplayer.app.db.entities.Album
import meloplayer.app.db.entities.Genre
import meloplayer.app.db.entities.Song
import meloplayer.app.db.entities.SongGenre
import meloplayer.app.db.entities.SongTag
import meloplayer.app.db.entities.Tag

class SongWithAllDetails(
    @Embedded val song: Song,
    @Relation(
        parentColumn = "album_id",
        entityColumn = "album_id"
    )
    val album: Album?,
    @Relation(
        parentColumn = "song_id",
        entityColumn = "song_id",
    )
    val artists: List<ArtistWithSongOrAlbumTypeDetail>,
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