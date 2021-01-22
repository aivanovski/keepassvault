package com.ivanovsky.passnotes.presentation.unlock

import android.app.Activity
import android.content.Intent
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
import com.ivanovsky.passnotes.data.entity.FileDescriptor
import com.ivanovsky.passnotes.databinding.UnlockFragmentBinding
import com.ivanovsky.passnotes.presentation.core_mvvm.extensions.hideKeyboard
import com.ivanovsky.passnotes.presentation.core_mvvm.extensions.setupActionBar
import com.ivanovsky.passnotes.presentation.core_mvvm.extensions.showSnackbarMessage
import com.ivanovsky.passnotes.presentation.debugmenu.DebugMenuActivity
import com.ivanovsky.passnotes.presentation.groups.GroupsActivity
import com.ivanovsky.passnotes.presentation.newdb.NewDatabaseActivity
import com.ivanovsky.passnotes.presentation.storagelist.Action
import com.ivanovsky.passnotes.presentation.storagelist.StorageListActivity
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
    ): View? {
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
        viewModel.showGroupsScreenEvent.observe(viewLifecycleOwner) {
            showGroupsScreen()
        }
        viewModel.showNewDatabaseScreenEvent.observe(viewLifecycleOwner) {
            showNewDatabaseScreen()
        }
        viewModel.showOpenFileScreenEvent.observe(viewLifecycleOwner) {
            showOpenFileScreen()
        }
        viewModel.showSettingsScreenEvent.observe(viewLifecycleOwner) {
            showSettingsScreen()
        }
        viewModel.showAboutScreenEvent.observe(viewLifecycleOwner) {
            showAboutScreen()
        }
        viewModel.showDebugMenuScreenEvent.observe(viewLifecycleOwner) {
            showDebugMenuScreen()
        }
        viewModel.hideKeyboardEvent.observe(viewLifecycleOwner) {
            hideKeyboard()
        }
        viewModel.showSnackbarMessage.observe(viewLifecycleOwner) { message ->
            showSnackbarMessage(message)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == Activity.RESULT_OK && requestCode == REQUEST_CODE_PICK_FILE) {
            val extras = data?.extras
            if (extras != null) {
                val file = extras.getParcelable<FileDescriptor>(StorageListActivity.EXTRA_RESULT)
                if (file != null) {
                    viewModel.onFilePicked(file)
                }
            }
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

    private fun showGroupsScreen() {
        startActivity(GroupsActivity.startForRootGroup(requireContext()))
    }

    private fun showNewDatabaseScreen() {
        startActivity(Intent(requireContext(), NewDatabaseActivity::class.java))
    }

    private fun showOpenFileScreen() {
        val intent = StorageListActivity.createStartIntent(requireContext(), Action.PICK_FILE)
        startActivityForResult(intent, REQUEST_CODE_PICK_FILE)
    }

    private fun showSettingsScreen() {
        throw RuntimeException("Not implemented") //TODO: handle menu click
    }

    private fun showAboutScreen() {
        throw RuntimeException("Not implemented") //TODO: handle menu click
    }

    private fun showDebugMenuScreen() {
        val intent = DebugMenuActivity.createStartIntent(requireContext())
        startActivity(intent)
    }

    companion object {

        private const val REQUEST_CODE_PICK_FILE = 100

        fun newInstance() = UnlockFragment()
    }
}