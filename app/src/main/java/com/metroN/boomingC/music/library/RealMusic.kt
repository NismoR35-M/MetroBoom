package com.metroN.boomingC.music.library

import android.content.Context
import androidx.annotation.VisibleForTesting
import java.security.MessageDigest
import java.text.CollationKey
import java.text.Collator
import kotlin.math.max
import com.metroN.boomingC.R
import com.metroN.boomingC.list.Sort
import com.metroN.boomingC.music.Album
import com.metroN.boomingC.music.Artist
import com.metroN.boomingC.music.Genre
import com.metroN.boomingC.music.Music
import com.metroN.boomingC.music.MusicMode
import com.metroN.boomingC.music.MusicSettings
import com.metroN.boomingC.music.Song
import com.metroN.boomingC.music.metadata.Date
import com.metroN.boomingC.music.metadata.Disc
import com.metroN.boomingC.music.metadata.ReleaseType
import com.metroN.boomingC.music.metadata.parseId3GenreNames
import com.metroN.boomingC.music.metadata.parseMultiValue
import com.metroN.boomingC.music.storage.MimeType
import com.metroN.boomingC.music.storage.Path
import com.metroN.boomingC.music.storage.toAudioUri
import com.metroN.boomingC.music.storage.toCoverUri
import com.metroN.boomingC.util.nonZeroOrNull
import com.metroN.boomingC.util.toUuidOrNull
import com.metroN.boomingC.util.unlikelyToBeNull

/**
 * Library-backed implementation of [RealSong].
 * @param rawSong The [RawSong] to derive the member data from.
 * @param musicSettings [MusicSettings] to perform further user-configured parsing.
 */
class RealSong(rawSong: RawSong, musicSettings: MusicSettings) : Song {
    override val uid =
    // Attempt to use a MusicBrainz ID first before falling back to a hashed UID.
    rawSong.musicBrainzId?.toUuidOrNull()?.let { Music.UID.musicBrainz(MusicMode.SONGS, it) }
            ?: Music.UID.auxio(MusicMode.SONGS) {
                // Song UIDs are based on the raw data without parsing so that they remain
                // consistent across music setting changes. Parents are not held up to the
                // same standard since grouping is already inherently linked to settings.
                update(rawSong.name)
                update(rawSong.albumName)
                update(rawSong.date)

                update(rawSong.track)
                update(rawSong.disc)

                update(rawSong.artistNames)
                update(rawSong.albumArtistNames)
            }
    override val rawName = requireNotNull(rawSong.name) { "Invalid raw: No title" }
    override val rawSortName = rawSong.sortName
    override val collationKey = makeCollationKey(this)
    override fun resolveName(context: Context) = rawName

    override val track = rawSong.track
    override val disc = rawSong.disc?.let { Disc(it, rawSong.subtitle) }
    override val date = rawSong.date
    override val uri = requireNotNull(rawSong.mediaStoreId) { "Invalid raw: No id" }.toAudioUri()
    override val path =
        Path(
            name = requireNotNull(rawSong.fileName) { "Invalid raw: No display name" },
            parent = requireNotNull(rawSong.directory) { "Invalid raw: No parent directory" })
    override val mimeType =
        MimeType(
            fromExtension =
                requireNotNull(rawSong.extensionMimeType) { "Invalid raw: No mime type" },
            fromFormat = null)
    override val size = requireNotNull(rawSong.size) { "Invalid raw: No size" }
    override val durationMs = requireNotNull(rawSong.durationMs) { "Invalid raw: No duration" }
    override val dateAdded = requireNotNull(rawSong.dateAdded) { "Invalid raw: No date added" }
    private var _album: RealAlbum? = null
    override val album: Album
        get() = unlikelyToBeNull(_album)

    // Note: Only compare by UID so songs that differ only in MBID are treated differently.
    override fun hashCode() = uid.hashCode()
    override fun equals(other: Any?) = other is Song && uid == other.uid

    private val artistMusicBrainzIds = rawSong.artistMusicBrainzIds.parseMultiValue(musicSettings)
    private val artistNames = rawSong.artistNames.parseMultiValue(musicSettings)
    private val artistSortNames = rawSong.artistSortNames.parseMultiValue(musicSettings)
    private val rawIndividualArtists =
        artistNames.mapIndexed { i, name ->
            RawArtist(
                artistMusicBrainzIds.getOrNull(i)?.toUuidOrNull(),
                name,
                artistSortNames.getOrNull(i))
        }

