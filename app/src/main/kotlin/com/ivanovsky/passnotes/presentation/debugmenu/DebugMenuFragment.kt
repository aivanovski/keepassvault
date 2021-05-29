package com.ivanovsky.passnotes.presentation.debugmenu

import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.AdapterView.OnItemSelectedListener
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.lifecycle.observe
import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.data.entity.FSType
import com.ivanovsky.passnotes.databinding.DebugMenuFragmentBinding
import com.ivanovsky.passnotes.presentation.core.extensions.setupActionBar
import com.ivanovsky.passnotes.presentation.core.extensions.showSnackbarMessage
import org.koin.androidx.viewmodel.ext.android.viewModel

class DebugMenuFragment : Fragment() {

    private val viewModel: DebugMenuViewModel by viewModel()

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        setupActionBar {
            title = getString(R.string.debug_menu)
            setHomeAsUpIndicator(null)
            setDisplayHomeAsUpEnabled(true)
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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.showSnackbarEvent.observe(viewLifecycleOwner) { message ->
            showSnackbarMessage(message)
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

    companion object {

        private val FILE_SYSTEM_ITEMS = listOf(
            FSType.REGULAR_FS to R.string.device_file_system,
            FSType.DROPBOX to R.string.dropbox,
            FSType.WEBDAV to R.string.webdav
        )

        fun newInstance() = DebugMenuFragment()
    }
}