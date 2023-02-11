package com.metroN.boomingC.playback

import android.content.ActivityNotFoundException
import android.content.Intent
import android.media.audiofx.AudioEffect
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.Toolbar
import androidx.core.view.updatePadding
import androidx.fragment.app.activityViewModels
import dagger.hilt.android.AndroidEntryPoint
import com.metroN.boomingC.MainFragmentDirections
import com.metroN.boomingC.R
import com.metroN.boomingC.databinding.FragmentPlaybackPanelBinding
import com.metroN.boomingC.music.MusicParent
import com.metroN.boomingC.music.Song
import com.metroN.boomingC.playback.state.RepeatMode
import com.metroN.boomingC.playback.ui.StyledSeekBar
import com.metroN.boomingC.ui.MainNavigationAction
import com.metroN.boomingC.ui.NavigationViewModel
import com.metroN.boomingC.ui.ViewBindingFragment
import com.metroN.boomingC.util.collectImmediately
import com.metroN.boomingC.util.showToast
import com.metroN.boomingC.util.systemBarInsetsCompat

/**
 * A [ViewBindingFragment] more information about the currently playing song, alongside all
 * available controls.
 */
@AndroidEntryPoint
class PlaybackPanelFragment :
    ViewBindingFragment<FragmentPlaybackPanelBinding>(),
    Toolbar.OnMenuItemClickListener,
    StyledSeekBar.Listener {
    private val playbackModel: PlaybackViewModel by activityViewModels()
    private val navModel: NavigationViewModel by activityViewModels()
    private var equalizerLauncher: ActivityResultLauncher<Intent>? = null

    override fun onCreateBinding(inflater: LayoutInflater) =
        FragmentPlaybackPanelBinding.inflate(inflater)

    override fun onBindingCreated(
        binding: FragmentPlaybackPanelBinding,
        savedInstanceState: Bundle?
    ) {
        super.onBindingCreated(binding, savedInstanceState)

        // AudioEffect expects you to use startActivityForResult with the panel intent. There is no
        // contract analogue for this intent, so the generic contract is used instead.
        equalizerLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                // Nothing to do
            }

        // --- UI SETUP ---
        binding.root.setOnApplyWindowInsetsListener { view, insets ->
            val bars = insets.systemBarInsetsCompat
            view.updatePadding(top = bars.top, bottom = bars.bottom)
            insets
        }

        binding.playbackToolbar.apply {
            setNavigationOnClickListener { navModel.mainNavigateTo(MainNavigationAction.Collapse) }
            setOnMenuItemClickListener(this@PlaybackPanelFragment)
        }

        // Set up marquee on song information, alongside click handlers that navigate to each
        // respective item.
        binding.playbackSong.apply {
            isSelected = true
            setOnClickListener { playbackModel.song.value?.let(navModel::exploreNavigateTo) }
        }
        binding.playbackArtist.apply {
            isSelected = true
            setOnClickListener { navigateToCurrentArtist() }
        }
        binding.playbackAlbum.apply {
            isSelected = true
            setOnClickListener { navigateToCurrentAlbum() }
        }

        binding.playbackSeekBar.listener = this

        // Set up actions
        // TODO: Add better playback button accessibility
        binding.playbackRepeat.setOnClickListener { playbackModel.toggleRepeatMode() }
        binding.playbackSkipPrev.setOnClickListener { playbackModel.prev() }
        binding.playbackPlayPause.setOnClickListener { playbackModel.togglePlaying() }
        binding.playbackSkipNext.setOnClickListener { playbackModel.next() }
        binding.playbackShuffle.setOnClickListener { playbackModel.toggleShuffled() }

        // --- VIEWMODEL SETUP --
        collectImmediately(playbackModel.song, ::updateSong)
        collectImmediately(playbackModel.parent, ::updateParent)
        collectImmediately(playbackModel.positionDs, ::updatePosition)
        collectImmediately(playbackModel.repeatMode, ::updateRepeat)
        collectImmediately(playbackModel.isPlaying, ::updatePlaying)
        collectImmediately(playbackModel.isShuffled, ::updateShuffled)
    }

    override fun onDestroyBinding(binding: FragmentPlaybackPanelBinding) {
        equalizerLauncher = null
        binding.playbackToolbar.setOnMenuItemClickListener(null)
        // Marquee elements leak if they are not disabled when the views are destroyed.
        binding.playbackSong.isSelected = false
        binding.playbackArtist.isSelected = false
        binding.playbackAlbum.isSelected = false
    }

    override fun onMenuItemClick(item: MenuItem) =
        when (item.itemId) {
            R.id.action_open_equalizer -> {
                // Launch the system equalizer app, if possible.
                val equalizerIntent =
                    Intent(AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL)
                        // Provide audio session ID so the equalizer can show options for this app
                        // in particular.
                        .putExtra(
                            AudioEffect.EXTRA_AUDIO_SESSION, playbackModel.currentAudioSessionId)
                        // Signal music type so that the equalizer settings are appropriate for
                        // music playback.
                        .putExtra(AudioEffect.EXTRA_CONTENT_TYPE, AudioEffect.CONTENT_TYPE_MUSIC)
                try {
                    requireNotNull(equalizerLauncher) {
                            "Equalizer panel launcher was not available"
                        }
                        .launch(equalizerIntent)
                } catch (e: ActivityNotFoundException) {
                    requireContext().showToast(R.string.err_no_app)
                }
                true
            }
            R.id.action_go_artist -> {
                navigateToCurrentArtist()
                true
            }
            R.id.action_go_album -> {
                navigateToCurrentAlbum()
                true
            }
            R.id.action_song_detail -> {
                playbackModel.song.value?.let { song ->
                    navModel.mainNavigateTo(
                        MainNavigationAction.Directions(
                            MainFragmentDirections.actionShowDetails(song.uid)))
                }
                true
            }
            else -> false
        }

    override fun onSeekConfirmed(positionDs: Long) {
        playbackModel.seekTo(positionDs)
    }

    private fun updateSong(song: Song?) {
        if (song == null) {
            // Nothing to do.
            return
        }

        val binding = requireBinding()
        val context = requireContext()
        binding.playbackCover.bind(song)
        binding.playbackSong.text = song.resolveName(context)
        binding.playbackArtist.text = song.resolveArtistContents(context)
        binding.playbackAlbum.text = song.album.resolveName(context)
        binding.playbackSeekBar.durationDs = song.durationMs.msToDs()
    }

    private fun updateParent(parent: MusicParent?) {
        val binding = requireBinding()
        val context = requireContext()
        binding.playbackToolbar.subtitle =
            parent?.resolveName(context) ?: context.getString(R.string.lbl_all_songs)
    }

    private fun updatePosition(positionDs: Long) {
        requireBinding().playbackSeekBar.positionDs = positionDs
    }

    private fun updateRepeat(repeatMode: RepeatMode) {
        requireBinding().playbackRepeat.apply {
            setIconResource(repeatMode.icon)
            isActivated = repeatMode != RepeatMode.NONE
        }
    }

    private fun updatePlaying(isPlaying: Boolean) {
        requireBinding().playbackPlayPause.isActivated = isPlaying
    }

    private fun updateShuffled(isShuffled: Boolean) {
        requireBinding().playbackShuffle.isActivated = isShuffled
    }

    /** Navigate to one of the currently playing [Song]'s Artists. */
    private fun navigateToCurrentArtist() {
        val song = playbackModel.song.value ?: return
        navModel.exploreNavigateToParentArtist(song)
    }

    /** Navigate to the currently playing [Song]'s albums. */
    private fun navigateToCurrentAlbum() {
        val song = playbackModel.song.value ?: return
        navModel.exploreNavigateTo(song.album)
    }
}