    private val albumArtistMusicBrainzIds =
        rawSong.albumArtistMusicBrainzIds.parseMultiValue(musicSettings)
    private val albumArtistNames = rawSong.albumArtistNames.parseMultiValue(musicSettings)
    private val albumArtistSortNames = rawSong.albumArtistSortNames.parseMultiValue(musicSettings)
    private val rawAlbumArtists =
        albumArtistNames.mapIndexed { i, name ->
            RawArtist(
                albumArtistMusicBrainzIds.getOrNull(i)?.toUuidOrNull(),
                name,
                albumArtistSortNames.getOrNull(i))
        }

    private val _artists = mutableListOf<RealArtist>()
    override val artists: List<Artist>
        get() = _artists
    override fun resolveArtistContents(context: Context) = resolveNames(context, artists)
    override fun areArtistContentsTheSame(other: Song): Boolean {
        for (i in 0 until max(artists.size, other.artists.size)) {
            val a = artists.getOrNull(i) ?: return false
            val b = other.artists.getOrNull(i) ?: return false
            if (a.rawName != b.rawName) {
                return false
            }
        }

        return true
    }

    private val _genres = mutableListOf<RealGenre>()
    override val genres: List<Genre>
        get() = _genres
    override fun resolveGenreContents(context: Context) = resolveNames(context, genres)

    /**
     * The [RawAlbum] instances collated by the [RealSong]. This can be used to group [RealSong]s
     * into an [RealAlbum].
     */
    val rawAlbum =
        RawAlbum(
            mediaStoreId = requireNotNull(rawSong.albumMediaStoreId) { "Invalid raw: No album id" },
            musicBrainzId = rawSong.albumMusicBrainzId?.toUuidOrNull(),
            name = requireNotNull(rawSong.albumName) { "Invalid raw: No album name" },
            sortName = rawSong.albumSortName,
            releaseType = ReleaseType.parse(rawSong.releaseTypes.parseMultiValue(musicSettings)),
            rawArtists =
                rawAlbumArtists
                    .ifEmpty { rawIndividualArtists }
                    .ifEmpty { listOf(RawArtist(null, null)) })

    /**
     * The [RawArtist] instances collated by the [RealSong]. The artists of the song take priority,
     * followed by the album artists. If there are no artists, this field will be a single "unknown"
     * [RawArtist]. This can be used to group up [RealSong]s into an [RealArtist].
     */
    val rawArtists =
        rawIndividualArtists.ifEmpty { rawAlbumArtists }.ifEmpty { listOf(RawArtist()) }

    /**
     * The [RawGenre] instances collated by the [RealSong]. This can be used to group up [RealSong]s
     * into a [RealGenre]. ID3v2 Genre names are automatically converted to their resolved names.
     */
    val rawGenres =
        rawSong.genreNames
            .parseId3GenreNames(musicSettings)
            .map { RawGenre(it) }
            .ifEmpty { listOf(RawGenre()) }

    /**
     * Links this [RealSong] with a parent [RealAlbum].
     * @param album The parent [RealAlbum] to link to.
     */
    fun link(album: RealAlbum) {
        _album = album
    }

    /**
     * Links this [RealSong] with a parent [RealArtist].
     * @param artist The parent [RealArtist] to link to.
     */
    fun link(artist: RealArtist) {
        _artists.add(artist)
    }

    /**
     * Links this [RealSong] with a parent [RealGenre].
     * @param genre The parent [RealGenre] to link to.
     */
    fun link(genre: RealGenre) {
        _genres.add(genre)
    }

    /**
     * Perform final validation and comanization on this instance.
     * @return This instance upcasted to [Song].
     */
    fun finalize(): Song {
        checkNotNull(_album) { "Malformed song: No album" }

        check(_artists.isNotEmpty()) { "Malformed song: No artists" }
        for (i in _artists.indices) {
            // Non-destructively reorder the linked artists so that they align with
            // the artist ordering within the song metadata.
            val newIdx = _artists[i].getOriginalPositionIn(rawArtists)
            val other = _artists[newIdx]
            _artists[newIdx] = _artists[i]
            _artists[i] = other
        }

        check(_genres.isNotEmpty()) { "Malformed song: No genres" }
        for (i in _genres.indices) {
            // Non-destructively reorder the linked genres so that they align with
            // the genre ordering within the song metadata.
            val newIdx = _genres[i].getOriginalPositionIn(rawGenres)
            val other = _genres[newIdx]
            _genres[newIdx] = _genres[i]
            _genres[i] = other
        }
        return this
    }
}

