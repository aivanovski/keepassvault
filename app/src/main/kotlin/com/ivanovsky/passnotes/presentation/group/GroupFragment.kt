package com.ivanovsky.passnotes.presentation.group

import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.observe
import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.databinding.GroupFragmentBinding
import com.ivanovsky.passnotes.presentation.core_mvvm.extensions.finishActivity
import com.ivanovsky.passnotes.presentation.core_mvvm.extensions.hideKeyboard
import com.ivanovsky.passnotes.presentation.core_mvvm.extensions.setupActionBar
import com.ivanovsky.passnotes.presentation.core_mvvm.extensions.withArguments
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.util.*

class GroupFragment : Fragment() {

    private val viewModel: GroupViewModel by viewModel()
    private var menu: Menu? = null

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        setupActionBar {
            title = getString(R.string.new_group)
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
        return GroupFragmentBinding.inflate(inflater, container, false)
            .also {
                it.lifecycleOwner = viewLifecycleOwner
                it.viewModel = viewModel
            }
            .root
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return if (item.itemId == R.id.menu_done) {
            viewModel.createNewGroup()
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
        viewModel.finishScreenEvent.observe(viewLifecycleOwner) {
            finishActivity()
        }
        viewModel.hideKeyboardEvent.observe(viewLifecycleOwner) {
            hideKeyboard()
        }

        val parentGroupUid = arguments?.getSerializable(PARENT_GROUP_UID) as? UUID
        viewModel.start(parentGroupUid)
    }

    private fun setDoneButtonVisibility(isVisible: Boolean) {
        val menu = this.menu ?: return

        val item = menu.findItem(R.id.menu_done)
        item.isVisible = isVisible
    }

    companion object {

        private const val PARENT_GROUP_UID = "parentGroupUid"

        fun newInstance(parentGroupUid: UUID?) = GroupFragment().withArguments {
            putSerializable(PARENT_GROUP_UID, parentGroupUid)
        }
    }
}