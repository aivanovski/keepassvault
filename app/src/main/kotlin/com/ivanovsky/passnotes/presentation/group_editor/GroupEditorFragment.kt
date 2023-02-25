package com.ivanovsky.passnotes.presentation.group_editor

import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.observe
import com.github.terrakok.cicerone.Router
import com.ivanovsky.passnotes.databinding.GroupEditorFragmentBinding
import com.ivanovsky.passnotes.injection.GlobalInjector.inject
import com.ivanovsky.passnotes.presentation.ApplicationLaunchMode
import com.ivanovsky.passnotes.presentation.Screens
import com.ivanovsky.passnotes.presentation.core.DatabaseInteractionWatcher
import com.ivanovsky.passnotes.presentation.core.FragmentWithDoneButton
import com.ivanovsky.passnotes.presentation.core.extensions.getMandatoryArgument
import com.ivanovsky.passnotes.presentation.core.extensions.hideKeyboard
import com.ivanovsky.passnotes.presentation.core.extensions.setupActionBar
import com.ivanovsky.passnotes.presentation.core.extensions.withArguments
import com.ivanovsky.passnotes.presentation.unlock.UnlockScreenArgs

class GroupEditorFragment : FragmentWithDoneButton() {

    private val viewModel: GroupEditorViewModel by lazy {
        ViewModelProvider(
            this,
            GroupEditorViewModel.Factory(args = getMandatoryArgument(ARGUMENTS))
        )
            .get(GroupEditorViewModel::class.java)
    }
    private val router: Router by inject()

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        setupActionBar {
            title = viewModel.screenTitle
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
        subscribeToEvents()

        viewModel.onScreenCreated()
    }

    private fun subscribeToLiveData() {
        viewModel.doneButtonVisibility.observe(viewLifecycleOwner) { isVisible ->
            setDoneButtonVisibility(isVisible)
        }
        viewModel.hideKeyboardEvent.observe(viewLifecycleOwner) {
            hideKeyboard()
        }
    }

    private fun subscribeToEvents() {
        viewModel.lockScreenEvent.observe(viewLifecycleOwner) {
            router.backTo(
                Screens.UnlockScreen(
                    args = UnlockScreenArgs(ApplicationLaunchMode.NORMAL)
                )
            )
        }
    }

    companion object {

        private const val ARGUMENTS = "args"

        fun newInstance(args: GroupEditorArgs) = GroupEditorFragment().withArguments {
            putParcelable(ARGUMENTS, args)
        }
    }
}