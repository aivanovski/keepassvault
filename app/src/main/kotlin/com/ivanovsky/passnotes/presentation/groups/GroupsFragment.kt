package com.ivanovsky.passnotes.presentation.groups

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.observe
import androidx.recyclerview.widget.GridLayoutManager
import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.data.entity.Group
import com.ivanovsky.passnotes.data.entity.Note
import com.ivanovsky.passnotes.data.entity.Template
import com.ivanovsky.passnotes.databinding.GroupsFragmentBinding
import com.ivanovsky.passnotes.presentation.core.dialog.ConfirmationDialog
import com.ivanovsky.passnotes.presentation.core.extensions.setupActionBar
import com.ivanovsky.passnotes.presentation.core.extensions.showToastMessage
import com.ivanovsky.passnotes.presentation.core.extensions.withArguments
import com.ivanovsky.passnotes.presentation.group.GroupActivity
import com.ivanovsky.passnotes.presentation.groups.dialog.ChooseOptionDialog
import com.ivanovsky.passnotes.presentation.note.NoteActivity
import com.ivanovsky.passnotes.presentation.note_editor.NoteEditorActivity
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.util.*

class GroupsFragment : Fragment() {

    private val viewModel: GroupsViewModel by viewModel()

    private lateinit var binding: GroupsFragmentBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = GroupsFragmentBinding.inflate(inflater, container, false)
            .also {
                it.lifecycleOwner = viewLifecycleOwner
                it.viewModel = viewModel
            }

        setupRecyclerView()

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val group = arguments?.getParcelable(ARG_GROUP) as? Group

        setupActionBar {
            title = group?.title ?: resources.getString(R.string.groups)
            setDisplayHomeAsUpEnabled(true)
        }

        subscribeToLiveData()

        viewModel.start(group?.uid)
    }

    private fun subscribeToLiveData() {
        viewModel.toastMessage.observe(viewLifecycleOwner) { message ->
            showToastMessage(message)
        }
        viewModel.showNoteListScreenEvent.observe(viewLifecycleOwner) { group ->
            showNoteListScreen(group)
        }
        viewModel.showNoteScreenEvent.observe(viewLifecycleOwner) { note ->
            showNoteScreen(note)
        }
        viewModel.showNewGroupScreenEvent.observe(viewLifecycleOwner) { parentGroupUid ->
            showNewGroupScreen(parentGroupUid)
        }
        viewModel.showNewNoteScreenEvent.observe(viewLifecycleOwner) { (groupUid, template) ->
            showNewNoteScreen(groupUid, template)
        }
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
        viewModel.showEditNoteScreenEvent.observe(viewLifecycleOwner) { note ->
            showEditNoteScreen(note)
        }
        viewModel.showEditGroupScreenEvent.observe(viewLifecycleOwner) { group ->
            showEditGroupScreen(group)
        }
    }

    private fun setupRecyclerView() {
        (binding.recyclerView.layoutManager as? GridLayoutManager)?.let {
            it.spanCount = COLUMN_COUNT
        }
    }

    private fun showNoteListScreen(group: Group) {
        val context = this.context ?: return

        startActivity(GroupsActivity.intentFroGroup(context, group))
    }

    private fun showNoteScreen(note: Note) {
        val context = this.context ?: return

        startActivity(NoteActivity.createStartIntent(context, note))
    }

    private fun showNewGroupScreen(parentGroupUid: UUID) {
        val context = this.context ?: return

        startActivity(GroupActivity.createChildGroup(context, parentGroupUid))
    }

    private fun showNewNoteScreen(parentGroupUid: UUID, template: Template?) {
        val context = this.context ?: return

        startActivity(NoteEditorActivity.intentForNewNote(context, parentGroupUid, template))
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
            when (itemIdx) {
                0 -> viewModel.onEditGroupClicked(group)
                1 -> viewModel.onRemoveGroupClicked(group)
            }
        }
        dialog.show(childFragmentManager, ChooseOptionDialog.TAG)
    }

    private fun showNoteActionsDialog(note: Note) {
        val entries = resources.getStringArray(R.array.item_actions_entries)

        val dialog = ChooseOptionDialog.newInstance(null, entries.toList())
        dialog.onItemClickListener = { itemIdx ->
            when (itemIdx) {
                0 -> viewModel.onEditNoteClicked(note)
                1 -> viewModel.onRemoveNoteClicked(note)
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
        dialog.onConfirmationLister = {
            viewModel.onRemoveConfirmed(group, note)
        }
        dialog.show(childFragmentManager, ConfirmationDialog.TAG)
    }

    private fun showEditNoteScreen(note: Note) {
        val noteUid = note.uid ?: return

        val intent = NoteEditorActivity.intentForEditNote(requireContext(), noteUid, note.title)
        startActivity(intent)
    }

    private fun showEditGroupScreen(group: Group) {
        // TODO: implement
    }

    companion object {

        private const val ARG_GROUP = "group"

        private const val COLUMN_COUNT = 3

        fun newInstance(group: Group?) = GroupsFragment().withArguments {
            putParcelable(ARG_GROUP, group)
        }
    }
}