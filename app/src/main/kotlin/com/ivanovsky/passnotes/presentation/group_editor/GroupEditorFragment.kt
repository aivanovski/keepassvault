package com.ivanovsky.passnotes.presentation.group_editor

import android.os.Bundle
import android.view.*
import androidx.lifecycle.observe
import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.databinding.GroupEditorFragmentBinding
import com.ivanovsky.passnotes.presentation.core.FragmentWithDoneButton
import com.ivanovsky.passnotes.presentation.core.DatabaseInteractionWatcher
import com.ivanovsky.passnotes.presentation.core.extensions.getMandatoryArgument
import com.ivanovsky.passnotes.presentation.core.extensions.hideKeyboard
import com.ivanovsky.passnotes.presentation.core.extensions.setupActionBar
import com.ivanovsky.passnotes.presentation.core.extensions.withArguments
import org.koin.androidx.viewmodel.ext.android.viewModel

class GroupEditorFragment : FragmentWithDoneButton() {

    private val viewModel: GroupEditorViewModel by viewModel()
    private val args: GroupEditorArgs by lazy { getMandatoryArgument(ARGS) }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        setupActionBar {
            title = getScreenTitle()
            setHomeAsUpIndicator(null)
            setDisplayHomeAsUpEnabled(true)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return GroupEditorFragmentBinding.inflate(inflater, container, false)
            .also {
                it.lifecycleOwner = viewLifecycleOwner
                it.viewModel = viewModel
            }
            .root
    }

    override fun onDoneMenuClicked() {
        viewModel.onDoneButtonClicked()
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

        viewLifecycleOwner.lifecycle.addObserver(DatabaseInteractionWatcher(this))

        subscribeToLiveData()

        viewModel.start(args)
    }

    private fun subscribeToLiveData() {
        viewModel.doneButtonVisibility.observe(viewLifecycleOwner) { isVisible ->
            setDoneButtonVisibility(isVisible)
        }
        viewModel.hideKeyboardEvent.observe(viewLifecycleOwner) {
            hideKeyboard()
        }
    }

    private fun getScreenTitle(): String {
        return when(args) {
            is GroupEditorArgs.NewGroup -> getString(R.string.new_group)
            is GroupEditorArgs.EditGroup -> getString(R.string.edit_group)
        }
    }

    companion object {

        private const val ARGS = "args"

        fun newInstance(args: GroupEditorArgs) = GroupEditorFragment().withArguments {
            putParcelable(ARGS, args)
        }
    }
}