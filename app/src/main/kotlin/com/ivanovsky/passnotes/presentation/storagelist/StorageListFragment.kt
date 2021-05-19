package com.ivanovsky.passnotes.presentation.storagelist

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.observe
import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.data.entity.FSAuthority
import com.ivanovsky.passnotes.data.entity.FileDescriptor
import com.ivanovsky.passnotes.data.repository.file.AuthType
import com.ivanovsky.passnotes.data.repository.file.FileSystemResolver
import com.ivanovsky.passnotes.databinding.StorageListFragmentBinding
import com.ivanovsky.passnotes.injection.GlobalInjector.inject
import com.ivanovsky.passnotes.presentation.core.extensions.requireArgument
import com.ivanovsky.passnotes.presentation.core.extensions.setupActionBar
import com.ivanovsky.passnotes.presentation.core.extensions.withArguments
import com.ivanovsky.passnotes.presentation.filepicker.FilePickerActivity
import com.ivanovsky.passnotes.presentation.filepicker.model.FilePickerArgs
import org.koin.androidx.viewmodel.ext.android.viewModel

class StorageListFragment : Fragment() {

    private val viewModel: StorageListViewModel by viewModel()
    private val fileSystemResolver: FileSystemResolver by inject()
    private var requestedFSAuthority: FSAuthority? = null

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        setupActionBar {
            title = getString(R.string.select_storage)
            setDisplayHomeAsUpEnabled(true)
        }
    }

    override fun onStart() {
        super.onStart()
        viewModel.onScreenStart()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return StorageListFragmentBinding.inflate(inflater)
            .also {
                it.lifecycleOwner = viewLifecycleOwner
                it.viewModel = viewModel
            }
            .root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        subscribeToEvents()

        val requiredAction = arguments?.getSerializable(ARG_REQUIRED_ACTION) as? Action
            ?: requireArgument(ARG_REQUIRED_ACTION)

        viewModel.loadData(requiredAction)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            REQUEST_CODE_PICK_FILE -> {
                if (resultCode == Activity.RESULT_OK) {
                    val extras = data?.extras ?: return
                    val file =
                        extras.getParcelable<FileDescriptor>(FilePickerActivity.EXTRA_RESULT) ?: return

                    viewModel.onFilePickedByPicker(file)
                }
            }
            REQUEST_CODE_INTERNAL_AUTHENTICATION -> {
                val fsAuthority = requestedFSAuthority ?: return
                val authenticator =
                    fileSystemResolver.resolveProvider(fsAuthority).authenticator ?: return

                val newFsAuthority = data?.let {
                    authenticator.getAuthorityFromResult(it)
                }

                if (resultCode == Activity.RESULT_OK && newFsAuthority != null) {
                    viewModel.onInternalAuthSuccess(newFsAuthority)
                } else {
                    viewModel.onInternalAuthFailed()
                }
            }
        }
    }

    private fun subscribeToEvents() {
        viewModel.selectFileEvent.observe(viewLifecycleOwner) { selectedFile ->
            onFileSelected(selectedFile)
        }
        viewModel.showAuthActivityEvent.observe(viewLifecycleOwner) { fsAuthority ->
            showAuthActivity(fsAuthority)
        }
        viewModel.showFilePickerScreenEvent.observe(viewLifecycleOwner) { args ->
            showFilePickerScreen(args)
        }
    }

    private fun onFileSelected(file: FileDescriptor) {
        val data = Intent().apply {
            putExtra(StorageListActivity.EXTRA_RESULT, file)
        }

        requireActivity().apply {
            setResult(Activity.RESULT_OK, data)
            finish()
        }
    }

    private fun showAuthActivity(fsAuthority: FSAuthority) {
        requestedFSAuthority = fsAuthority

        val authenticator = fileSystemResolver.resolveProvider(fsAuthority).authenticator
        val authType = authenticator?.getAuthType()
        if (authType == AuthType.INTERNAL) {
            startActivityForResult(
                authenticator.getAuthIntent(requireContext()),
                REQUEST_CODE_INTERNAL_AUTHENTICATION
            )
        } else if (authType == AuthType.EXTERNAL) {
            authenticator.startAuthActivity(requireContext())
        }
    }

    private fun showFilePickerScreen(args: FilePickerArgs) {
        val intent = FilePickerActivity.createStartIntent(
            context = requireContext(),
            args = args
        )

        startActivityForResult(intent, REQUEST_CODE_PICK_FILE)
    }

    companion object {

        private const val REQUEST_CODE_PICK_FILE = 100
        private const val REQUEST_CODE_INTERNAL_AUTHENTICATION = 101

        private const val ARG_REQUIRED_ACTION = "requiredAction"

        fun newInstance(requiredAction: Action) = StorageListFragment().withArguments {
            putSerializable(ARG_REQUIRED_ACTION, requiredAction)
        }
    }
}