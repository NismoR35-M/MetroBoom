package com.metroN.boomingC.music

import android.content.Context
import android.net.Uri
import android.os.Parcelable
import java.security.MessageDigest
import java.text.CollationKey
import java.util.UUID
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import com.metroN.boomingC.list.Item
import com.metroN.boomingC.music.metadata.Date
import com.metroN.boomingC.music.metadata.Disc
import com.metroN.boomingC.music.metadata.ReleaseType
import com.metroN.boomingC.music.storage.MimeType
import com.metroN.boomingC.music.storage.Path
import com.metroN.boomingC.util.toUuidOrNull

/**
 * Abstract music data. This contains universal information about all concrete music
 * implementations, such as identification information and names.
 */
sealed interface Music : Item {
    /**
     * A unique identifier for this music item.
     * @see UID
     */
    val uid: UID

    /**
     * The raw name of this item as it was extracted from the file-system. Will be null if the
     * item's name is unknown. When showing this item in a UI, avoid this in favor of [resolveName].
     */
    val rawName: String?

    /**
     * Returns a name suitable for use in the app UI. This should be favored over [rawName] in
     * nearly all cases.
     * @param context [Context] required to obtain placeholder text or formatting information.
     * @return A human-readable string representing the name of this music. In the case that the
     * item does not have a name, an analogous "Unknown X" name is returned.
     */
    fun resolveName(context: Context): String

    /**
     * The raw sort name of this item as it was extracted from the file-system. This can be used not
     * only when sorting music, but also trying to locate music based on a fuzzy search by the user.
     * Will be null if the item has no known sort name.
     */
    val rawSortName: String?

    /**
     * A [CollationKey] derived from [rawName] and [rawSortName] that can be used to sort items in a
     * semantically-correct manner. Will be null if the item has no name.
     *
     * The key will have the following attributes:
     * - If [rawSortName] is present, this key will be derived from it. Otherwise [rawName] is used.
     * - If the string begins with an article, such as "the", it will be stripped, as is usually
     * convention for sorting media. This is not internationalized.
     */
    val collationKey: CollationKey?

