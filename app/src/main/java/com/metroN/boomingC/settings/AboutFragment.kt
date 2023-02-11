package com.metroN.boomingC.settings

import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import androidx.core.net.toUri
import androidx.core.view.updatePadding
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.transition.MaterialFadeThrough
import dagger.hilt.android.AndroidEntryPoint
import com.metroN.boomingC.BuildConfig
import com.metroN.boomingC.R
import com.metroN.boomingC.databinding.FragmentAboutBinding
import com.metroN.boomingC.music.MusicViewModel
import com.metroN.boomingC.playback.formatDurationMs
import com.metroN.boomingC.ui.ViewBindingFragment
import com.metroN.boomingC.util.collectImmediately
import com.metroN.boomingC.util.logD
import com.metroN.boomingC.util.showToast
import com.metroN.boomingC.util.systemBarInsetsCompat

/**
 * A [ViewBindingFragment] that displays information about the app and the current music library.
 */
@AndroidEntryPoint
class AboutFragment : ViewBindingFragment<FragmentAboutBinding>() {
    private val musicModel: MusicViewModel by activityViewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enterTransition = MaterialFadeThrough()
        exitTransition = MaterialFadeThrough()
    }

    override fun onCreateBinding(inflater: LayoutInflater) = FragmentAboutBinding.inflate(inflater)

    override fun onBindingCreated(binding: FragmentAboutBinding, savedInstanceState: Bundle?) {
        super.onBindingCreated(binding, savedInstanceState)

        // --- UI SETUP ---
        binding.aboutToolbar.setNavigationOnClickListener { findNavController().navigateUp() }
        binding.aboutContents.setOnApplyWindowInsetsListener { view, insets ->
            view.updatePadding(bottom = insets.systemBarInsetsCompat.bottom)
            insets
        }

        binding.aboutVersion.text = BuildConfig.VERSION_NAME
        binding.aboutCode.setOnClickListener { openLinkInBrowser(LINK_SOURCE) }
        binding.aboutWiki.setOnClickListener { openLinkInBrowser(LINK_WIKI) }
        binding.aboutLicenses.setOnClickListener { openLinkInBrowser(LINK_LICENSES) }
        binding.aboutAuthor.setOnClickListener { openLinkInBrowser(LINK_AUTHOR) }

        // VIEWMODEL SETUP
        collectImmediately(musicModel.statistics, ::updateStatistics)
    }

    private fun updateStatistics(statistics: MusicViewModel.Statistics?) {
        val binding = requireBinding()
        binding.aboutSongCount.text = getString(R.string.fmt_lib_song_count, statistics?.songs ?: 0)
        requireBinding().aboutAlbumCount.text =
            getString(R.string.fmt_lib_album_count, statistics?.albums ?: 0)
        requireBinding().aboutArtistCount.text =
            getString(R.string.fmt_lib_artist_count, statistics?.artists ?: 0)
        requireBinding().aboutGenreCount.text =
            getString(R.string.fmt_lib_genre_count, statistics?.genres ?: 0)
        binding.aboutTotalDuration.text =
            getString(
                R.string.fmt_lib_total_duration,
                (statistics?.durationMs ?: 0).formatDurationMs(false))
    }

    /**
     * Open the given URI in a web browser.
     * @param uri The URL to open.
     */
    private fun openLinkInBrowser(uri: String) {
        logD("Opening $uri")
        val context = requireContext()
        val browserIntent =
            Intent(Intent.ACTION_VIEW, uri.toUri()).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11 seems to now handle the app chooser situations on its own now
            // [along with adding a new permission that breaks the old manual code], so
            // we just do a typical activity launch.
            try {
                context.startActivity(browserIntent)
            } catch (e: ActivityNotFoundException) {
                // No app installed to open the link
                context.showToast(R.string.err_no_app)
            }
        } else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            // On older versions of android, opening links from an ACTION_VIEW intent might
            // not work in all cases, especially when no default app was set. If that is the
            // case, we will try to manually handle these cases before we try to launch the
            // browser.
            val pkgName =
                context.packageManager
                    .resolveActivity(browserIntent, PackageManager.MATCH_DEFAULT_ONLY)
                    ?.run { activityInfo.packageName }

            if (pkgName != null) {
                if (pkgName == "android") {
                    // No default browser [Must open app chooser, may not be supported]
                    openAppChooser(browserIntent)
                } else
                    try {
                        browserIntent.setPackage(pkgName)
                        startActivity(browserIntent)
                    } catch (e: ActivityNotFoundException) {
                        // Not a browser but an app chooser
                        browserIntent.setPackage(null)
                        openAppChooser(browserIntent)
                    }
            } else {
                // No app installed to open the link
                context.showToast(R.string.err_no_app)
            }
        }
    }

    /**
     * Open an app chooser for a given [Intent].
     * @param intent The [Intent] to show an app chooser for.
     */
    private fun openAppChooser(intent: Intent) {
        val chooserIntent =
            Intent(Intent.ACTION_CHOOSER)
                .putExtra(Intent.EXTRA_INTENT, intent)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(chooserIntent)
    }

    private companion object {
        /** The URL to the source code. */
        const val LINK_SOURCE = "https://github.com/OxygenCobalt/Auxio"
        /** The URL to the app wiki. */
        const val LINK_WIKI = "$LINK_SOURCE/wiki"
        /** The URL to the licenses wiki page. */
        const val LINK_LICENSES = "$LINK_WIKI/Licenses"
        /** The URL to the app author. */
        const val LINK_AUTHOR = "https://github.com/OxygenCobalt"
    }
}
