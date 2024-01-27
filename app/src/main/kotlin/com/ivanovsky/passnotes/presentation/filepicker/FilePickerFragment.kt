package com.ivanovsky.passnotes.presentation.filepicker

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.databinding.FilePickerFragmentBinding
import com.ivanovsky.passnotes.domain.PermissionHelper
import com.ivanovsky.passnotes.domain.entity.SystemPermission
import com.ivanovsky.passnotes.injection.GlobalInjector.inject
import com.ivanovsky.passnotes.presentation.core.FragmentWithDoneButton
import com.ivanovsky.passnotes.presentation.core.dialog.AllFilesPermissionDialog
import com.ivanovsky.passnotes.presentation.core.extensions.getMandatoryArgument
import com.ivanovsky.passnotes.presentation.core.extensions.requestSystemPermission
import com.ivanovsky.passnotes.presentation.core.extensions.setupActionBar
import com.ivanovsky.passnotes.presentation.core.extensions.showSnackbarMessage
import com.ivanovsky.passnotes.presentation.core.extensions.withArguments
import com.ivanovsky.passnotes.presentation.core.permission.PermissionRequestResultReceiver
import com.ivanovsky.passnotes.presentation.filepicker.Action.PICK_DIRECTORY
import com.ivanovsky.passnotes.presentation.filepicker.Action.PICK_FILE
import timber.log.Timber

class FilePickerFragment :
    FragmentWithDoneButton(),
    PermissionRequestResultReceiver {

    private val viewModel: FilePickerViewModel by lazy {
        ViewModelProvider(
            this,
            FilePickerViewModel.Factory(
                args = getMandatoryArgument(ARGUMENTS)
            )
        )
            .get(FilePickerViewModel::class.java)
    }
    private val permissionHelper: PermissionHelper by inject()
    private val args by lazy { getMandatoryArgument<FilePickerArgs>(ARGUMENTS) }

    override fun onPermissionRequestResult(permission: SystemPermission, isGranted: Boolean) {
        when (permission) {
            SystemPermission.ALL_FILES_PERMISSION -> viewModel.onPermissionResult(isGranted)
            SystemPermission.SDCARD_PERMISSION -> viewModel.onPermissionResult(isGranted)
            else -> Timber.e("Invalid permission received: $permission")
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == SystemPermission.ALL_FILES_PERMISSION.requestCode) {
            val isGranted =
                permissionHelper.isPermissionGranted(SystemPermission.ALL_FILES_PERMISSION)

            viewModel.onPermissionResult(isGranted)
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        setupActionBar {
            title = when (args.action) {
                PICK_FILE -> getString(R.string.select_file)
                PICK_DIRECTORY -> getString(R.string.select_directory)
            }
            setHomeAsUpIndicator(R.drawable.ic_close_24dp)
            setDisplayHomeAsUpEnabled(true)
        }
    }

    override fun onBackPressed(): Boolean {
        viewModel.onBackClicked()
        return true
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return FilePickerFragmentBinding.inflate(inflater)
            .also {
                it.lifecycleOwner = viewLifecycleOwner
                it.viewModel = viewModel
            }
            .root
    }

    override fun onDoneMenuClicked() {
        viewModel.onDoneButtonClicked()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                viewModel.navigateToPreviousScreen()
                true
            }

            else -> {
                super.onOptionsItemSelected(item)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        viewModel.start()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        subscribeToEvents()
    }

    private fun subscribeToEvents() {
        viewModel.isDoneButtonVisible.observe(viewLifecycleOwner) { isVisible ->
            setDoneButtonVisibility(isVisible)
        }
        viewModel.requestPermissionEvent.observe(viewLifecycleOwner) { permission ->
            requestSystemPermission(permission)
        }
        viewModel.showSnackbarMessageEvent.observe(viewLifecycleOwner) { message ->
            showSnackbarMessage(message)
        }
        viewModel.showAllFilePermissionDialogEvent.observe(viewLifecycleOwner) {
            showAllFilePermissionDialog()
        }
    }

    private fun showAllFilePermissionDialog() {
        val dialog = AllFilesPermissionDialog.newInstance()
            .apply {
                onPositiveClicked = {
                    requestSystemPermission(SystemPermission.ALL_FILES_PERMISSION)
                }
                onNegativeClicked = {
                    viewModel.navigateToPreviousScreen()
                }
            }
        dialog.show(childFragmentManager, AllFilesPermissionDialog.TAG)
    }

    companion object {

        private const val ARGUMENTS = "arguments"

        fun newInstance(
            args: FilePickerArgs
        ) = FilePickerFragment().withArguments {
            putParcelable(ARGUMENTS, args)
        }
    }
}