/**
 * Library-backed implementation of [RealAlbum].
 * @param rawAlbum The [RawAlbum] to derive the member data from.
 * @param songs The [RealSong]s that are a part of this [RealAlbum]. These items will be linked to
 * this [RealAlbum].
 * @author Alexander Capehart (OxygenCobalt)
 */
class RealAlbum(val rawAlbum: RawAlbum, override val songs: List<RealSong>) : Album {
    override val uid =
    // Attempt to use a MusicBrainz ID first before falling back to a hashed UID.
    rawAlbum.musicBrainzId?.let { Music.UID.musicBrainz(MusicMode.ALBUMS, it) }
            ?: Music.UID.auxio(MusicMode.ALBUMS) {
                // Hash based on only names despite the presence of a date to increase stability.
                // I don't know if there is any situation where an artist will have two albums with
                // the exact same name, but if there is, I would love to know.
                update(rawAlbum.name)
                update(rawAlbum.rawArtists.map { it.name })
            }
    override val rawName = rawAlbum.name
    override val rawSortName = rawAlbum.sortName
    override val collationKey = makeCollationKey(this)
    override fun resolveName(context: Context) = rawName

    override val dates = Date.Range.from(songs.mapNotNull { it.date })
    override val releaseType = rawAlbum.releaseType ?: ReleaseType.Album(null)
    override val coverUri = rawAlbum.mediaStoreId.toCoverUri()
    override val durationMs: Long
    override val dateAdded: Long

    // Note: Append song contents to MusicParent equality so that Groups with
    // the same UID but different contents are not equal.
    override fun hashCode() = 31 * uid.hashCode() + songs.hashCode()
    override fun equals(other: Any?) =
        other is RealAlbum && uid == other.uid && songs == other.songs

    private val _artists = mutableListOf<RealArtist>()
    override val artists: List<Artist>
        get() = _artists
    override fun resolveArtistContents(context: Context) = resolveNames(context, artists)
    override fun areArtistContentsTheSame(other: Album): Boolean {
        for (i in 0 until max(artists.size, other.artists.size)) {
            val a = artists.getOrNull(i) ?: return false
            val b = other.artists.getOrNull(i) ?: return false
            if (a.rawName != b.rawName) {
                return false
            }
        }

        return true
    }
    init {
        var totalDuration: Long = 0
        var earliestDateAdded: Long = Long.MAX_VALUE

        // Do linking and value generation in the same loop for efficiency.
        for (song in songs) {
            song.link(this)
            if (song.dateAdded < earliestDateAdded) {
                earliestDateAdded = song.dateAdded
            }
            totalDuration += song.durationMs
        }

        durationMs = totalDuration
        dateAdded = earliestDateAdded
    }

    /**
     * The [RawArtist] instances collated by the [RealAlbum]. The album artists of the song take
     * priority, followed by the artists. If there are no artists, this field will be a single
     * "unknown" [RawArtist]. This can be used to group up [RealAlbum]s into an [RealArtist].
     */
    val rawArtists = rawAlbum.rawArtists

    /**
     * Links this [RealAlbum] with a parent [RealArtist].
     * @param artist The parent [RealArtist] to link to.
     */
    fun link(artist: RealArtist) {
        _artists.add(artist)
    }

    /**
     * Perform final validation and comanization on this instance.
     * @return This instance upcasted to [Album].
     */
    fun finalize(): Album {
        check(songs.isNotEmpty()) { "Malformed album: Empty" }
        check(_artists.isNotEmpty()) { "Malformed album: No artists" }
        for (i in _artists.indices) {
            // Non-destructively reorder the linked artists so that they align with
            // the artist ordering within the song metadata.
            val newIdx = _artists[i].getOriginalPositionIn(rawArtists)
            val other = _artists[newIdx]
            _artists[newIdx] = _artists[i]
            _artists[i] = other
        }
        return this
    }
}

/**
 * Library-backed implementation of [RealArtist].
 * @param rawArtist The [RawArtist] to derive the member data from.
 * @param songAlbums A list of the [RealSong]s and [RealAlbum]s that are a part of this [RealArtist]
 * , either through artist or album artist tags. Providing [RealSong]s to the artist is optional.
 * These instances will be linked to this [RealArtist].
 * @author Alexander Capehart (OxygenCobalt)
 */
