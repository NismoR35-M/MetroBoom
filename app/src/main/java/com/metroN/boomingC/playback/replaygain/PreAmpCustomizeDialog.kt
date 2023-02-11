package com.metroN.boomingC.playback.replaygain

import android.os.Bundle
import android.view.LayoutInflater
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import kotlin.math.abs
import com.metroN.boomingC.R
import com.metroN.boomingC.databinding.DialogPreAmpBinding
import com.metroN.boomingC.playback.PlaybackSettings
import com.metroN.boomingC.ui.ViewBindingDialogFragment

/**
 * aa [ViewBindingDialogFragment] that allows user configuration of the current [ReplayGainPreAmp].
 */
class PreAmpCustomizeDialog : ViewBindingDialogFragment<DialogPreAmpBinding>() {
    override fun onCreateBinding(inflater: LayoutInflater) = DialogPreAmpBinding.inflate(inflater)

    override fun onConfigDialog(builder: AlertDialog.Builder) {
        builder
            .setTitle(R.string.set_pre_amp)
            .setPositiveButton(R.string.lbl_ok) { _, _ ->
                val binding = requireBinding()
                PlaybackSettings.from(requireContext()).replayGainPreAmp =
                    ReplayGainPreAmp(binding.withTagsSlider.value, binding.withoutTagsSlider.value)
            }
            .setNeutralButton(R.string.lbl_reset) { _, _ ->
                PlaybackSettings.from(requireContext()).replayGainPreAmp = ReplayGainPreAmp(0f, 0f)
            }
            .setNegativeButton(R.string.lbl_cancel, null)
    }

    override fun onBindingCreated(binding: DialogPreAmpBinding, savedInstanceState: Bundle?) {
        if (savedInstanceState == null) {
            // First initialization, we need to supply the sliders with the values from
            // settings. After this, the sliders save their own state, so we do not need to
            // do any restore behavior.
            val preAmp = PlaybackSettings.from(requireContext()).replayGainPreAmp
            binding.withTagsSlider.value = preAmp.with
            binding.withoutTagsSlider.value = preAmp.without
        }

        // The listener always fires when the Slider restores it's own state, *except* when
        // it's at it's default value. Then it doesn't. Just initialize the ticker ourselves.
        updateTicker(binding.withTagsTicker, binding.withTagsSlider.value)
        binding.withTagsSlider.addOnChangeListener { _, value, _ ->
            updateTicker(binding.withTagsTicker, value)
        }

        updateTicker(binding.withoutTagsTicker, binding.withoutTagsSlider.value)
        binding.withoutTagsSlider.addOnChangeListener { _, value, _ ->
            updateTicker(binding.withoutTagsTicker, value)
        }
    }

    private fun updateTicker(ticker: TextView, valueDb: Float) {
        // It is more clear to prepend a +/- before the pre-amp value to make it easier to
        // gauge how much it may be increasing the volume, however android does not add +
        // to positive float values when formatting them in a string. Instead, add it ourselves.
        ticker.text =
            if (valueDb >= 0) {
                getString(R.string.fmt_db_pos, valueDb)
            } else {
                getString(R.string.fmt_db_neg, abs(valueDb))
            }
    }
}
