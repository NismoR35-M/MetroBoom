package com.metroN.boomingC.home.tabs

import android.content.Context
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.metroN.boomingC.R
import com.metroN.boomingC.music.MusicMode
import com.metroN.boomingC.util.logD

/**
 * A [TabLayoutMediator.TabConfigurationStrategy] that uses larger/smaller tab configurations
 * depending on the screen configuration.
 * @param context [Context] required to obtain window information
 * @param tabs Current tab configuration from settings
 */
class AdaptiveTabStrategy(context: Context, private val tabs: List<MusicMode>) :
    TabLayoutMediator.TabConfigurationStrategy {
    private val width = context.resources.configuration.smallestScreenWidthDp

    override fun onConfigureTab(tab: TabLayout.Tab, position: Int) {
        val icon: Int
        val string: Int

        when (tabs[position]) {
            MusicMode.SONGS -> {
                icon = R.drawable.ic_song_24
                string = R.string.lbl_songs
            }
            MusicMode.ALBUMS -> {
                icon = R.drawable.ic_album_24
                string = R.string.lbl_albums
            }
            MusicMode.ARTISTS -> {
                icon = R.drawable.ic_artist_24
                string = R.string.lbl_artists
            }
            MusicMode.GENRES -> {
                icon = R.drawable.ic_genre_24
                string = R.string.lbl_genres
            }
        }

        // Use expected sw* size thresholds when choosing a configuration.
        when {
            // On small screens, only display an icon.
            width < 370 -> {
                logD("Using icon-only configuration")
                tab.setIcon(icon).setContentDescription(string)
            }
            // On large screens, display an icon and text.
            width < 600 -> {
                logD("Using text-only configuration")
                tab.setText(string)
            }
            // On medium-size screens, display text.
            else -> {
                logD("Using icon-and-text configuration")
                tab.setIcon(icon).setText(string)
            }
        }
    }
}