class RealArtist(private val rawArtist: RawArtist, songAlbums: List<Music>) : Artist {
    override val uid =
    // Attempt to use a MusicBrainz ID first before falling back to a hashed UID.
    rawArtist.musicBrainzId?.let { Music.UID.musicBrainz(MusicMode.ARTISTS, it) }
            ?: Music.UID.auxio(MusicMode.ARTISTS) { update(rawArtist.name) }
    override val rawName = rawArtist.name
    override val rawSortName = rawArtist.sortName
    override val collationKey = makeCollationKey(this)
    override fun resolveName(context: Context) = rawName ?: context.getString(R.string.def_artist)
    override val songs: List<Song>

    override val albums: List<Album>
    override val durationMs: Long?
    override val isCollaborator: Boolean

    // Note: Append song contents to MusicParent equality so that Groups with
    // the same UID but different contents are not equal.
    override fun hashCode() = 31 * uid.hashCode() + songs.hashCode()
    override fun equals(other: Any?) =
        other is RealArtist && uid == other.uid && songs == other.songs

    override lateinit var genres: List<Genre>
    override fun resolveGenreContents(context: Context) = resolveNames(context, genres)
    override fun areGenreContentsTheSame(other: Artist): Boolean {
        for (i in 0 until max(genres.size, other.genres.size)) {
            val a = genres.getOrNull(i) ?: return false
            val b = other.genres.getOrNull(i) ?: return false
            if (a.rawName != b.rawName) {
                return false
            }
        }

        return true
    }

    init {
        val distinctSongs = mutableSetOf<Song>()
        val distinctAlbums = mutableSetOf<Album>()

        var noAlbums = true

        for (music in songAlbums) {
            when (music) {
                is RealSong -> {
                    music.link(this)
                    distinctSongs.add(music)
                    distinctAlbums.add(music.album)
                }
                is RealAlbum -> {
                    music.link(this)
                    distinctAlbums.add(music)
                    noAlbums = false
                }
                else -> error("Unexpected input music ${music::class.simpleName}")
            }
        }

        songs = distinctSongs.toList()
        albums = distinctAlbums.toList()
        durationMs = songs.sumOf { it.durationMs }.nonZeroOrNull()
        isCollaborator = noAlbums
    }

    /**
     * Returns the original position of this [RealArtist]'s [RawArtist] within the given [RawArtist]
     * list. This can be used to create a consistent ordering within child [RealArtist] lists based
     * on the original tag order.
     * @param rawArtists The [RawArtist] instances to check. It is assumed that this [RealArtist]'s
     * [RawArtist] will be within the list.
     * @return The index of the [RealArtist]'s [RawArtist] within the list.
     */
    fun getOriginalPositionIn(rawArtists: List<RawArtist>) = rawArtists.indexOf(rawArtist)

    /**
     * Perform final validation and comanization on this instance.
     * @return This instance upcasted to [Artist].
     */
    fun finalize(): Artist {
        check(songs.isNotEmpty() || albums.isNotEmpty()) { "Malformed artist: Empty" }
        genres =
            Sort(Sort.Mode.ByName, Sort.Direction.ASCENDING)
                .genres(songs.flatMapTo(mutableSetOf()) { it.genres })
                .sortedByDescending { genre -> songs.count { it.genres.contains(genre) } }
        return this
    }
}
/**
 * Library-backed implementation of [RealGenre].
 * @author Alexander Capehart (OxygenCobalt)
 */
class RealGenre(private val rawGenre: RawGenre, override val songs: List<RealSong>) : Genre {
    override val uid = Music.UID.auxio(MusicMode.GENRES) { update(rawGenre.name) }
    override val rawName = rawGenre.name
    override val rawSortName = rawName
    override val collationKey = makeCollationKey(this)
    override fun resolveName(context: Context) = rawName ?: context.getString(R.string.def_genre)

    override val albums: List<Album>
    override val artists: List<Artist>
    override val durationMs: Long

    // Note: Append song contents to MusicParent equality so that Groups with
    // the same UID but different contents are not equal.
    override fun hashCode() = 31 * uid.hashCode() + songs.hashCode()
    override fun equals(other: Any?) =
        other is RealGenre && uid == other.uid && songs == other.songs

