package com.ivanovsky.passnotes.presentation.debugmenu

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.AdapterView.OnItemSelectedListener
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.lifecycle.observe
import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.data.repository.file.FSType
import com.ivanovsky.passnotes.databinding.DebugMenuFragmentBinding
import com.ivanovsky.passnotes.presentation.core_mvvm.extensions.setupActionBar
import com.ivanovsky.passnotes.presentation.core_mvvm.extensions.showSnackbarMessage
import org.koin.androidx.viewmodel.ext.android.viewModel

class DebugMenuFragment : Fragment() {

    private val viewModel: DebugMenuViewModel by viewModel()

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        setupActionBar {
            title = getString(R.string.debug_menu)
            setDisplayHomeAsUpEnabled(true)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.showSnackbarEvent.observe(viewLifecycleOwner) { message ->
            showSnackbarMessage(message)
        }
    }

    private fun createFileSystemSpinnerAdapter(): ArrayAdapter<String> {
        val items = listOf(
            getString(R.string.device_file_system),
            getString(R.string.dropbox)
        )
        return ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, items).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
    }

    private fun onFileSystemItemSelected(position: Int) {
        when (position) {
            0 -> viewModel.onFileSystemSelected(FSType.REGULAR_FS)
            1 -> viewModel.onFileSystemSelected(FSType.DROPBOX)
        }
    }

    companion object {

        fun newInstance() = DebugMenuFragment()
    }
}