package com.metroN.boomingC.music.extractor

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.metroN.boomingC.music.Song
import com.metroN.boomingC.music.library.RawSong
import com.metroN.boomingC.music.metadata.Date
import com.metroN.boomingC.music.metadata.correctWhitespace
import com.metroN.boomingC.music.metadata.splitEscaped
import com.metroN.boomingC.util.*

/**
 * A cache of music metadata obtained in prior music loading operations. Obtain an instance with
 * [MetadataCacheRepository].
 */
interface MetadataCache {
    /** Whether this cache has encountered a [RawSong] that did not have a cache entry. */
    val invalidated: Boolean

    /**
     * Populate a [RawSong] from a cache entry, if it exists.
     * @param rawSong The [RawSong] to populate.
     * @return true if a cache entry could be applied to [rawSong], false otherwise.
     */
    fun populate(rawSong: RawSong): Boolean
}

private class RealMetadataCache(cachedSongs: List<CachedSong>) : MetadataCache {
    private val cacheMap = buildMap {
        for (cachedSong in cachedSongs) {
            put(cachedSong.mediaStoreId, cachedSong)
        }
    }

    override var invalidated = false
    override fun populate(rawSong: RawSong): Boolean {

        // For a cached raw song to be used, it must exist within the cache and have matching
        // addition and modification timestamps. Technically the addition timestamp doesn't
        // exist, but to safeguard against possible OEM-specific timestamp incoherence, we
        // check for it anyway.
        val cachedSong = cacheMap[rawSong.mediaStoreId]
        if (cachedSong != null &&
            cachedSong.dateAdded == rawSong.dateAdded &&
            cachedSong.dateModified == rawSong.dateModified) {
            cachedSong.copyToRaw(rawSong)
            return true
        }

        // We could not populate this song. This means our cache is stale and should be
        // re-written with newly-loaded music.
        invalidated = true
        return false
    }
}

/**
 * A repository allowing access to cached metadata obtained in prior music loading operations.
 * @author Alexander Capehart (OxygenCobalt)
 */
interface MetadataCacheRepository {
    /**
     * Read the current [MetadataCache], if it exists.
     * @return The stored [MetadataCache], or null if it could not be obtained.
     */
    suspend fun readCache(): MetadataCache?

    /**
     * Write the list of newly-loaded [RawSong]s to the cache, replacing the prior data.
     * @param rawSongs The [rawSongs] to write to the cache.
     */
    suspend fun writeCache(rawSongs: List<RawSong>)

    companion object {
        /**
         * Create a framework-backed instance.
         * @param context [Context] required.
         * @return A new instance.
         */
        fun from(context: Context): MetadataCacheRepository = RealMetadataCacheRepository(context)
    }
}

private class RealMetadataCacheRepository(private val context: Context) : MetadataCacheRepository {
    private val cachedSongsDao: CachedSongsDao by lazy {
        MetadataCacheDatabase.getInstance(context).cachedSongsDao()
    }

    override suspend fun readCache() =
        try {
            // Faster to load the whole database into memory than do a query on each
            // populate call.
            RealMetadataCache(cachedSongsDao.readSongs())
        } catch (e: Exception) {
            logE("Unable to load cache database.")
            logE(e.stackTraceToString())
            null
        }

    override suspend fun writeCache(rawSongs: List<RawSong>) {
        try {
            // Still write out whatever data was extracted.
            cachedSongsDao.nukeSongs()
            cachedSongsDao.insertSongs(rawSongs.map(CachedSong::fromRaw))
        } catch (e: Exception) {
            logE("Unable to save cache database.")
            logE(e.stackTraceToString())
        }
    }
}

@Database(entities = [CachedSong::class], version = 27, exportSchema = false)
private abstract class MetadataCacheDatabase : RoomDatabase() {
    abstract fun cachedSongsDao(): CachedSongsDao

    companion object {
        @Volatile private var INSTANCE: MetadataCacheDatabase? = null

        /**
         * Get/create the shared instance of this database.
         * @param context [Context] required.
         */
        fun getInstance(context: Context): MetadataCacheDatabase {
            val instance = INSTANCE
            if (instance != null) {
                return instance
            }

            synchronized(this) {
                val newInstance =
                    Room.databaseBuilder(
                            context.applicationContext,
                            MetadataCacheDatabase::class.java,
                            "auxio_metadata_cache.db")
                        .fallbackToDestructiveMigration()
                        .fallbackToDestructiveMigrationFrom(0)
                        .fallbackToDestructiveMigrationOnDowngrade()
                        .build()
                INSTANCE = newInstance
                return newInstance
            }
        }
    }
}

@Dao
private interface CachedSongsDao {
    @Query("SELECT * FROM ${CachedSong.TABLE_NAME}") suspend fun readSongs(): List<CachedSong>
    @Query("DELETE FROM ${CachedSong.TABLE_NAME}") suspend fun nukeSongs()
    @Insert suspend fun insertSongs(songs: List<CachedSong>)
}

