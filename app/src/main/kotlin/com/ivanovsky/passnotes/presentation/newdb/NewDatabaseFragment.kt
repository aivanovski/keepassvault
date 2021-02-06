package com.ivanovsky.passnotes.presentation.newdb

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.observe
import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.data.entity.FileDescriptor
import com.ivanovsky.passnotes.databinding.NewDatabaseFragmentBinding
import com.ivanovsky.passnotes.presentation.core_mvvm.extensions.finishActivity
import com.ivanovsky.passnotes.presentation.core_mvvm.extensions.hideKeyboard
import com.ivanovsky.passnotes.presentation.core_mvvm.extensions.setupActionBar
import com.ivanovsky.passnotes.presentation.groups.GroupsActivity
import com.ivanovsky.passnotes.presentation.storagelist.Action
import com.ivanovsky.passnotes.presentation.storagelist.StorageListActivity
import org.koin.androidx.viewmodel.ext.android.viewModel

class NewDatabaseFragment : Fragment() {

    private val viewModel: NewDatabaseViewModel by viewModel()
    private var menu: Menu? = null

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        setupActionBar {
            title = getString(R.string.new_database)
            setDisplayHomeAsUpEnabled(true)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        this.menu = menu
        inflater.inflate(R.menu.base_done, menu)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val binding = NewDatabaseFragmentBinding.inflate(inflater, container, false)
            .also {
                it.lifecycleOwner = viewLifecycleOwner
                it.viewModel = viewModel
            }
        return binding.root
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return if (item.itemId == R.id.menu_done) {
            viewModel.createNewDatabaseFile()
            true
        } else {
            super.onOptionsItemSelected(item)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.doneButtonVisibility.observe(viewLifecycleOwner) { isVisible ->
            setDoneButtonVisibility(isVisible)
        }
        viewModel.showGroupsScreenEvent.observe(viewLifecycleOwner) {
            showGroupsScreen()
        }
        viewModel.showStorageScreenEvent.observe(viewLifecycleOwner) {
            showStorageScreen()
        }
        viewModel.hideKeyboardEvent.observe(viewLifecycleOwner) {
            hideKeyboard()
        }
    }

    private fun showGroupsScreen() {
        finishActivity()
        startActivity(GroupsActivity.startForRootGroup(requireContext()))
    }

    private fun showStorageScreen() {
        startActivityForResult(
            StorageListActivity.createStartIntent(requireContext(), Action.PICK_STORAGE),
            REQUEST_CODE_PICK_STORAGE
        )
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        val extras = data?.extras
        if (resultCode == Activity.RESULT_OK &&
            requestCode == REQUEST_CODE_PICK_STORAGE &&
            extras != null
        ) {
            val file = extras.getParcelable<FileDescriptor>(StorageListActivity.EXTRA_RESULT)
            if (file != null) {
                viewModel.onStorageSelected(file)
            }
        }
    }

    private fun setDoneButtonVisibility(isVisible: Boolean) {
        val menu = this.menu ?: return

        val item = menu.findItem(R.id.menu_done)
        item.isVisible = isVisible
    }



    companion object {

        private const val REQUEST_CODE_PICK_STORAGE = 100

        fun newInstance() = NewDatabaseFragment()
    }
}