package com.ivanovsky.passnotes.presentation.filepicker

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.observe
import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.data.entity.FileDescriptor
import com.ivanovsky.passnotes.databinding.FilePickerFragmentBinding
import com.ivanovsky.passnotes.domain.PermissionHelper
import com.ivanovsky.passnotes.injection.GlobalInjector.inject
import com.ivanovsky.passnotes.presentation.core.extensions.requireArgument
import com.ivanovsky.passnotes.presentation.core.extensions.setupActionBar
import com.ivanovsky.passnotes.presentation.core.extensions.showSnackbarMessage
import com.ivanovsky.passnotes.presentation.core.extensions.withArguments
import com.ivanovsky.passnotes.presentation.filepicker.Action.PICK_DIRECTORY
import com.ivanovsky.passnotes.presentation.filepicker.Action.PICK_FILE
import com.ivanovsky.passnotes.presentation.filepicker.model.FilePickerArgs
import org.koin.androidx.viewmodel.ext.android.viewModel

class FilePickerFragment : Fragment() {

    private val viewModel: FilePickerViewModel by viewModel()
    private val permissionHelper: PermissionHelper by inject()

    private var menu: Menu? = null
    private lateinit var args: FilePickerArgs

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)

        args = arguments?.getParcelable(ARGUMENTS) as? FilePickerArgs
            ?: requireArgument(ARGUMENTS)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        setupActionBar {
            title = when (args.action) {
                PICK_FILE -> getString(R.string.select_file)
                PICK_DIRECTORY -> getString(R.string.select_directory)
            }
            setDisplayHomeAsUpEnabled(true)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return FilePickerFragmentBinding.inflate(inflater)
            .also {
                it.lifecycleOwner = viewLifecycleOwner
                it.viewModel = viewModel
            }
            .root
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

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        this.menu = menu

        inflater.inflate(R.menu.base_done, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return if (item.itemId == R.id.menu_done) {
            viewModel.onDoneButtonClicked()
            true
        } else {
            super.onOptionsItemSelected(item)
        }
    }

    private fun subscribeToEvents() {
        viewModel.doneButtonVisibility.observe(viewLifecycleOwner) { isVisible ->
            setDoneButtonVisibility(isVisible)
        }
        viewModel.requestPermissionEvent.observe(viewLifecycleOwner) { permission ->
            requestPermission(permission)
        }
        viewModel.selectFileAndFinishEvent.observe(viewLifecycleOwner) { file ->
            selectFileAndFinish(file)
        }
        viewModel.showSnackbarMessageEvent.observe(viewLifecycleOwner) { message ->
            showSnackbarMessage(message)
        }
    }

    private fun setDoneButtonVisibility(isVisible: Boolean) {
        val menu = this.menu ?: return

        val item = menu.findItem(R.id.menu_done)
        item.isVisible = isVisible
    }

    private fun selectFileAndFinish(file: FileDescriptor) {
        val data = Intent().apply {
            putExtra(FilePickerActivity.EXTRA_RESULT, file)
        }

        requireActivity().apply {
            setResult(Activity.RESULT_OK, data)
            finish()
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