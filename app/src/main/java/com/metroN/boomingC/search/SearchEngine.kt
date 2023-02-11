package com.metroN.boomingC.search

import android.content.Context
import java.text.Normalizer
import com.metroN.boomingC.music.Album
import com.metroN.boomingC.music.Artist
import com.metroN.boomingC.music.Genre
import com.metroN.boomingC.music.Music
import com.metroN.boomingC.music.Song

/**
 * Implements the fuzzy-ish searching algorithm used in the search view.
 */
interface SearchEngine {
    /**
     * Begin a search.
     * @param items The items to search over.
     * @param query The query to search for.
     * @return A list of items filtered by the given query.
     */
    suspend fun search(items: Items, query: String): Items

    /**
     * Input/output data to use with [SearchEngine].
     * @param songs A list of [Song]s, null if empty.
     * @param albums A list of [Album]s, null if empty.
     * @param artists A list of [Artist]s, null if empty.
     * @param genres A list of [Genre]s, null if empty.
     */
    data class Items(
        val songs: List<Song>?,
        val albums: List<Album>?,
        val artists: List<Artist>?,
        val genres: List<Genre>?
    )

    companion object {
        /**
         * Get a framework-backed implementation.
         * @param context [Context] required.
         */
        fun from(context: Context): SearchEngine = RealSearchEngine(context)
    }
}

private class RealSearchEngine(private val context: Context) : SearchEngine {
    override suspend fun search(items: SearchEngine.Items, query: String) =
        SearchEngine.Items(
            songs = items.songs?.searchListImpl(query) { q, song -> song.path.name.contains(q) },
            albums = items.albums?.searchListImpl(query),
            artists = items.artists?.searchListImpl(query),
            genres = items.genres?.searchListImpl(query))

    /**
     * Search a given [Music] list.
     * @param query The query to search for. The routine will compare this query to the names of
     * each object in the list and
     * @param fallback Additional comparison code to run if the item does not match the query
     * initially. This can be used to compare against additional attributes to improve search result
     * quality.
     */
    private inline fun <T : Music> List<T>.searchListImpl(
        query: String,
        fallback: (String, T) -> Boolean = { _, _ -> false }
    ) =
        filter {
                // See if the plain resolved name matches the query. This works for most
                // situations.
                val name = it.resolveName(context)
                if (name.contains(query, ignoreCase = true)) {
                    return@filter true
                }

                // See if the sort name matches. This can sometimes be helpful as certain
                // libraries
                // will tag sort names to have a alphabetized version of the title.
                val sortName = it.rawSortName
                if (sortName != null && sortName.contains(query, ignoreCase = true)) {
                    return@filter true
                }

                // As a last-ditch effort, see if the normalized name matches. This will replace
                // any non-alphabetical characters with their alphabetical representations,
                // which
                // could make it match the query.
                val normalizedName =
                    NORMALIZATION_SANITIZE_REGEX.replace(
                        Normalizer.normalize(name, Normalizer.Form.NFKD), "")
                if (normalizedName.contains(query, ignoreCase = true)) {
                    return@filter true
                }

                fallback(query, it)
            }
            .ifEmpty { null }

    private companion object {
        /**
         * Converts the output of [Normalizer] to remove any junk characters added by it's
         * replacements.
         */
        val NORMALIZATION_SANITIZE_REGEX = Regex("\\p{InCombiningDiacriticalMarks}+")
    }
}
