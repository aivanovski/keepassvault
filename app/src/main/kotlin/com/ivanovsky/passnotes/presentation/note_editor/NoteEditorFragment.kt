package com.ivanovsky.passnotes.presentation.note_editor

import android.os.Bundle
import android.view.*
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.presentation.core.BaseFragment
import com.ivanovsky.passnotes.presentation.note_editor.view.BaseDataItem
import com.ivanovsky.passnotes.presentation.note_editor.view.NoteEditorView
import com.ivanovsky.passnotes.presentation.note_editor.view.text.TextDataItem

class NoteEditorFragment : BaseFragment(), NoteEditorContract.View {

    override var presenter: NoteEditorContract.Presenter? = null
    private var editorItemsData = MutableLiveData<List<BaseDataItem>>()
    private val doneButtonVisibilityData = MutableLiveData<Boolean>()
    private var menu: Menu? = null
    private lateinit var fab: FloatingActionButton
    private lateinit var editorView: NoteEditorView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

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
        val view = inflater.inflate(R.layout.note_editor_fragment, container, false)

        editorView = view.findViewById(R.id.note_editor_view)
        fab = view.findViewById(R.id.fab)

        editorItemsData.observe(viewLifecycleOwner,
            Observer { items -> setEditorItemsInternal(items) })
        doneButtonVisibilityData.observe(viewLifecycleOwner,
            Observer { isVisible -> setDoneButtonVisibilityInternal(isVisible) })

        return view
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        this.menu = menu

        inflater.inflate(R.menu.base_done, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return if (item.itemId == R.id.menu_done) {
            onDoneMenuClicked()
            true
        } else {
            super.onOptionsItemSelected(item)
        }
    }

    override fun setEditorItems(items: List<BaseDataItem>) {
        editorItemsData.value = items
    }

    private fun setEditorItemsInternal(items: List<BaseDataItem>) {
        for (item in items) {
            editorView.addItem(item)
        }
    }

    override fun setDoneButtonVisibility(isVisible: Boolean) {
        doneButtonVisibilityData.value = isVisible
    }

    private fun setDoneButtonVisibilityInternal(isVisible: Boolean) {
        val item = menu?.findItem(R.id.menu_done) ?: return

        item.isVisible = isVisible
    }

    private fun onDoneMenuClicked() {
        presenter?.onDoneButtonClicked(editorView.getItems())
    }
}