    init {
        val distinctAlbums = mutableSetOf<Album>()
        val distinctArtists = mutableSetOf<Artist>()
        var totalDuration = 0L

        for (song in songs) {
            song.link(this)
            distinctAlbums.add(song.album)
            distinctArtists.addAll(song.artists)
            totalDuration += song.durationMs
        }

        albums =
            Sort(Sort.Mode.ByName, Sort.Direction.ASCENDING)
                .albums(distinctAlbums)
                .sortedByDescending { album -> album.songs.count { it.genres.contains(this) } }
        artists = Sort(Sort.Mode.ByName, Sort.Direction.ASCENDING).artists(distinctArtists)
        durationMs = totalDuration
    }

    /**
     * Returns the original position of this [RealGenre]'s [RawGenre] within the given [RawGenre]
     * list. This can be used to create a consistent ordering within child [RealGenre] lists based
     * on the original tag order.
     * @param rawGenres The [RawGenre] instances to check. It is assumed that this [RealGenre] 's
     * [RawGenre] will be within the list.
     * @return The index of the [RealGenre]'s [RawGenre] within the list.
     */
    fun getOriginalPositionIn(rawGenres: List<RawGenre>) = rawGenres.indexOf(rawGenre)

    /**
     * Perform final validation and comanization on this instance.
     * @return This instance upcasted to [Genre].
     */
    fun finalize(): Music {
        check(songs.isNotEmpty()) { "Malformed genre: Empty" }
        return this
    }
}

/**
 * Update a [MessageDigest] with a lowercase [String].
 * @param string The [String] to hash. If null, it will not be hashed.
 */
@VisibleForTesting
fun MessageDigest.update(string: String?) {
    if (string != null) {
        update(string.lowercase().toByteArray())
    } else {
        update(0)
    }
}

/**
 * Update a [MessageDigest] with the string representation of a [Date].
 * @param date The [Date] to hash. If null, nothing will be done.
 */
@VisibleForTesting
fun MessageDigest.update(date: Date?) {
    if (date != null) {
        update(date.toString().toByteArray())
    } else {
        update(0)
    }
}

/**
 * Update a [MessageDigest] with the lowercase versions of all of the input [String]s.
 * @param strings The [String]s to hash. If a [String] is null, it will not be hashed.
 */
@VisibleForTesting
fun MessageDigest.update(strings: List<String?>) {
    strings.forEach(::update)
}

/**
 * Update a [MessageDigest] with the little-endian bytes of a [Int].
 * @param n The [Int] to write. If null, nothing will be done.
 */
@VisibleForTesting
fun MessageDigest.update(n: Int?) {
    if (n != null) {
        update(byteArrayOf(n.toByte(), n.shr(8).toByte(), n.shr(16).toByte(), n.shr(24).toByte()))
    } else {
        update(0)
    }
}

/**
 * Join a list of [Music]'s resolved names into a string in a localized manner, using
 * [R.string.fmt_list].
 * @param context [Context] required to obtain localized formatting.
 * @param values The list of [Music] to format.
 * @return A single string consisting of the values delimited by a localized separator.
 */
private fun resolveNames(context: Context, values: List<Music>): String {
    if (values.isEmpty()) {
        // Nothing to do.
        return ""
    }

    var joined = values.first().resolveName(context)
    for (i in 1..values.lastIndex) {
        // Chain all previous values with the next value in the list with another delimiter.
        joined = context.getString(R.string.fmt_list, joined, values[i].resolveName(context))
    }
    return joined
}

/** Cached collator instance re-used with [makeCollationKey]. */
private val COLLATOR: Collator = Collator.getInstance().apply { strength = Collator.PRIMARY }

/**
 * Provided implementation to create a [CollationKey] in the way described by [Music.collationKey].
 * This should be used in all overrides of all [CollationKey].
 * @param music The [Music] to create the [CollationKey] for.
 * @return A [CollationKey] that follows the specification described by [Music.collationKey].
 */
private fun makeCollationKey(music: Music): CollationKey? {
    val sortName =
        (music.rawSortName ?: music.rawName)?.run {
            when {
                length > 5 && startsWith("the ", ignoreCase = true) -> substring(4)
                length > 4 && startsWith("an ", ignoreCase = true) -> substring(3)
                length > 3 && startsWith("a ", ignoreCase = true) -> substring(2)
                else -> this
            }
        }

    return COLLATOR.getCollationKey(sortName)
}