    /**
     * A unique identifier for a piece of music.
     *
     * [UID] enables a much cheaper and more reliable form of differentiating music, derived from
     * either a hash of meaningful metadata or the MusicBrainz ID spec. Using this enables several
     * improvements to music management in this app, including:
     *
     * - Proper differentiation of identical music. It's common for large, well-tagged libraries to
     * have functionally duplicate items that are differentiated with MusicBrainz IDs, and so [UID]
     * allows us to properly differentiate between these in the app.
     * - Better music persistence between restarts. Whereas directly storing song names would be
     * prone to collisions, and storing MediaStore IDs would drift rapidly as the music library
     * changes, [UID] enables a much stronger form of persistence given it's unique link to a
     * specific files metadata configuration, which is unlikely to collide with another item or
     * drift as the music library changes.
     *
     * Note: Generally try to use [UID] as a black box that can only be read, written, and compared.
     * It will not be fun if you try to manipulate it in any other manner.
     *
     * @author Alexander Capehart (OxygenCobalt)
     */
    @Parcelize
    class UID
    private constructor(
        private val format: Format,
        private val mode: MusicMode,
        private val uuid: UUID
    ) : Parcelable {
        // Cache the hashCode for HashMap efficiency.
        @IgnoredOnParcel private var hashCode = format.hashCode()

        init {
            hashCode = 31 * hashCode + mode.hashCode()
            hashCode = 31 * hashCode + uuid.hashCode()
        }

        override fun hashCode() = hashCode

        override fun equals(other: Any?) =
            other is UID && format == other.format && mode == other.mode && uuid == other.uuid

        override fun toString() = "${format.namespace}:${mode.intCode.toString(16)}-$uuid"

        /**
         * Internal marker of [Music.UID] format type.
         * @param namespace Namespace to use in the [Music.UID]'s string representation.
         */
        private enum class Format(val namespace: String) {
            /** @see auxio */
            AUXIO("metroN.metroN.boomingC"),

            /** @see musicBrainz */
            MUSICBRAINZ("metroN.musicbrainz")
        }

        companion object {
            /**
             * Creates an Auxio-style [UID] with a [UUID] composed of a hash of the non-subjective,
             * unlikely-to-change metadata of the music.
             * @param mode The analogous [MusicMode] of the item that created this [UID].
             * @param updates Block to update the [MessageDigest] hash with the metadata of the
             * item. Make sure the metadata hashed semantically aligns with the format
             * specification.
             * @return A new boomingC-style [UID].
             */
            fun auxio(mode: MusicMode, updates: MessageDigest.() -> Unit): UID {
                val digest =
                    MessageDigest.getInstance("SHA-256").run {
                        updates()
                        digest()
                    }
                // Convert the digest to a UUID. This does cleave off some of the hash, but this
                // is considered okay.
                val uuid =
                    UUID(
                        digest[0]
                            .toLong()
                            .shl(56)
                            .or(digest[1].toLong().and(0xFF).shl(48))
                            .or(digest[2].toLong().and(0xFF).shl(40))
                            .or(digest[3].toLong().and(0xFF).shl(32))
                            .or(digest[4].toLong().and(0xFF).shl(24))
                            .or(digest[5].toLong().and(0xFF).shl(16))
                            .or(digest[6].toLong().and(0xFF).shl(8))
                            .or(digest[7].toLong().and(0xFF)),
                        digest[8]
                            .toLong()
                            .shl(56)
                            .or(digest[9].toLong().and(0xFF).shl(48))
                            .or(digest[10].toLong().and(0xFF).shl(40))
                            .or(digest[11].toLong().and(0xFF).shl(32))
                            .or(digest[12].toLong().and(0xFF).shl(24))
                            .or(digest[13].toLong().and(0xFF).shl(16))
                            .or(digest[14].toLong().and(0xFF).shl(8))
                            .or(digest[15].toLong().and(0xFF)))
                return UID(Format.AUXIO, mode, uuid)
            }

            /**
             * Creates a MusicBrainz-style [UID] with a [UUID] derived from the MusicBrainz ID
             * extracted from a file.
             * @param mode The analogous [MusicMode] of the item that created this [UID].
             * @param mbid The analogous MusicBrainz ID for this item that was extracted from a
             * file.
             * @return A new MusicBrainz-style [UID].
             */
            fun musicBrainz(mode: MusicMode, mbid: UUID): UID = UID(Format.MUSICBRAINZ, mode, mbid)

            /**
             * Convert a [UID]'s string representation back into a concrete [UID] instance.
             * @param uid The [UID]'s string representation, formatted as
             * `format_namespace:music_mode_int-uuid`.
             * @return A [UID] converted from the string representation, or null if the string
             * representation was invalid.
             */
            fun fromString(uid: String): UID? {
                val split = uid.split(':', limit = 2)
                if (split.size != 2) {
                    return null
                }

                val format =
                    when (split[0]) {
                        Format.AUXIO.namespace -> Format.AUXIO
                        Format.MUSICBRAINZ.namespace -> Format.MUSICBRAINZ
                        else -> return null
                    }

                val ids = split[1].split('-', limit = 2)
                if (ids.size != 2) {
                    return null
                }

                val mode =
                    MusicMode.fromIntCode(ids[0].toIntOrNull(16) ?: return null) ?: return null
                val uuid = ids[1].toUuidOrNull() ?: return null
                return UID(format, mode, uuid)
            }
        }
    }
}

/**
 * An abstract grouping of [Song]s and other [Music] data.
 * @author Alexander Capehart (OxygenCobalt)
 */
sealed interface MusicParent : Music {
    /** The child [Song]s of this [MusicParent]. */
    val songs: List<Song>
}

/**
 * A song.
 * @author Alexander Capehart (OxygenCobalt)
 */
