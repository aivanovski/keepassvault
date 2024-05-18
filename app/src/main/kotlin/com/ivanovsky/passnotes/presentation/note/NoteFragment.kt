package com.ivanovsky.passnotes.presentation.note

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import com.github.terrakok.cicerone.Router
import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.data.entity.Note
import com.ivanovsky.passnotes.data.entity.Property
import com.ivanovsky.passnotes.databinding.NoteFragmentBinding
import com.ivanovsky.passnotes.injection.GlobalInjector.inject
import com.ivanovsky.passnotes.presentation.ApplicationLaunchMode
import com.ivanovsky.passnotes.presentation.Screens
import com.ivanovsky.passnotes.presentation.autofill.AutofillDialogFactory
import com.ivanovsky.passnotes.presentation.core.BaseFragment
import com.ivanovsky.passnotes.presentation.core.DatabaseInteractionWatcher
import com.ivanovsky.passnotes.presentation.core.adapter.ViewModelsAdapter
import com.ivanovsky.passnotes.presentation.core.dialog.ConfirmationDialog
import com.ivanovsky.passnotes.presentation.core.dialog.propertyAction.PropertyActionDialog
import com.ivanovsky.passnotes.presentation.core.dialog.propertyAction.PropertyActionDialogArgs
import com.ivanovsky.passnotes.presentation.core.extensions.finishActivity
import com.ivanovsky.passnotes.presentation.core.extensions.getMandatoryArgument
import com.ivanovsky.passnotes.presentation.core.extensions.openUrl
import com.ivanovsky.passnotes.presentation.core.extensions.sendAutofillResult
import com.ivanovsky.passnotes.presentation.core.extensions.setViewModels
import com.ivanovsky.passnotes.presentation.core.extensions.setupActionBar
import com.ivanovsky.passnotes.presentation.core.extensions.showSnackbarMessage
import com.ivanovsky.passnotes.presentation.core.extensions.updateMenuItemVisibility
import com.ivanovsky.passnotes.presentation.core.extensions.withArguments
import com.ivanovsky.passnotes.presentation.groups.dialog.ChooseOptionDialog
import com.ivanovsky.passnotes.presentation.note.NoteViewModel.AttachmentAction
import com.ivanovsky.passnotes.presentation.note.NoteViewModel.NoteMenuItem
import com.ivanovsky.passnotes.presentation.unlock.UnlockScreenArgs
import com.ivanovsky.passnotes.util.FileUtils
import com.ivanovsky.passnotes.util.IntentUtils
import com.ivanovsky.passnotes.util.StringUtils
import java.io.File

class NoteFragment : BaseFragment() {

