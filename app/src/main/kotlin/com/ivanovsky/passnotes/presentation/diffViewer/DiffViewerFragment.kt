package com.ivanovsky.passnotes.presentation.diffViewer

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.viewModels
import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.databinding.CoreComposeFragmentBinding
import com.ivanovsky.passnotes.presentation.core.BaseFragment
import com.ivanovsky.passnotes.presentation.core.DatabaseInteractionWatcher
import com.ivanovsky.passnotes.presentation.core.ViewModelFactory
import com.ivanovsky.passnotes.presentation.core.compose.AppTheme
import com.ivanovsky.passnotes.presentation.core.compose.getComposeTheme
import com.ivanovsky.passnotes.presentation.core.extensions.getMandatoryArgument
import com.ivanovsky.passnotes.presentation.core.extensions.setupActionBar
import com.ivanovsky.passnotes.presentation.core.extensions.showConfirmationDialog
import com.ivanovsky.passnotes.presentation.core.extensions.showReportErrorDialog
import com.ivanovsky.passnotes.presentation.core.extensions.updateMenuItemVisibility
import com.ivanovsky.passnotes.presentation.core.extensions.withArguments
import com.ivanovsky.passnotes.presentation.diffViewer.DiffViewerViewModel.DiffViewerMenuItem

class DiffViewerFragment : BaseFragment() {

    private var menu: Menu? = null

    private val args: DiffViewerScreenArgs by lazy {
        getMandatoryArgument(ARGUMENTS)
    }

    private val viewModel: DiffViewerViewModel by viewModels(
        factoryProducer = { ViewModelFactory(args) }
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
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
                    DiffViewerScreen(viewModel)
                }
            }
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (args.isHoldDatabaseInteraction) {
            viewLifecycleOwner.lifecycle.addObserver(DatabaseInteractionWatcher(this))
        }

        setupActionBar {
            title = when (args.mode) {
                is DiffViewerMode.Compare -> getString(R.string.compare_files)
                is DiffViewerMode.Merge -> getString(R.string.merge_changes)
            }
            setDisplayHomeAsUpEnabled(true)
            setHomeAsUpIndicator(null)
        }

        subscribeToLiveData()
        subscribeToLiveEvents()

        viewModel.start()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        this.menu = menu

        inflater.inflate(R.menu.diff_viewer, menu)

        viewModel.visibleMenuItems.value?.let { visibleItems ->
            updateMenuItemVisibility(
                menu = menu,
                visibleItems = visibleItems,
                allScreenItems = DiffViewerMenuItem.entries
            )
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val action = MENU_ACTIONS[item.itemId] ?: throw IllegalArgumentException()
        action.invoke(viewModel)
        return true
    }

    private fun subscribeToLiveData() {
        viewModel.visibleMenuItems.observe(viewLifecycleOwner) { visibleItems ->
            menu?.let { menu ->
                updateMenuItemVisibility(
                    menu = menu,
                    visibleItems = visibleItems,
                    allScreenItems = DiffViewerMenuItem.entries
                )
            }
        }
    }

    private fun subscribeToLiveEvents() {
        viewModel.showConfirmationDialogEvent.observe(viewLifecycleOwner) { args ->
            showConfirmationDialog(args) {
                viewModel.onMergeConfirmed()
            }
        }
        viewModel.showReportErrorDialogEvent.observe(viewLifecycleOwner) { args ->
            showReportErrorDialog(args)
        }
    }

    companion object {

        private const val ARGUMENTS = "arguments"

        private val MENU_ACTIONS = mapOf<Int, (vm: DiffViewerViewModel) -> Unit>(
            android.R.id.home to { vm -> vm.navigateBack() },
            R.id.menu_merge to { vm -> vm.onMergeButtonClicked() }
        )

        fun newInstance(
            args: DiffViewerScreenArgs
        ): DiffViewerFragment = DiffViewerFragment()
            .withArguments {
                putParcelable(ARGUMENTS, args)
            }
    }
}