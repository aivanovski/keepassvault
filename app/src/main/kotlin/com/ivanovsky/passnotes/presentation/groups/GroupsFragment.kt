package com.ivanovsky.passnotes.presentation.groups

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.observe
import com.github.terrakok.cicerone.Router
import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.data.entity.Group
import com.ivanovsky.passnotes.data.entity.Note
import com.ivanovsky.passnotes.data.entity.Template
import com.ivanovsky.passnotes.databinding.GroupsFragmentBinding
import com.ivanovsky.passnotes.injection.GlobalInjector.inject
import com.ivanovsky.passnotes.presentation.core.BaseFragment
import com.ivanovsky.passnotes.presentation.core.DatabaseInteractionWatcher
import com.ivanovsky.passnotes.presentation.core.dialog.ConfirmationDialog
import com.ivanovsky.passnotes.presentation.core.extensions.finishActivity
import com.ivanovsky.passnotes.presentation.core.extensions.getMandatoryArgument
import com.ivanovsky.passnotes.presentation.core.extensions.setupActionBar
import com.ivanovsky.passnotes.presentation.core.extensions.showToastMessage
import com.ivanovsky.passnotes.presentation.core.extensions.updateMenuItemVisibility
import com.ivanovsky.passnotes.presentation.core.extensions.withArguments
import com.ivanovsky.passnotes.presentation.groups.GroupsViewModel.GroupsMenuItem
import com.ivanovsky.passnotes.presentation.groups.dialog.ChooseOptionDialog
import com.ivanovsky.passnotes.presentation.groups.dialog.SortAndViewDialog
import com.ivanovsky.passnotes.presentation.main.navigation.NavigationMenuViewModel

class GroupsFragment : BaseFragment() {

    private val viewModel: GroupsViewModel by lazy {
        ViewModelProvider(
            this,
            GroupsViewModel.Factory(
                args = getMandatoryArgument(ARGUMENTS)
            )
        )
            .get(GroupsViewModel::class.java)
    }
    private val router: Router by inject()

