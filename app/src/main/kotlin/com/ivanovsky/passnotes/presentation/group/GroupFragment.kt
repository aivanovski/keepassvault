package com.ivanovsky.passnotes.presentation.group

import android.os.Bundle
import android.view.*
import androidx.lifecycle.observe
import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.databinding.GroupFragmentBinding
import com.ivanovsky.passnotes.presentation.core.FragmentWithDoneButton
import com.ivanovsky.passnotes.presentation.core.extensions.hideKeyboard
import com.ivanovsky.passnotes.presentation.core.extensions.setupActionBar
import com.ivanovsky.passnotes.presentation.core.extensions.withArguments
import java.util.UUID
import org.koin.androidx.viewmodel.ext.android.viewModel

class GroupFragment : FragmentWithDoneButton() {

    private val viewModel: GroupViewModel by viewModel()

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        setupActionBar {
            title = getString(R.string.new_group)
            setHomeAsUpIndicator(null)
            setDisplayHomeAsUpEnabled(true)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return GroupFragmentBinding.inflate(inflater, container, false)
            .also {
                it.lifecycleOwner = viewLifecycleOwner
                it.viewModel = viewModel
            }
            .root
    }

    override fun onDoneMenuClicked() {
        viewModel.createNewGroup()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                viewModel.navigateBack()
                true
            }
            else -> {
                super.onOptionsItemSelected(item)
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.doneButtonVisibility.observe(viewLifecycleOwner) { isVisible ->
            setDoneButtonVisibility(isVisible)
        }
        viewModel.hideKeyboardEvent.observe(viewLifecycleOwner) {
            hideKeyboard()
        }

        val parentGroupUid = arguments?.getSerializable(PARENT_GROUP_UID) as? UUID
        viewModel.start(parentGroupUid)
    }

    companion object {

        private const val PARENT_GROUP_UID = "parentGroupUid"

        fun newInstance(parentGroupUid: UUID?) = GroupFragment().withArguments {
            putSerializable(PARENT_GROUP_UID, parentGroupUid)
        }
    }
}