package com.ivanovsky.passnotes.presentation.storagelist

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.data.entity.FSAuthority
import com.ivanovsky.passnotes.data.repository.file.AuthType
import com.ivanovsky.passnotes.data.repository.file.FileSystemResolver
import com.ivanovsky.passnotes.databinding.StorageListFragmentBinding
import com.ivanovsky.passnotes.injection.GlobalInjector.inject
import com.ivanovsky.passnotes.presentation.core.BaseFragment
import com.ivanovsky.passnotes.presentation.core.adapter.ViewModelsAdapter
import com.ivanovsky.passnotes.presentation.core.extensions.getMandatoryArgument
import com.ivanovsky.passnotes.presentation.core.extensions.setViewModels
import com.ivanovsky.passnotes.presentation.core.extensions.setupActionBar
import com.ivanovsky.passnotes.presentation.core.extensions.withArguments
import com.ivanovsky.passnotes.util.FileUtils.DEFAULT_DB_NAME
import com.ivanovsky.passnotes.util.IntentUtils.newCreateFileIntent
import com.ivanovsky.passnotes.util.IntentUtils.newOpenFileIntent

class StorageListFragment : BaseFragment() {

    private val fileSystemResolver: FileSystemResolver by inject()

    private val viewModel: StorageListViewModel by lazy {
        ViewModelProvider(
            this,
            StorageListViewModel.Factory(
                args = getMandatoryArgument(ARGUMENTS)
            )
        )
            .get(StorageListViewModel::class.java)
    }

    private lateinit var binding: StorageListFragmentBinding

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        setupActionBar {
            title = getString(R.string.select_storage)
            setHomeAsUpIndicator(null)
            setDisplayHomeAsUpEnabled(true)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_CODE_PICK_FILE) {
            if (resultCode == Activity.RESULT_OK) {
                val uri = data?.data ?: return
                viewModel.onExternalStorageFileSelected(uri)
            } else {
                viewModel.onExternalStorageFileSelectionCanceled()
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = StorageListFragmentBinding.inflate(inflater)
            .also {
                it.lifecycleOwner = viewLifecycleOwner
                it.viewModel = viewModel
            }

        binding.recyclerView.adapter = ViewModelsAdapter(
            lifecycleOwner = viewLifecycleOwner,
            viewTypes = viewModel.viewTypes
        )

        return binding.root
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                viewModel.navigateBack()
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onStart() {
        super.onStart()
        viewModel.onScreenStart()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        subscribeToLiveData()
        subscribeToLiveEvents()
    }

    private fun subscribeToLiveData() {
        viewModel.cellViewModels.observe(viewLifecycleOwner) { viewModels ->
            binding.recyclerView.setViewModels(viewModels)
        }
    }

    private fun subscribeToLiveEvents() {
        viewModel.showAuthActivityEvent.observe(viewLifecycleOwner) { fsAuthority ->
            showAuthActivity(fsAuthority)
        }
        viewModel.showSystemFilePickerEvent.observe(viewLifecycleOwner) {
            showSystemFilePicker()
        }
        viewModel.showSystemFileCreatorEvent.observe(viewLifecycleOwner) {
            showSystemFileCreator()
        }
    }

    private fun showAuthActivity(fsAuthority: FSAuthority) {
        val authenticator = fileSystemResolver.resolveProvider(fsAuthority).authenticator
        if (authenticator.getAuthType() == AuthType.EXTERNAL) {
            authenticator.startAuthActivity(requireContext())
        }
    }

    private fun showSystemFilePicker() {
        startActivityForResult(newOpenFileIntent(), REQUEST_CODE_PICK_FILE)
    }

    private fun showSystemFileCreator() {
        startActivityForResult(newCreateFileIntent(DEFAULT_DB_NAME), REQUEST_CODE_PICK_FILE)
    }

    companion object {

        private const val REQUEST_CODE_PICK_FILE = 1

        private const val ARGUMENTS = "arguments"

        fun newInstance(args: StorageListArgs) = StorageListFragment()
            .withArguments {
                putParcelable(ARGUMENTS, args)
            }
    }
}