package com.metroN.boomingC.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.XmlRes
import androidx.appcompat.widget.Toolbar
import androidx.core.view.updatePadding
import androidx.navigation.fragment.findNavController
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.children
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.transition.MaterialSharedAxis
import com.metroN.boomingC.R
import com.metroN.boomingC.settings.ui.IntListPreference
import com.metroN.boomingC.settings.ui.IntListPreferenceDialog
import com.metroN.boomingC.settings.ui.PreferenceHeaderItemDecoration
import com.metroN.boomingC.settings.ui.WrappedDialogPreference
import com.metroN.boomingC.util.logD
import com.metroN.boomingC.util.systemBarInsetsCompat

/**
 * Shared [PreferenceFragmentCompat] used across all preference screens.
 */
abstract class BasePreferenceFragment(@XmlRes private val screen: Int) :
    PreferenceFragmentCompat() {
    /**
     * Called when the UI entry of a given [Preference] needs to be configured.
     * @param preference The [Preference] to configure.
     */
    open fun onSetupPreference(preference: Preference) {}

    /**
     * Called when an arbitrary [WrappedDialogPreference] needs to be opened.
     * @param preference The [WrappedDialogPreference] to open.
     */
    open fun onOpenDialogPreference(preference: WrappedDialogPreference) {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enterTransition = MaterialSharedAxis(MaterialSharedAxis.X, true)
        returnTransition = MaterialSharedAxis(MaterialSharedAxis.X, false)
        exitTransition = MaterialSharedAxis(MaterialSharedAxis.X, true)
        reenterTransition = MaterialSharedAxis(MaterialSharedAxis.X, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<AppBarLayout>(R.id.preferences_appbar).liftOnScrollTargetViewId =
            androidx.preference.R.id.recycler_view
        view.findViewById<Toolbar>(R.id.preferences_toolbar).apply {
            title = preferenceScreen.title
            setNavigationOnClickListener { findNavController().navigateUp() }
        }

        preferenceManager.onDisplayPreferenceDialogListener = this
        preferenceScreen.children.forEach(::setupPreference)

        logD("Fragment created")
    }

    override fun onCreateRecyclerView(
        inflater: LayoutInflater,
        parent: ViewGroup,
        savedInstanceState: Bundle?
    ) =
        super.onCreateRecyclerView(inflater, parent, savedInstanceState).apply {
            clipToPadding = false
            addItemDecoration(PreferenceHeaderItemDecoration(context))
            setOnApplyWindowInsetsListener { _, insets ->
                updatePadding(bottom = insets.systemBarInsetsCompat.bottom)
                insets
            }
        }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(screen, rootKey)
    }

    override fun onDisplayPreferenceDialog(preference: Preference) {
        when (preference) {
            is IntListPreference -> {
                // Copy the built-in preference dialog launching code into our project so
                // we can automatically use the provided preference class.
                val dialog = IntListPreferenceDialog.from(preference)
                @Suppress("Deprecation") dialog.setTargetFragment(this, 0)
                dialog.show(parentFragmentManager, IntListPreferenceDialog.TAG)
            }
            is WrappedDialogPreference -> {
                // These dialog preferences cannot launch on their own, delegate to
                // implementations.
                onOpenDialogPreference(preference)
            }
            else -> super.onDisplayPreferenceDialog(preference)
        }
    }

    private fun setupPreference(preference: Preference) {
        if (!preference.isVisible) {
            // Nothing to do.
            return
        }

        if (preference is PreferenceCategory) {
            preference.children.forEach(::setupPreference)
            return
        }

        onSetupPreference(preference)
    }
}
