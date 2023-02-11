package com.metroN.boomingC.music

import com.metroN.boomingC.music.library.Library

/**
 * A repository granting access to the music library.
 *
 * This can be used to obtain certain music items, or await changes to the music library. It is
 * generally recommended to use this over Indexer to keep track of the library state, as the
 * interface will be less volatile.
 */
interface MusicRepository {
    /**
     * The current [Library]. May be null if a [Library] has not been successfully loaded yet. This
     * can change, so it's highly recommended to not access this directly and instead rely on
     * [Listener].
     */
    var library: Library?

    /**
     * Add a [Listener] to this instance. This can be used to receive changes in the music library.
     * Will invoke all [Listener] methods to initialize the instance with the current state.
     * @param listener The [Listener] to add.
     * @see Listener
     */
    fun addListener(listener: Listener)

    /**
     * Remove a [Listener] from this instance, preventing it from receiving any further updates.
     * @param listener The [Listener] to remove. Does nothing if the [Listener] was never added in
     * the first place.
     * @see Listener
     */
    fun removeListener(listener: Listener)

    /** A listener for changes in [MusicRepository] */
    interface Listener {
        /**
         * Called when the current [Library] has changed.
         * @param library The new [Library], or null if no [Library] has been loaded yet.
         */
        fun onLibraryChanged(library: Library?)
    }

    companion object {
        /**
         * Create a new instance.
         * @return A newly-created implementation of [MusicRepository].
         */
        fun new(): MusicRepository = RealMusicRepository()
    }
}

private class RealMusicRepository : MusicRepository {
    private val listeners = mutableListOf<MusicRepository.Listener>()

    @Volatile
    override var library: Library? = null
        set(value) {
            field = value
            for (callback in listeners) {
                callback.onLibraryChanged(library)
            }
        }

    @Synchronized
    override fun addListener(listener: MusicRepository.Listener) {
        listener.onLibraryChanged(library)
        listeners.add(listener)
    }

    @Synchronized
    override fun removeListener(listener: MusicRepository.Listener) {
        listeners.remove(listener)
    }
}
