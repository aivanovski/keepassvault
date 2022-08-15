package com.ivanovsky.passnotes.presentation.debugmenu

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.AdapterView.OnItemSelectedListener
import android.widget.ArrayAdapter
import androidx.lifecycle.observe
import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.data.entity.FSType
import com.ivanovsky.passnotes.databinding.DebugMenuFragmentBinding
import com.ivanovsky.passnotes.presentation.core.BaseFragment
import com.ivanovsky.passnotes.presentation.core.extensions.setupActionBar
import com.ivanovsky.passnotes.presentation.core.extensions.showSnackbarMessage
import com.ivanovsky.passnotes.util.FileUtils.DEFAULT_DB_NAME
import com.ivanovsky.passnotes.util.IntentUtils.newCreateFileIntent
import com.ivanovsky.passnotes.util.IntentUtils.newOpenFileIntent
import org.koin.androidx.viewmodel.ext.android.viewModel

class DebugMenuFragment : BaseFragment() {

    private val viewModel: DebugMenuViewModel by viewModel()

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        setupActionBar {
            title = getString(R.string.debug_menu)
            setHomeAsUpIndicator(null)
            setDisplayHomeAsUpEnabled(true)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_CODE_PICK_FILE && resultCode == Activity.RESULT_OK) {
            val uri = data?.data ?: return
            viewModel.onFilePicked(uri)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = DebugMenuFragmentBinding.inflate(inflater, container, false)
            .also {
                it.lifecycleOwner = viewLifecycleOwner
                it.viewModel = viewModel
            }
        binding.fileSystemSpinner.adapter = createFileSystemSpinnerAdapter()
        binding.fileSystemSpinner.onItemSelectedListener = object : OnItemSelectedListener {

            override fun onNothingSelected(parent: AdapterView<*>?) {
            }

            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                onFileSystemItemSelected(position)
            }
        }

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
        navigationViewModel.setNavigationEnabled(true)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        subscribeToLiveData()

        viewModel.onScreenStart()
    }

    private fun subscribeToLiveData() {
        viewModel.showSnackbarEvent.observe(viewLifecycleOwner) { message ->
            showSnackbarMessage(message)
        }
        viewModel.showSystemFilePickerEvent.observe(viewLifecycleOwner) {
            showSystemFilePicker()
        }
        viewModel.showSystemFileCreatorEvent.observe(viewLifecycleOwner) {
            showSystemFileCreator()
        }
    }

    private fun createFileSystemSpinnerAdapter(): ArrayAdapter<String> {
        val items = FILE_SYSTEM_ITEMS.map { (_, resId) -> requireContext().getString(resId) }
        return ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, items).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
    }

    private fun onFileSystemItemSelected(position: Int) {
        val fsType = FILE_SYSTEM_ITEMS[position].first
        viewModel.onFileSystemSelected(fsType)
    }

    private fun showSystemFilePicker() {
        startActivityForResult(newOpenFileIntent(), REQUEST_CODE_PICK_FILE)
    }

    private fun showSystemFileCreator() {
        startActivityForResult(newCreateFileIntent(DEFAULT_DB_NAME), REQUEST_CODE_PICK_FILE)
    }

    companion object {

        private const val REQUEST_CODE_PICK_FILE = 1

        private val FILE_SYSTEM_ITEMS = listOf(
            FSType.INTERNAL_STORAGE to R.string.internal_storage,
            FSType.EXTERNAL_STORAGE to R.string.external_storage,
            FSType.SAF to R.string.storage_access_framework,
            FSType.DROPBOX to R.string.dropbox,
            FSType.WEBDAV to R.string.webdav,
            FSType.GIT to R.string.git
        )

        fun newInstance() = DebugMenuFragment()
    }
}