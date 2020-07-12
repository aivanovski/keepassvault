package com.ivanovsky.passnotes.presentation.groups

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.data.entity.Group
import com.ivanovsky.passnotes.data.entity.Note
import com.ivanovsky.passnotes.presentation.Screen
import com.ivanovsky.passnotes.presentation.core.BaseFragment
import com.ivanovsky.passnotes.presentation.core.ScreenDisplayingMode
import com.ivanovsky.passnotes.presentation.core.ScreenState
import com.ivanovsky.passnotes.presentation.core.livedata.SingleLiveEvent
import com.ivanovsky.passnotes.presentation.group.GroupActivity
import com.ivanovsky.passnotes.presentation.note.NoteActivity
import com.ivanovsky.passnotes.presentation.note_editor.NoteEditorActivity
import java.util.*

class GroupsFragment : BaseFragment(), GroupsContract.View {

    override var presenter: GroupsContract.Presenter? = null

    private lateinit var adapter: GroupsAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var fab: FloatingActionButton
    private val itemsData = MutableLiveData<List<GroupsAdapter.ListItem>>()
    private val showNoteScreenEvent = SingleLiveEvent<Note>()
    private val showNoteListScreenEvent = SingleLiveEvent<Group>()
    private val showNewGroupScreenEvent = SingleLiveEvent<UUID>()
    private val showNewNoteScreenEvent = SingleLiveEvent<UUID>()
    private val showNewEntryDialogEvent = SingleLiveEvent<Unit>()

    override fun onStart() {
        super.onStart()
        presenter?.start()
    }

    override fun onDestroy() {
        super.onDestroy()
        presenter?.destroy()
    }

    override fun onCreateContentView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.groups_fragment, container, false)

        val context = view.context

        recyclerView = view.findViewById(R.id.recycler_view)
        fab = view.findViewById(R.id.fab)

        val layoutManager = GridLayoutManager(context, 3)
        recyclerView.layoutManager = layoutManager

        adapter = GroupsAdapter(context)
        adapter.onListItemClickListener = { position -> presenter?.onListItemClicked(position) }

        recyclerView.adapter = adapter

        fab.setOnClickListener { presenter?.onAddButtonClicked() }

        presenter?.globalSnackbarMessageAction?.observe(viewLifecycleOwner, Screen.GROUPS,
            Observer { message -> showSnackbar(message) })

        itemsData.observe(viewLifecycleOwner,
            Observer { items -> setItemsInternal(items) })
        showNoteListScreenEvent.observe(viewLifecycleOwner,
            Observer { group ->  showNoteListScreenInternal(group) })
        showNoteScreenEvent.observe(viewLifecycleOwner,
            Observer { note -> showNoteScreenInternal(note)})
        showNewGroupScreenEvent.observe(viewLifecycleOwner,
            Observer { parentGroupUid -> showNewGroupScreenInternal(parentGroupUid) })
        showNewNoteScreenEvent.observe(viewLifecycleOwner,
            Observer { parentGroupUid -> showNewNoteScreenInternal(parentGroupUid) })
        showNewEntryDialogEvent.observe(viewLifecycleOwner,
            Observer { showNewEntryDialogInternal() })

        return view
    }

    override fun getContentContainerId(): Int {
        return R.id.recycler_view
    }

    override fun onScreenStateChanged(screenState: ScreenState) {
        when (screenState.displayingMode) {
            ScreenDisplayingMode.EMPTY,
            ScreenDisplayingMode.DISPLAYING_DATA_WITH_ERROR_PANEL,
            ScreenDisplayingMode.DISPLAYING_DATA -> fab.show()

            ScreenDisplayingMode.LOADING, ScreenDisplayingMode.ERROR -> fab.hide()
        }
    }

    override fun setItems(items: List<GroupsAdapter.ListItem>) {
        itemsData.value = items
    }

    private fun setItemsInternal(items: List<GroupsAdapter.ListItem>) {
        adapter.setItems(items)
        adapter.notifyDataSetChanged()
    }

    override fun showNoteListScreen(group: Group) {
        showNoteListScreenEvent.call(group)
    }

    private fun showNoteListScreenInternal(group: Group) {
        val context = this.context ?: return

        startActivity(GroupsActivity.intentFroGroup(context, group))
    }

    override fun showNoteScreen(note: Note) {
        showNoteScreenEvent.call(note)
    }

    private fun showNoteScreenInternal(note: Note) {
        val context = this.context ?: return

        startActivity(NoteActivity.createStartIntent(context, note))
    }

    override fun showNewGroupScreen(parentGroupUid: UUID) {
        showNewGroupScreenEvent.call(parentGroupUid)
    }

    private fun showNewGroupScreenInternal(parentGroupUid: UUID) {
        val context = this.context ?: return

        startActivity(GroupActivity.createChildGroup(context, parentGroupUid))
    }

    override fun showNewNoteScreen(parentGroupUid: UUID) {
        showNewNoteScreenEvent.call(parentGroupUid)
    }

    private fun showNewNoteScreenInternal(parentGroupUid: UUID) {
        val context = this.context ?: return

        startActivity(NoteEditorActivity.intentForNewNote(context, parentGroupUid))
    }

    override fun showNewEntryDialog() {
        showNewEntryDialogEvent.call()
    }

    private fun showNewEntryDialogInternal() {
        val dialog = NewEntryDialog()
        dialog.onItemClickListener = { itemIdx ->
            // TODO: create constants for buttons
            if (itemIdx == 0) {
                presenter?.onCreateNewGroupClicked()
            } else if (itemIdx == 1) {
                presenter?.onCreateNewNoteClicked()
            }
        }
        dialog.show(parentFragmentManager, NewEntryDialog.TAG)
    }
}