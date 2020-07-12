package com.ivanovsky.passnotes.presentation.note

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.data.entity.Note
import com.ivanovsky.passnotes.data.entity.Property
import com.ivanovsky.passnotes.data.entity.PropertyType
import com.ivanovsky.passnotes.domain.entity.PropertySpreader
import com.ivanovsky.passnotes.presentation.core.BaseFragment
import com.ivanovsky.passnotes.presentation.core.livedata.SingleLiveEvent
import com.ivanovsky.passnotes.presentation.note_editor.NoteEditorActivity
import com.ivanovsky.passnotes.util.formatAccordingSystemLocale
import java.util.*

class NoteFragment : BaseFragment(),
    NoteContract.View {

    override var presenter: NoteContract.Presenter? = null
    private val actionBarTitleData = MutableLiveData<String>()
    private val showEditNoteScreenEvent = SingleLiveEvent<Note>()
    private lateinit var adapter: NoteAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var modifiedTextView: TextView

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
        val view = inflater.inflate(R.layout.note_fragment, container, false)

        recyclerView = view.findViewById(R.id.recycler_view)
        modifiedTextView = view.findViewById(R.id.modified_date)
        val fab: View = view.findViewById(R.id.fab)

        val layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)

        adapter = NoteAdapter(requireContext())

        recyclerView.layoutManager = layoutManager
        recyclerView.adapter = adapter

        fab.setOnClickListener { presenter?.onEditNoteButtonClicked() }

        showEditNoteScreenEvent.observe(viewLifecycleOwner,
            Observer { note -> showEditNoteScreenInternal(note) })
        actionBarTitleData.observe(viewLifecycleOwner,
            Observer { title -> setActionBarTitleInternal(title) })

        return view
    }

    override fun showNote(note: Note) {
        val propertySpreader = PropertySpreader(note.properties ?: emptyList())
        val adapterItems = createAdapterItemsFromProperties(propertySpreader.visibleProperties)

        adapter.setItems(adapterItems)
        adapter.notifyDataSetChanged()

        adapter.onCopyButtonClickListener =
            { position -> onCopyButtonClicked(propertySpreader.visibleProperties[position]) }

        modifiedTextView.text = formatModifiedDate(note.modified)
    }

    private fun formatModifiedDate(edited: Date): String {
        return getString(R.string.edited_at, edited.formatAccordingSystemLocale(context!!))
    }

    private fun createAdapterItemsFromProperties(properties: List<Property>): List<NoteAdapter.Item> {
        val items = mutableListOf<NoteAdapter.Item>()

        for (property in properties) {
            val isVisibilityButtonVisible = (property.type == PropertyType.PASSWORD)

            items.add(
                NoteAdapter.NotePropertyItem(
                    property.name ?: "",
                    property.value ?: "",
                    isVisibilityButtonVisible,
                    isVisibilityButtonVisible
                )
            )
        }

        return items
    }

    private fun onCopyButtonClicked(property: Property) {
        presenter?.onCopyToClipboardClicked(property.value ?: "")
    }

    override fun showEditNoteScreen(note: Note) {
        showEditNoteScreenEvent.call(note)
    }

    private fun showEditNoteScreenInternal(note: Note) {
        val noteUid = note.uid ?: return

        val intent = NoteEditorActivity.intentForEditNote(requireContext(), noteUid, note.title)
        startActivity(intent)
    }

    override fun setActionBarTitle(title: String) {
        actionBarTitleData.value = title
    }

    private fun setActionBarTitleInternal(title: String) {
        val activity = this.activity as? AppCompatActivity ?: return

        activity.supportActionBar?.title = title
    }
}