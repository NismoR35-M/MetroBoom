package com.metroN.boomingC.settings

import android.os.Bundle
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.google.android.material.transition.MaterialFadeThrough
import com.google.android.material.transition.MaterialSharedAxis
import dagger.hilt.android.AndroidEntryPoint
import com.metroN.boomingC.R
import com.metroN.boomingC.music.MusicViewModel
import com.metroN.boomingC.playback.PlaybackViewModel
import com.metroN.boomingC.settings.ui.WrappedDialogPreference
import com.metroN.boomingC.util.showToast

/**
 * The [PreferenceFragmentCompat] that displays the root settings list.
 */
@AndroidEntryPoint
class RootPreferenceFragment : BasePreferenceFragment(R.xml.preferences_root) {
    private val playbackModel: PlaybackViewModel by activityViewModels()
    private val musicModel: MusicViewModel by activityViewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enterTransition = MaterialFadeThrough()
        returnTransition = MaterialFadeThrough()
        exitTransition = MaterialFadeThrough()
        reenterTransition = MaterialSharedAxis(MaterialSharedAxis.X, false)
    }

    override fun onOpenDialogPreference(preference: WrappedDialogPreference) {
        if (preference.key == getString(R.string.set_key_music_dirs)) {
            findNavController().navigate(RootPreferenceFragmentDirections.goToMusicDirsDialog())
        }
    }

    override fun onPreferenceTreeClick(preference: Preference): Boolean {
        // Hook generic preferences to their specified preferences
        // TODO: These seem like good things to put into a side navigation view, if I choose to
        //  do one.
        when (preference.key) {
            getString(R.string.set_key_ui) -> {
                findNavController().navigate(RootPreferenceFragmentDirections.goToUiPreferences())
            }
            getString(R.string.set_key_personalize) -> {
                findNavController()
                    .navigate(RootPreferenceFragmentDirections.goToPersonalizePreferences())
            }
            getString(R.string.set_key_music) -> {
                findNavController()
                    .navigate(RootPreferenceFragmentDirections.goToMusicPreferences())
            }
            getString(R.string.set_key_audio) -> {
                findNavController()
                    .navigate(RootPreferenceFragmentDirections.goToAudioPreferences())
            }
            getString(R.string.set_key_reindex) -> musicModel.refresh()
            getString(R.string.set_key_rescan) -> musicModel.rescan()
            getString(R.string.set_key_save_state) -> {
                playbackModel.savePlaybackState { saved ->
                    // Use the nullable context, as we could try to show a toast when this
                    // fragment is no longer attached.
                    if (saved) {
                        context?.showToast(R.string.lbl_state_saved)
                    } else {
                        context?.showToast(R.string.err_did_not_save)
                    }
                }
            }
            getString(R.string.set_key_wipe_state) -> {
                playbackModel.wipePlaybackState { wiped ->
                    if (wiped) {
                        // Use the nullable context, as we could try to show a toast when this
                        // fragment is no longer attached.
                        context?.showToast(R.string.lbl_state_wiped)
                    } else {
                        context?.showToast(R.string.err_did_not_wipe)
                    }
                }
            }
            getString(R.string.set_key_restore_state) ->
                playbackModel.tryRestorePlaybackState { restored ->
                    if (restored) {
                        // Use the nullable context, as we could try to show a toast when this
                        // fragment is no longer attached.
                        context?.showToast(R.string.lbl_state_restored)
                    } else {
                        context?.showToast(R.string.err_did_not_restore)
                    }
                }
            else -> return super.onPreferenceTreeClick(preference)
        }

        return true
    }
}
