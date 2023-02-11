package com.metroN.boomingC.detail

import android.os.Bundle
import android.text.format.Formatter
import android.view.LayoutInflater
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isInvisible
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import dagger.hilt.android.AndroidEntryPoint
import com.metroN.boomingC.R
import com.metroN.boomingC.databinding.DialogSongDetailBinding
import com.metroN.boomingC.music.Song
import com.metroN.boomingC.music.metadata.AudioInfo
import com.metroN.boomingC.playback.formatDurationMs
import com.metroN.boomingC.ui.ViewBindingDialogFragment
import com.metroN.boomingC.util.collectImmediately

/**
 * A [ViewBindingDialogFragment] that shows information about a Song.
 */
@AndroidEntryPoint
class SongDetailDialog : ViewBindingDialogFragment<DialogSongDetailBinding>() {
    private val detailModel: DetailViewModel by activityViewModels()
    // Information about what song to display is initially within the navigation arguments
    // as a UID, as that is the only safe way to parcel an song.
    private val args: SongDetailDialogArgs by navArgs()

    override fun onCreateBinding(inflater: LayoutInflater) =
        DialogSongDetailBinding.inflate(inflater)

    override fun onConfigDialog(builder: AlertDialog.Builder) {
        super.onConfigDialog(builder)
        builder.setTitle(R.string.lbl_props).setPositiveButton(R.string.lbl_ok, null)
    }

    override fun onBindingCreated(binding: DialogSongDetailBinding, savedInstanceState: Bundle?) {
        super.onBindingCreated(binding, savedInstanceState)
        // DetailViewModel handles most initialization from the navigation argument.
        detailModel.setSongUid(args.itemUid)
        collectImmediately(detailModel.currentSong, detailModel.songAudioInfo, ::updateSong)
    }

    private fun updateSong(song: Song?, info: AudioInfo?) {
        if (song == null) {
            // Song we were showing no longer exists.
            findNavController().navigateUp()
            return
        }

        val binding = requireBinding()
        if (info != null) {
            // Finished loading song audio info, populate and show the list of Song information.
            binding.detailLoading.isInvisible = true
            binding.detailContainer.isInvisible = false

            val context = requireContext()
            binding.detailFileName.setText(song.path.name)
            binding.detailRelativeDir.setText(song.path.parent.resolveName(context))
            binding.detailFormat.setText(info.resolvedMimeType.resolveName(context))
            binding.detailSize.setText(Formatter.formatFileSize(context, song.size))
            binding.detailDuration.setText(song.durationMs.formatDurationMs(true))

            if (info.bitrateKbps != null) {
                binding.detailBitrate.setText(getString(R.string.fmt_bitrate, info.bitrateKbps))
            } else {
                binding.detailBitrate.setText(R.string.def_bitrate)
            }

            if (info.sampleRateHz != null) {
                binding.detailSampleRate.setText(
                    getString(R.string.fmt_sample_rate, info.sampleRateHz))
            } else {
                binding.detailSampleRate.setText(R.string.def_sample_rate)
            }
        } else {
            // Loading is still on-going, don't show anything yet.
            binding.detailLoading.isInvisible = false
            binding.detailContainer.isInvisible = true
        }
    }
}