interface Song : Music {
    /** The track number. Will be null if no valid track number was present in the metadata. */
    val track: Int?
    /** The [Disc] number. Will be null if no valid disc number was present in the metadata. */
    val disc: Disc?
    /** The release [Date]. Will be null if no valid date was present in the metadata. */
    val date: Date?
    /**
     * The URI to the audio file that this instance was created from. This can be used to access the
     * audio file in a way that is scoped-storage-safe.
     */
    val uri: Uri
    /**
     * The [Path] to this audio file. This is only intended for display, [uri] should be favored
     * instead for accessing the audio file.
     */
    val path: Path
    /** The [MimeType] of the audio file. Only intended for display. */
    val mimeType: MimeType
    /** The size of the audio file, in bytes. */
    val size: Long
    /** The duration of the audio file, in milliseconds. */
    val durationMs: Long
    /** The date the audio file was added to the device, as a unix epoch timestamp. */
    val dateAdded: Long
    /**
     * The parent [Album]. If the metadata did not specify an album, it's parent directory is used
     * instead.
     */
    val album: Album
    /**
     * The parent [Artist]s of this [Song]. Is often one, but there can be multiple if more than one
     * [Artist] name was specified in the metadata. Unliked [Album], artists are prioritized for
     * this field.
     */
    val artists: List<Artist>
    /**
     * Resolves one or more [Artist]s into a single piece of human-readable names.
     * @param context [Context] required for [resolveName]. formatter.
     */
    fun resolveArtistContents(context: Context): String
    /**
     * Checks if the [Artist] *display* of this [Song] and another [Song] are equal. This will only
     * compare surface-level names, and not [Music.UID]s.
     * @param other The [Song] to compare to.
     * @return True if the [Artist] displays are equal, false otherwise
     */
    fun areArtistContentsTheSame(other: Song): Boolean
    /**
     * The parent [Genre]s of this [Song]. Is often one, but there can be multiple if more than one
     * [Genre] name was specified in the metadata.
     */
    val genres: List<Genre>
    /**
     * Resolves one or more [Genre]s into a single piece human-readable names.
     * @param context [Context] required for [resolveName].
     */
    fun resolveGenreContents(context: Context): String
}

/**
 * An abstract release group. While it may be called an album, it encompasses other types of
 * releases like singles, EPs, and compilations.
 * @author Alexander Capehart (OxygenCobalt)
 */
interface Album : MusicParent {
    /** The [Date.Range] that [Song]s in the [Album] were released. */
    val dates: Date.Range?
    /**
     * The [ReleaseType] of this album, signifying the type of release it actually is. Defaults to
     * [ReleaseType.Album].
     */
    val releaseType: ReleaseType
    /**
     * The URI to a MediaStore-provided album cover. These images will be fast to load, but at the
     * cost of image quality.
     */
    val coverUri: Uri
    /** The duration of all songs in the album, in milliseconds. */
    val durationMs: Long
    /** The earliest date a song in this album was added, as a unix epoch timestamp. */
    val dateAdded: Long
    /**
     * The parent [Artist]s of this [Album]. Is often one, but there can be multiple if more than
     * one [Artist] name was specified in the metadata of the [Song]'s. Unlike [Song], album artists
     * are prioritized for this field.
     */
    val artists: List<Artist>
    /**
     * Resolves one or more [Artist]s into a single piece of human-readable names.
     * @param context [Context] required for [resolveName].
     */
    fun resolveArtistContents(context: Context): String
    /**
     * Checks if the [Artist] *display* of this [Album] and another [Album] are equal. This will
     * only compare surface-level names, and not [Music.UID]s.
     * @param other The [Album] to compare to.
     * @return True if the [Artist] displays are equal, false otherwise
     */
    fun areArtistContentsTheSame(other: Album): Boolean
}

/**
 * An abstract artist. These are actually a combination of the artist and album artist tags from
 * within the library, derived from [Song]s and [Album]s respectively.
 * @author Alexander Capehart (OxygenCobalt)
 */
interface Artist : MusicParent {
    /**
     * All of the [Album]s this artist is credited to. Note that any [Song] credited to this artist
     * will have it's [Album] considered to be "indirectly" linked to this [Artist], and thus
     * included in this list.
     */
    val albums: List<Album>
    /**
     * The duration of all [Song]s in the artist, in milliseconds. Will be null if there are no
     * songs.
     */
    val durationMs: Long?
    /**
     * Whether this artist is considered a "collaborator", i.e it is not directly credited on any
     * [Album].
     */
    val isCollaborator: Boolean
    /** The [Genre]s of this artist. */
    val genres: List<Genre>
    /**
     * Resolves one or more [Genre]s into a single piece of human-readable names.
     * @param context [Context] required for [resolveName].
     */
    fun resolveGenreContents(context: Context): String
    /**
     * Checks if the [Genre] *display* of this [Artist] and another [Artist] are equal. This will
     * only compare surface-level names, and not [Music.UID]s.
     * @param other The [Artist] to compare to.
     * @return True if the [Genre] displays are equal, false otherwise
     */
    fun areGenreContentsTheSame(other: Artist): Boolean
}

/**
 * A genre.
 * @author Alexander Capehart (OxygenCobalt)
 */
interface Genre : MusicParent {
    /** The albums indirectly linked to by the [Song]s of this [Genre]. */
    val albums: List<Album>
    /** The artists indirectly linked to by the [Artist]s of this [Genre]. */
    val artists: List<Artist>
    /** The total duration of the songs in this genre, in milliseconds. */
    val durationMs: Long
}