    private val viewModel: NoteViewModel by lazy {
        ViewModelProvider(
            this,
            NoteViewModel.Factory(
                args = getMandatoryArgument(ARGUMENTS)
            )
        )
            .get(NoteViewModel::class.java)
    }
    private val router: Router by inject()
    private var menu: Menu? = null
    private lateinit var binding: NoteFragmentBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        setupActionBar {
            title = StringUtils.EMPTY
            setHomeAsUpIndicator(null)
            setDisplayHomeAsUpEnabled(true)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        this.menu = menu

        inflater.inflate(R.menu.note, menu)

        viewModel.visibleMenuItems.value?.let { visibleItems ->
            updateMenuItemVisibility(
                menu = menu,
                visibleItems = visibleItems,
                allScreenItems = NoteMenuItem.values().toList()
            )
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                viewModel.navigateBack()
                true
            }

            R.id.menu_lock -> {
                viewModel.onLockButtonClicked()
                true
            }

            R.id.menu_search -> {
                viewModel.onSearchButtonClicked()
                true
            }

            R.id.menu_settings -> {
                viewModel.onSettingsButtonClicked()
                true
            }

            R.id.menu_select -> {
                viewModel.onSelectButtonClicked()
                true
            }

            R.id.menu_toggle_hidden -> {
                viewModel.onToggleHiddenClicked()
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = NoteFragmentBinding.inflate(inflater, container, false)
            .also {
                it.lifecycleOwner = viewLifecycleOwner
                it.viewModel = viewModel
            }

        binding.recyclerView.adapter = ViewModelsAdapter(
            lifecycleOwner = viewLifecycleOwner,
            viewTypes = viewModel.viewTypes
        )

        return binding.root
    }

    override fun onStart() {
        super.onStart()
        navigationViewModel.setNavigationEnabled(true)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewLifecycleOwner.lifecycle.addObserver(DatabaseInteractionWatcher(this))

        subscribeToLiveData()
        subscribeToEvents()

        viewModel.loadData()
    }

    private fun subscribeToLiveData() {
        viewModel.cellViewModels.observe(viewLifecycleOwner) { viewModels ->
            binding.recyclerView.setViewModels(viewModels)
        }
        viewModel.visibleMenuItems.observe(viewLifecycleOwner) { visibleItems ->
            menu?.let { menu ->
                updateMenuItemVisibility(
                    menu = menu,
                    visibleItems = visibleItems,
                    allScreenItems = NoteMenuItem.values().toList()
                )
            }
        }
        viewModel.actionBarTitle.observe(viewLifecycleOwner) { actionBarTitle ->
            setupActionBar {
                title = actionBarTitle
            }
        }
    }

    private fun subscribeToEvents() {
        viewModel.showSnackbarMessageEvent.observe(viewLifecycleOwner) { message ->
            showSnackbarMessage(message)
        }
        viewModel.finishActivityEvent.observe(viewLifecycleOwner) {
            finishActivity()
        }
        viewModel.sendAutofillResponseEvent.observe(viewLifecycleOwner) { (note, structure) ->
            sendAutofillResult(note, structure)
            finishActivity()
        }
        viewModel.showAddAutofillDataDialog.observe(viewLifecycleOwner) {
            showAddAutofillDataDialog(it)
        }
        viewModel.lockScreenEvent.observe(viewLifecycleOwner) {
            router.backTo(
                Screens.UnlockScreen(
                    args = UnlockScreenArgs(ApplicationLaunchMode.NORMAL)
                )
            )
        }
        viewModel.showPropertyActionDialog.observe(viewLifecycleOwner) { property ->
            showPropertyActionDialog(property)
        }
        viewModel.openUrlEvent.observe(viewLifecycleOwner) { url ->
            openUrl(url)
        }
        viewModel.shareFileEvent.observe(viewLifecycleOwner) { file ->
            shareFile(file)
        }
        viewModel.openFileEvent.observe(viewLifecycleOwner) { file ->
            openFile(file)
        }
        viewModel.showAttachmentActionDialog.observe(viewLifecycleOwner) { actions ->
            showAttachmentActionDialog(actions)
        }
    }

    private fun showAddAutofillDataDialog(note: Note) {
        val dialog = AutofillDialogFactory(requireContext()).createAddAutofillDataToNoteDialog(
            onConfirmed = { viewModel.onAddAutofillDataConfirmed(note) },
            onDenied = { viewModel.onAddAutofillDataDenied(note) }
        )
        dialog.show(childFragmentManager, ConfirmationDialog.TAG)
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

    private fun shareFile(file: File) {
        val intent = IntentUtils.newShareFileIntent(requireContext(), file)
        startActivity(Intent.createChooser(intent, null))
    }

    private fun openFile(file: File) {
        val intent = IntentUtils.newViewFileIntent(requireContext(), file)
        startActivity(Intent.createChooser(intent, null))
    }

    private fun openAsText(file: File) {
        val intent = IntentUtils.newViewFileIntent(
            requireContext(),
            file,
            mimeType = FileUtils.MIME_TYPE_TEXT
        )
        startActivity(Intent.createChooser(intent, null))
    }

    private fun showAttachmentActionDialog(actions: List<AttachmentAction>) {
        val entries = actions.map { action ->
            when (action) {
                is AttachmentAction.OpenFile -> getString(R.string.view)
                is AttachmentAction.OpenAsText -> getString(R.string.view_as_text)
                is AttachmentAction.ShareFile -> getString(R.string.share)
            }
        }

        val dialog = ChooseOptionDialog.newInstance(
            title = null,
            entries = entries
        ).apply {
            onItemClickListener = { idx ->
                when (val action = actions[idx]) {
                    is AttachmentAction.OpenFile -> {
                        openFile(action.file)
                    }

                    is AttachmentAction.OpenAsText -> {
                        openAsText(action.file)
                    }

                    is AttachmentAction.ShareFile -> {
                        shareFile(action.file)
                    }
                }
            }
        }

        dialog.show(childFragmentManager, ChooseOptionDialog.TAG)
    }

    companion object {

        private const val ARGUMENTS = "arguments"

        fun newInstance(args: NoteScreenArgs) = NoteFragment()
            .withArguments {
                putParcelable(ARGUMENTS, args)
            }
    }
}