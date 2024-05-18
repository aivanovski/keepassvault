package com.ivanovsky.passnotes.presentation.history

import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.ViewModelProvider
import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.data.entity.Property
import com.ivanovsky.passnotes.databinding.CoreComposeFragmentBinding
import com.ivanovsky.passnotes.presentation.core.BaseFragment
import com.ivanovsky.passnotes.presentation.core.DatabaseInteractionWatcher
import com.ivanovsky.passnotes.presentation.core.compose.AppTheme
import com.ivanovsky.passnotes.presentation.core.compose.getComposeTheme
import com.ivanovsky.passnotes.presentation.core.dialog.propertyAction.PropertyActionDialog
import com.ivanovsky.passnotes.presentation.core.dialog.propertyAction.PropertyActionDialogArgs
import com.ivanovsky.passnotes.presentation.core.extensions.getMandatoryArgument
import com.ivanovsky.passnotes.presentation.core.extensions.setupActionBar
import com.ivanovsky.passnotes.presentation.core.extensions.showSnackbarMessage
import com.ivanovsky.passnotes.presentation.core.extensions.withArguments

class HistoryFragment : BaseFragment() {

    private val viewModel: HistoryViewModel by lazy {
        ViewModelProvider(
            this,
            HistoryViewModel.Factory(
                args = getMandatoryArgument(ARGUMENTS)
            )
        )
            .get(HistoryViewModel::class.java)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = CoreComposeFragmentBinding.inflate(inflater, container, false)

        binding.composeView.apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)

            setContent {
                val theme by viewModel.theme.collectAsState(initial = context.getComposeTheme())

                AppTheme(theme = theme) {
                    HistoryScreen(viewModel)
                }
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

        viewLifecycleOwner.lifecycle.addObserver(DatabaseInteractionWatcher(this))

        subscribeToEvents()

        setupActionBar {
            title = getString(R.string.history)
            setHomeAsUpIndicator(null)
            setDisplayHomeAsUpEnabled(true)
        }

        viewModel.start()
    }

    private fun subscribeToEvents() {
        viewModel.showPropertyActionDialog.observe(viewLifecycleOwner) { property ->
            showPropertyActionDialog(property)
        }
        viewModel.showSnackbarMessageEvent.observe(viewLifecycleOwner) { message ->
            showSnackbarMessage(message)
        }
    }

    private fun showPropertyActionDialog(property: Property) {
        val dialog = PropertyActionDialog.newInstance(
            args = PropertyActionDialogArgs(property)
        )
            .apply {
                onActionClicked = { action ->
                    viewModel.onPropertyActionClicked(action)
                }
            }

        dialog.show(childFragmentManager, PropertyActionDialog.TAG)
    }

    companion object {

        private const val ARGUMENTS = "arguments"

        fun newInstance(args: HistoryScreenArgs): HistoryFragment {
            return HistoryFragment()
                .withArguments {
                    putParcelable(ARGUMENTS, args)
                }
        }
    }
}