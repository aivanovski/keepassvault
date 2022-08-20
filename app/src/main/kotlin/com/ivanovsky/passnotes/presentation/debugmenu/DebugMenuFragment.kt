package com.ivanovsky.passnotes.presentation.debugmenu

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.observe
import com.ivanovsky.passnotes.R
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
        return DebugMenuFragmentBinding.inflate(inflater, container, false)
            .also {
                it.lifecycleOwner = viewLifecycleOwner
                it.viewModel = viewModel
            }
            .root
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

    private fun showSystemFilePicker() {
        startActivityForResult(newOpenFileIntent(), REQUEST_CODE_PICK_FILE)
    }

    private fun showSystemFileCreator() {
        startActivityForResult(newCreateFileIntent(DEFAULT_DB_NAME), REQUEST_CODE_PICK_FILE)
    }

    companion object {

        private const val REQUEST_CODE_PICK_FILE = 1

        fun newInstance() = DebugMenuFragment()
    }
}