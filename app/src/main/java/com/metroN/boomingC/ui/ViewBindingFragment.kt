package com.metroN.boomingC.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.viewbinding.ViewBinding
import com.metroN.boomingC.util.logD
import com.metroN.boomingC.util.unlikelyToBeNull

abstract class ViewBindingFragment<VB : ViewBinding> : Fragment() {
    private var _binding: VB? = null

    /**
     * Inflate the [ViewBinding] during [onCreateView].
     * @param inflater The [LayoutInflater] to inflate the [ViewBinding] with.
     * @return A new [ViewBinding] instance.
     * @see onCreateView
     */
    protected abstract fun onCreateBinding(inflater: LayoutInflater): VB

    /**
     * Configure the newly-inflated [ViewBinding] during [onViewCreated].
     * @param binding The [ViewBinding] to configure.
     * @param savedInstanceState The previously saved state of the UI.
     * @see onViewCreated
     */
    protected open fun onBindingCreated(binding: VB, savedInstanceState: Bundle?) {}

    /**
     * Free memory held by the [ViewBinding] during [onDestroyView]
     * @param binding The [ViewBinding] to release.
     * @see onDestroyView
     */
    protected open fun onDestroyBinding(binding: VB) {}

    /** The [ViewBinding], or null if it has not been inflated yet. */
    protected val binding: VB?
        get() = _binding

    /**
     * Get the [ViewBinding] under the assumption that it has been inflated.
     * @return The currently-inflated [ViewBinding].
     * @throws IllegalStateException if the [ViewBinding] is not inflated.
     */
    protected fun requireBinding() =
        requireNotNull(_binding) {
            "ViewBinding was available. Fragment should be a valid state " +
                "right now, but instead it was ${lifecycle.currentState}"
        }

    final override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = onCreateBinding(inflater).also { _binding = it }.root

    final override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Configure binding
        onBindingCreated(requireBinding(), savedInstanceState)
        logD("Fragment created")
    }

    final override fun onDestroyView() {
        super.onDestroyView()
        onDestroyBinding(unlikelyToBeNull(_binding))
        // Clear binding
        _binding = null
        logD("Fragment destroyed")
    }
}
