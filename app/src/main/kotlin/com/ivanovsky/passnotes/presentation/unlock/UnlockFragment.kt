package com.ivanovsky.passnotes.presentation.unlock

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.observe
import com.ivanovsky.passnotes.BuildConfig
import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.databinding.UnlockFragmentBinding
import com.ivanovsky.passnotes.presentation.core.extensions.hideKeyboard
import com.ivanovsky.passnotes.presentation.core.extensions.setupActionBar
import com.ivanovsky.passnotes.presentation.core.extensions.showSnackbarMessage
import com.ivanovsky.passnotes.presentation.unlock.model.DropDownItem
import com.ivanovsky.passnotes.util.FileUtils

class UnlockFragment : Fragment() {

    private lateinit var fileAdapter: FileSpinnerAdapter
    private lateinit var binding: UnlockFragmentBinding

    private val viewModel: UnlockViewModel by lazy {
        ViewModelProvider(requireActivity(), UnlockViewModel.FACTORY)
            .get(UnlockViewModel::class.java)
    }

    private val spinnerSelectedListener = object : AdapterView.OnItemSelectedListener {
        override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
            autofillPasswordIfNeed()
            viewModel.onRecentlyUsedItemSelected(position)
        }

        override fun onNothingSelected(parent: AdapterView<*>?) {
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        setupActionBar {
            title = getString(R.string.app_name)
            setDisplayHomeAsUpEnabled(true)
            setHomeAsUpIndicator(R.drawable.ic_menu_white_24dp)
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
    ): View {
        binding = UnlockFragmentBinding.inflate(inflater, container, false)
            .also {
                it.lifecycleOwner = viewLifecycleOwner
                it.viewModel = viewModel
            }

        fileAdapter = FileSpinnerAdapter(requireContext())
        binding.fileSpinner.adapter = fileAdapter

        // TODO: move fab initialization to @BindingAdapter
        val fabItems = resources.getStringArray(R.array.unlock_fab_actions).toList()
        binding.fab.inflate(fabItems)
        binding.fab.onItemClickListener = { position -> viewModel.onFabActionClicked(position) }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        subscribeToLiveData()
        subscribeToLiveEvents()

        viewModel.loadData(resetSelection = true)
    }

    private fun subscribeToLiveData() {
        viewModel.items.observe(viewLifecycleOwner) { items ->
            setRecentlyUsedItems(items)
        }
        viewModel.selectedItem.observe(viewLifecycleOwner) { position ->
            setSelectedRecentlyUsedItem(position)
        }
    }

    private fun subscribeToLiveEvents() {
        viewModel.hideKeyboardEvent.observe(viewLifecycleOwner) {
            hideKeyboard()
        }
        viewModel.showSnackbarMessage.observe(viewLifecycleOwner) { message ->
            showSnackbarMessage(message)
        }
    }

    private fun setRecentlyUsedItems(items: List<DropDownItem>) {
        binding.fileSpinner.onItemSelectedListener = null

        fileAdapter.setItems(items)
        fileAdapter.notifyDataSetChanged()

        binding.fileSpinner.onItemSelectedListener = spinnerSelectedListener
    }

    private fun setSelectedRecentlyUsedItem(position: Int) {
        binding.fileSpinner.onItemSelectedListener = null
        binding.fileSpinner.setSelection(position)
        binding.fileSpinner.onItemSelectedListener = spinnerSelectedListener

        autofillPasswordIfNeed()
    }

    private fun autofillPasswordIfNeed() {
        if (!BuildConfig.DEBUG) {
            return
        }

        val selectedPosition = binding.fileSpinner.selectedItemPosition

        val filePath = viewModel.items.value?.get(selectedPosition)?.path ?: return

        val fileName = FileUtils.getFileNameWithoutExtensionFromPath(filePath) ?: return

        for (rule in viewModel.debugPasswordRules) {
            if (rule.pattern.matcher(fileName).matches()) {
                binding.password.setText(rule.password)
                break
            }
        }
    }

    companion object {
        fun newInstance() = UnlockFragment()
    }
}