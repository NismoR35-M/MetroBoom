package com.metroN.boomingC.music.storage

import android.content.ActivityNotFoundException
import android.net.Uri
import android.os.Bundle
import android.os.storage.StorageManager
import android.provider.DocumentsContract
import android.view.LayoutInflater
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.view.ViewCompat
import androidx.core.view.isVisible
import dagger.hilt.android.AndroidEntryPoint
import com.metroN.boomingC.BuildConfig
import com.metroN.boomingC.R
import com.metroN.boomingC.databinding.DialogMusicDirsBinding
import com.metroN.boomingC.music.MusicSettings
import com.metroN.boomingC.ui.ViewBindingDialogFragment
import com.metroN.boomingC.util.getSystemServiceCompat
import com.metroN.boomingC.util.logD
import com.metroN.boomingC.util.showToast

/**
 * Dialog that manages the music dirs setting.
 */
@AndroidEntryPoint
class MusicDirsDialog :
    ViewBindingDialogFragment<DialogMusicDirsBinding>(), DirectoryAdapter.Listener {
    private val dirAdapter = DirectoryAdapter(this)
    private var openDocumentTreeLauncher: ActivityResultLauncher<Uri?>? = null
    private var storageManager: StorageManager? = null

    override fun onCreateBinding(inflater: LayoutInflater) =
        DialogMusicDirsBinding.inflate(inflater)

    override fun onConfigDialog(builder: AlertDialog.Builder) {
        builder
            .setTitle(R.string.set_dirs)
            .setNegativeButton(R.string.lbl_cancel, null)
            .setPositiveButton(R.string.lbl_save) { _, _ ->
                val settings = MusicSettings.from(requireContext())
                val newDirs = MusicDirectories(dirAdapter.dirs, isUiModeInclude(requireBinding()))
                if (settings.musicDirs != newDirs) {
                    logD("Committing changes")
                    settings.musicDirs = newDirs
                }
            }
    }

    override fun onBindingCreated(binding: DialogMusicDirsBinding, savedInstanceState: Bundle?) {
        val context = requireContext()
        val storageManager =
            context.getSystemServiceCompat(StorageManager::class).also { storageManager = it }

        openDocumentTreeLauncher =
            registerForActivityResult(
                ActivityResultContracts.OpenDocumentTree(), ::addDocumentTreeUriToDirs)

        binding.dirsAdd.apply {
            ViewCompat.setTooltipText(this, contentDescription)
            setOnClickListener {
                logD("Opening launcher")
                val launcher =
                    requireNotNull(openDocumentTreeLauncher) {
                        "Document tree launcher was not available"
                    }

                try {
                    launcher.launch(null)
                } catch (e: ActivityNotFoundException) {
                    // User doesn't have a capable file manager.
                    requireContext().showToast(R.string.err_no_app)
                }
            }
        }

        binding.dirsRecycler.apply {
            adapter = dirAdapter
            itemAnimator = null
        }

        var dirs = MusicSettings.from(context).musicDirs
        if (savedInstanceState != null) {
            val pendingDirs = savedInstanceState.getStringArrayList(KEY_PENDING_DIRS)
            if (pendingDirs != null) {
                dirs =
                    MusicDirectories(
                        pendingDirs.mapNotNull {
                            Directory.fromDocumentTreeUri(storageManager, it)
                        },
                        savedInstanceState.getBoolean(KEY_PENDING_MODE))
            }
        }

        dirAdapter.addAll(dirs.dirs)
        requireBinding().dirsEmpty.isVisible = dirs.dirs.isEmpty()

        binding.folderModeGroup.apply {
            check(
                if (dirs.shouldInclude) {
                    R.id.dirs_mode_include
                } else {
                    R.id.dirs_mode_exclude
                })

            updateMode()
            addOnButtonCheckedListener { _, _, _ -> updateMode() }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putStringArrayList(
            KEY_PENDING_DIRS, ArrayList(dirAdapter.dirs.map { it.toString() }))
        outState.putBoolean(KEY_PENDING_MODE, isUiModeInclude(requireBinding()))
    }

    override fun onDestroyBinding(binding: DialogMusicDirsBinding) {
        super.onDestroyBinding(binding)
        storageManager = null
        openDocumentTreeLauncher = null
        binding.dirsRecycler.adapter = null
    }

    override fun onRemoveDirectory(dir: Directory) {
        dirAdapter.remove(dir)
        requireBinding().dirsEmpty.isVisible = dirAdapter.dirs.isEmpty()
    }

    /**
     * Add a Document Tree [Uri] chosen by the user to the current [MusicDirectories] instance.
     * @param uri The document tree [Uri] to add, chosen by the user. Will do nothing if the [Uri]
     * is null or not valid.
     */
    private fun addDocumentTreeUriToDirs(uri: Uri?) {
        if (uri == null) {
            // A null URI means that the user left the file picker without picking a directory
            logD("No URI given (user closed the dialog)")
            return
        }

        // Convert the document tree URI into it's relative path form, which can then be
        // parsed into a Directory instance.
        val docUri =
            DocumentsContract.buildDocumentUriUsingTree(
                uri, DocumentsContract.getTreeDocumentId(uri))
        val treeUri = DocumentsContract.getTreeDocumentId(docUri)
        val dir =
            Directory.fromDocumentTreeUri(
                requireNotNull(storageManager) { "StorageManager was not available" }, treeUri)

        if (dir != null) {
            dirAdapter.add(dir)
            requireBinding().dirsEmpty.isVisible = false
        } else {
            requireContext().showToast(R.string.err_bad_dir)
        }
    }

    private fun updateMode() {
        val binding = requireBinding()
        if (isUiModeInclude(binding)) {
            binding.dirsModeExclude.icon = null
            binding.dirsModeInclude.setIconResource(R.drawable.ic_check_24)
            binding.dirsModeDesc.setText(R.string.set_dirs_mode_include_desc)
        } else {
            binding.dirsModeExclude.setIconResource(R.drawable.ic_check_24)
            binding.dirsModeInclude.icon = null
            binding.dirsModeDesc.setText(R.string.set_dirs_mode_exclude_desc)
        }
    }

    /** Get if the UI has currently configured [MusicDirectories.shouldInclude] to be true. */
    private fun isUiModeInclude(binding: DialogMusicDirsBinding) =
        binding.folderModeGroup.checkedButtonId == R.id.dirs_mode_include

    private companion object {
        const val KEY_PENDING_DIRS = BuildConfig.APPLICATION_ID + ".key.PENDING_DIRS"
        const val KEY_PENDING_MODE = BuildConfig.APPLICATION_ID + ".key.SHOULD_INCLUDE"
    }
}
