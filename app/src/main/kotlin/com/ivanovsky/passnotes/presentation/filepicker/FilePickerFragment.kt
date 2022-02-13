package com.ivanovsky.passnotes.presentation.filepicker

import android.os.Bundle
import android.view.*
import androidx.lifecycle.observe
import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.databinding.FilePickerFragmentBinding
import com.ivanovsky.passnotes.domain.PermissionHelper
import com.ivanovsky.passnotes.injection.GlobalInjector.inject
import com.ivanovsky.passnotes.presentation.core.FragmentWithDoneButton
import com.ivanovsky.passnotes.presentation.core.extensions.getMandatoryArgument
import com.ivanovsky.passnotes.presentation.core.extensions.setupActionBar
import com.ivanovsky.passnotes.presentation.core.extensions.showSnackbarMessage
import com.ivanovsky.passnotes.presentation.core.extensions.withArguments
import com.ivanovsky.passnotes.presentation.filepicker.Action.PICK_DIRECTORY
import com.ivanovsky.passnotes.presentation.filepicker.Action.PICK_FILE
import com.ivanovsky.passnotes.presentation.filepicker.model.FilePickerArgs
import org.koin.androidx.viewmodel.ext.android.viewModel

class FilePickerFragment : FragmentWithDoneButton() {

    private val viewModel: FilePickerViewModel by viewModel()
    private val permissionHelper: PermissionHelper by inject()
    private val args by lazy { getMandatoryArgument<FilePickerArgs>(ARGUMENTS) }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        setupActionBar {
            title = when (args.action) {
                PICK_FILE -> getString(R.string.select_file)
                PICK_DIRECTORY -> getString(R.string.select_directory)
            }
            setHomeAsUpIndicator(null)
            setDisplayHomeAsUpEnabled(true)
        }
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
                viewModel.navigateBack()
                true
            }
            else -> {
                super.onOptionsItemSelected(item)
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        subscribeToEvents()

        viewModel.start(
            args.action,
            args.rootFile,
            args.isBrowsingEnabled
        )
    }

    private fun subscribeToEvents() {
        viewModel.doneButtonVisibility.observe(viewLifecycleOwner) { isVisible ->
            setDoneButtonVisibility(isVisible)
        }
        viewModel.requestPermissionEvent.observe(viewLifecycleOwner) { permission ->
            requestPermission(permission)
        }
        viewModel.showSnackbarMessageEvent.observe(viewLifecycleOwner) { message ->
            showSnackbarMessage(message)
        }
    }

    private fun requestPermission(permission: String) {
        permissionHelper.requestPermission(this, permission, REQUEST_CODE_PERMISSION)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == REQUEST_CODE_PERMISSION) {
            viewModel.onPermissionResult(permissionHelper.isAllGranted(grantResults))
        }
    }

    companion object {

        private const val REQUEST_CODE_PERMISSION = 100

        private const val ARGUMENTS = "arguments"

        fun newInstance(
            args: FilePickerArgs
        ) = FilePickerFragment().withArguments {
            putParcelable(ARGUMENTS, args)
        }
    }
}