    private lateinit var binding: GroupsFragmentBinding
    private var menu: Menu? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onBackPressed(): Boolean {
        viewModel.onBackClicked()
        return true
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = GroupsFragmentBinding.inflate(inflater, container, false)
            .also {
                it.lifecycleOwner = viewLifecycleOwner
                it.viewModel = viewModel
            }

        return binding.root
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        this.menu = menu

        inflater.inflate(R.menu.groups, menu)

        viewModel.visibleMenuItems.value?.let { visibleItems ->
            updateMenuItemVisibility(
                menu = menu,
                visibleItems = visibleItems,
                allScreenItems = GroupsMenuItem.values().toList()
            )
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                viewModel.onBackClicked()
                true
            }
            R.id.menu_lock -> {
                viewModel.onLockButtonClicked()
                true
            }
            R.id.menu_add_templates -> {
                viewModel.onAddTemplatesClicked()
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
            R.id.menu_sort_and_view -> {
                viewModel.onSortAndViewButtonClicked()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onStart() {
        super.onStart()
        navigationViewModel.setVisibleItems(
            NavigationMenuViewModel.createNavigationItemsForDbScreens()
        )
        navigationViewModel.setNavigationEnabled(true)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewLifecycleOwner.lifecycle.addObserver(DatabaseInteractionWatcher(this))

        setupActionBar {
            setHomeAsUpIndicator(null)
            setDisplayHomeAsUpEnabled(true)
        }

        subscribeToLiveData()
        subscribeToEvents()

        viewModel.start()
    }

    private fun subscribeToLiveData() {
        viewModel.showToastEvent.observe(viewLifecycleOwner) { message ->
            showToastMessage(message)
        }
        viewModel.screenTitle.observe(viewLifecycleOwner) {
            setupActionBar {
                title = it
            }
        }
        viewModel.visibleMenuItems.observe(viewLifecycleOwner) { visibleItems ->
            menu?.let { menu ->
                updateMenuItemVisibility(
                    menu = menu,
                    visibleItems = visibleItems,
                    allScreenItems = GroupsMenuItem.values().toList()
                )
            }
        }
    }

    private fun subscribeToEvents() {
        viewModel.showNewEntryDialogEvent.observe(viewLifecycleOwner) { templates ->
            showNewEntryDialog(templates)
        }
        viewModel.showGroupActionsDialogEvent.observe(viewLifecycleOwner) { group ->
            showGroupActionsDialog(group)
        }
        viewModel.showNoteActionsDialogEvent.observe(viewLifecycleOwner) { note ->
            showNoteActionsDialog(note)
        }
        viewModel.showRemoveConfirmationDialogEvent.observe(viewLifecycleOwner) { (group, note) ->
            showRemoveConfirmationDialog(group, note)
        }
        viewModel.showAddTemplatesDialogEvent.observe(viewLifecycleOwner) {
            showAddTemplatesDialog()
        }
        viewModel.finishActivityEvent.observe(viewLifecycleOwner) {
            finishActivity()
        }
        viewModel.showUnlockScreenEvent.observe(viewLifecycleOwner) {
            router.backTo(it)
        }
        viewModel.showSortAndViewDialogEvent.observe(viewLifecycleOwner) {
            showSortAndViewDialog()
        }
    }

    private fun showNewEntryDialog(templates: List<Template>) {
        val templateEntries = templates.map { template -> template.title }

        val entries = listOf(getString(R.string.new_item_entry_standard_note)) +
            templateEntries +
            listOf(getString(R.string.new_item_entry_new_group))

        val dialog = ChooseOptionDialog.newInstance(getString(R.string.create), entries)

        dialog.onItemClickListener = { itemIdx ->
            when (itemIdx) {
                0 -> viewModel.onCreateNewNoteClicked()
                entries.size - 1 -> viewModel.onCreateNewGroupClicked()
                else -> viewModel.onCreateNewNoteFromTemplateClicked(templates[itemIdx - 1])
            }
        }

        dialog.show(childFragmentManager, ChooseOptionDialog.TAG)
    }

    private fun showGroupActionsDialog(group: Group) {
        val entries = resources.getStringArray(R.array.item_actions_entries)

        val dialog = ChooseOptionDialog.newInstance(null, entries.toList())
        dialog.onItemClickListener = { itemIdx ->
            when (itemIdx) { // TODO: refactor, move menu creation to VM
                0 -> viewModel.onEditGroupClicked(group)
                1 -> viewModel.onRemoveGroupClicked(group)
                2 -> viewModel.onCutGroupClicked(group)
            }
        }
        dialog.show(childFragmentManager, ChooseOptionDialog.TAG)
    }

    private fun showNoteActionsDialog(note: Note) {
        val entries = resources.getStringArray(R.array.item_actions_entries)

        val dialog = ChooseOptionDialog.newInstance(null, entries.toList())
        dialog.onItemClickListener = { itemIdx ->
            when (itemIdx) { // TODO: refactor, move menu creation to VM
                0 -> viewModel.onEditNoteClicked(note)
                1 -> viewModel.onRemoveNoteClicked(note)
                2 -> viewModel.onCutNoteClicked(note)
            }
        }
        dialog.show(childFragmentManager, ChooseOptionDialog.TAG)
    }

    private fun showRemoveConfirmationDialog(group: Group?, note: Note?) {
        val message = if (note != null) {
            getString(R.string.note_remove_confirmation_message, note.title)
        } else {
            getString(R.string.group_remove_confirmation_message)
        }

        val dialog = ConfirmationDialog.newInstance(
            message,
            getString(R.string.yes),
            getString(R.string.no)
        )
        dialog.onConfirmed = {
            viewModel.onRemoveConfirmed(group, note)
        }
        dialog.show(childFragmentManager, ConfirmationDialog.TAG)
    }

    private fun showAddTemplatesDialog() {
        val dialog = ConfirmationDialog.newInstance(
            getString(R.string.add_templates_confirmation_message),
            getString(R.string.yes),
            getString(R.string.no)
        ).apply {
            onConfirmed = {
                viewModel.onAddTemplatesConfirmed()
            }
        }
        dialog.show(childFragmentManager, ConfirmationDialog.TAG)
    }

    private fun showSortAndViewDialog() {
        val dialog = SortAndViewDialog.newInstance()
        dialog.show(childFragmentManager, SortAndViewDialog.TAG)
    }

    companion object {

        private const val ARGUMENTS = "arguments"

        fun newInstance(args: GroupsScreenArgs) = GroupsFragment()
            .withArguments {
                putParcelable(ARGUMENTS, args)
            }
    }
}