@Entity(tableName = CachedSong.TABLE_NAME)
@TypeConverters(CachedSong.Converters::class)
private data class CachedSong(
    /**
     * The ID of the [Song]'s audio file, obtained from MediaStore. Note that this ID is highly
     * unstable and should only be used for accessing the audio file.
     */
    @PrimaryKey var mediaStoreId: Long,
    /** @see RawSong.dateAdded */
    var dateAdded: Long,
    /** The latest date the [Song]'s audio file was modified, as a unix epoch timestamp. */
    var dateModified: Long,
    /** @see RawSong.size */
    var size: Long? = null,
    /** @see RawSong */
    var durationMs: Long,
    /** @see RawSong.musicBrainzId */
    var musicBrainzId: String? = null,
    /** @see RawSong.name */
    var name: String,
    /** @see RawSong.sortName */
    var sortName: String? = null,
    /** @see RawSong.track */
    var track: Int? = null,
    /** @see RawSong.name */
    var disc: Int? = null,
    /** @See RawSong.subtitle */
    var subtitle: String? = null,
    /** @see RawSong.date */
    var date: Date? = null,
    /** @see RawSong.albumMusicBrainzId */
    var albumMusicBrainzId: String? = null,
    /** @see RawSong.albumName */
    var albumName: String,
    /** @see RawSong.albumSortName */
    var albumSortName: String? = null,
    /** @see RawSong.releaseTypes */
    var releaseTypes: List<String> = listOf(),
    /** @see RawSong.artistMusicBrainzIds */
    var artistMusicBrainzIds: List<String> = listOf(),
    /** @see RawSong.artistNames */
    var artistNames: List<String> = listOf(),
    /** @see RawSong.artistSortNames */
    var artistSortNames: List<String> = listOf(),
    /** @see RawSong.albumArtistMusicBrainzIds */
    var albumArtistMusicBrainzIds: List<String> = listOf(),
    /** @see RawSong.albumArtistNames */
    var albumArtistNames: List<String> = listOf(),
    /** @see RawSong.albumArtistSortNames */
    var albumArtistSortNames: List<String> = listOf(),
    /** @see RawSong.genreNames */
    var genreNames: List<String> = listOf()
) {
    fun copyToRaw(rawSong: RawSong): CachedSong {
        rawSong.musicBrainzId = musicBrainzId
        rawSong.name = name
        rawSong.sortName = sortName

        rawSong.size = size
        rawSong.durationMs = durationMs

        rawSong.track = track
        rawSong.disc = disc
        rawSong.date = date

        rawSong.albumMusicBrainzId = albumMusicBrainzId
        rawSong.albumName = albumName
        rawSong.albumSortName = albumSortName
        rawSong.releaseTypes = releaseTypes

        rawSong.artistMusicBrainzIds = artistMusicBrainzIds
        rawSong.artistNames = artistNames
        rawSong.artistSortNames = artistSortNames

        rawSong.albumArtistMusicBrainzIds = albumArtistMusicBrainzIds
        rawSong.albumArtistNames = albumArtistNames
        rawSong.albumArtistSortNames = albumArtistSortNames

        rawSong.genreNames = genreNames
        return this
    }

    object Converters {
        @TypeConverter
        fun fromMultiValue(values: List<String>) =
            values.joinToString(";") { it.replace(";", "\\;") }

        @TypeConverter
        fun toMultiValue(string: String) = string.splitEscaped { it == ';' }.correctWhitespace()

        @TypeConverter fun fromDate(date: Date?) = date?.toString()

        @TypeConverter fun toDate(string: String?) = string?.let(Date::from)
    }

    companion object {
        const val TABLE_NAME = "cached_songs"

        fun fromRaw(rawSong: RawSong) =
            CachedSong(
                mediaStoreId =
                    requireNotNull(rawSong.mediaStoreId) { "Invalid raw: No MediaStore ID" },
                dateAdded = requireNotNull(rawSong.dateAdded) { "Invalid raw: No date added" },
                dateModified =
                    requireNotNull(rawSong.dateModified) { "Invalid raw: No date modified" },
                musicBrainzId = rawSong.musicBrainzId,
                name = requireNotNull(rawSong.name) { "Invalid raw: No name" },
                sortName = rawSong.sortName,
                size = rawSong.size,
                durationMs = requireNotNull(rawSong.durationMs) { "Invalid raw: No duration" },
                track = rawSong.track,
                disc = rawSong.disc,
                date = rawSong.date,
                albumMusicBrainzId = rawSong.albumMusicBrainzId,
                albumName = requireNotNull(rawSong.albumName) { "Invalid raw: No album name" },
                albumSortName = rawSong.albumSortName,
                releaseTypes = rawSong.releaseTypes,
                artistMusicBrainzIds = rawSong.artistMusicBrainzIds,
                artistNames = rawSong.artistNames,
                artistSortNames = rawSong.artistSortNames,
                albumArtistMusicBrainzIds = rawSong.albumArtistMusicBrainzIds,
                albumArtistNames = rawSong.albumArtistNames,
                albumArtistSortNames = rawSong.albumArtistSortNames,
                genreNames = rawSong.genreNames)
    }
}
