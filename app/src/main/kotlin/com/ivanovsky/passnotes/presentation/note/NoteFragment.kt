package com.ivanovsky.passnotes.presentation.note

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.data.entity.Note
import com.ivanovsky.passnotes.data.entity.Property
import com.ivanovsky.passnotes.data.entity.PropertyType
import com.ivanovsky.passnotes.domain.entity.PropertySpreader
import com.ivanovsky.passnotes.presentation.core.BaseFragment
import com.ivanovsky.passnotes.util.formatAccordingSystemLocale
import java.util.*

class NoteFragment : BaseFragment(),
    NoteContract.View {

    override lateinit var presenter: NoteContract.Presenter
    private lateinit var adapter: NoteAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var modifiedTextView: TextView

    override fun onStart() {
        super.onStart()
        presenter.start()
    }

    override fun onDestroy() {
        super.onDestroy()
        presenter.destroy()
    }

    override fun onCreateContentView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.note_fragment, container, false)

        recyclerView = view.findViewById(R.id.recycler_view)
        modifiedTextView = view.findViewById(R.id.modified_date)

        val layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)

        adapter = NoteAdapter(context!!)

        recyclerView.layoutManager = layoutManager
        recyclerView.adapter = adapter

        return view
    }

    override fun showNote(note: Note) {
        val propertySpreader = PropertySpreader(note.properties)
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
                    property.name,
                    property.value,
                    isVisibilityButtonVisible,
                    isVisibilityButtonVisible
                )
            )
        }

        return items
    }

    private fun onCopyButtonClicked(property: Property) {
        presenter.onCopyToClipboardClicked(property.value)
    }
}