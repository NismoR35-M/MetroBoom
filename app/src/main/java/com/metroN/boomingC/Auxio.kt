package com.metroN.boomingC

import android.app.Application
import android.content.Intent
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.request.CachePolicy
import com.metroN.boomingC.image.ImageSettings
import com.metroN.boomingC.image.extractor.AlbumCoverFetcher
import com.metroN.boomingC.image.extractor.ArtistImageFetcher
import com.metroN.boomingC.image.extractor.ErrorCrossfadeTransitionFactory
import com.metroN.boomingC.image.extractor.GenreImageFetcher
import com.metroN.boomingC.image.extractor.MusicKeyer
import com.metroN.boomingC.playback.PlaybackSettings
import com.metroN.boomingC.ui.UISettings
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class Auxio : Application(), ImageLoaderFactory {
    override fun onCreate() {
        super.onCreate()
        // Migrate any settings that may have changed in an app update.
        ImageSettings.from(this).migrate()
        PlaybackSettings.from(this).migrate()
        UISettings.from(this).migrate()
        // Adding static shortcuts in a dynamic manner is better than declaring them
        // manually, as it will properly handle the difference between debug and release
        // Auxio instances.
        ShortcutManagerCompat.addDynamicShortcuts(
            this,
            listOf(
                ShortcutInfoCompat.Builder(
                        this, com.metroN.boomingC.Auxio.Companion.SHORTCUT_SHUFFLE_ID)
                    .setShortLabel(
                        getString(com.metroN.boomingC.R.string.lbl_shuffle_shortcut_short))
                    .setLongLabel(getString(com.metroN.boomingC.R.string.lbl_shuffle_shortcut_long))
                    .setIcon(
                        IconCompat.createWithResource(
                            this, com.metroN.boomingC.R.drawable.ic_shortcut_shuffle_24))
                    .setIntent(
                        Intent(this, com.metroN.boomingC.MainActivity::class.java)
                            .setAction(
                                com.metroN.boomingC.Auxio.Companion.INTENT_KEY_SHORTCUT_SHUFFLE))
                    .build()))
    }

    override fun newImageLoader() =
        ImageLoader.Builder(applicationContext)
            .components {
                // Add fetchers for Music components to make them usable with ImageRequest
                add(MusicKeyer())
                add(AlbumCoverFetcher.SongFactory())
                add(AlbumCoverFetcher.AlbumFactory())
                add(ArtistImageFetcher.Factory())
                add(GenreImageFetcher.Factory())
            }
            // Use our own crossfade with error drawable support
            .transitionFactory(ErrorCrossfadeTransitionFactory())
            // Not downloading anything, so no disk-caching
            .diskCachePolicy(CachePolicy.DISABLED)
            .build()

    companion object {
        /** The [Intent] name for the "Shuffle All" shortcut. */
        const val INTENT_KEY_SHORTCUT_SHUFFLE = BuildConfig.APPLICATION_ID + ".action.SHUFFLE_ALL"
        /** The ID of the "Shuffle All" shortcut. */
        private const val SHORTCUT_SHUFFLE_ID = "shortcut_shuffle"
    }
}
