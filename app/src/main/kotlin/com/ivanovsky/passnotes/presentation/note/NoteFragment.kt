package com.ivanovsky.passnotes.presentation.note

import android.databinding.DataBindingUtil
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.data.entity.Note
import com.ivanovsky.passnotes.data.entity.Property
import com.ivanovsky.passnotes.data.entity.PropertyType
import com.ivanovsky.passnotes.databinding.NoteFragmentBinding
import com.ivanovsky.passnotes.domain.entity.PropertySpreader
import com.ivanovsky.passnotes.presentation.core.BaseFragment
import com.ivanovsky.passnotes.presentation.core.FragmentState
import com.ivanovsky.passnotes.util.formatAccordingSystemLocale
import java.util.*

class NoteFragment : BaseFragment(),
		NoteContract.View {

	private lateinit var binding: NoteFragmentBinding
	private lateinit var presenter: NoteContract.Presenter
	private lateinit var adapter: NoteAdapter

	companion object {

		fun newInstance(): NoteFragment {
			return NoteFragment()
		}
	}

	override fun onResume() {
		super.onResume()
		presenter.start()
	}

	override fun onPause() {
		super.onPause()
		presenter.stop()
	}

	override fun onCreateContentView(inflater: LayoutInflater?,
									 container: ViewGroup?,
									 savedInstanceState: Bundle?): View {
		binding = DataBindingUtil.inflate(inflater,
				R.layout.note_fragment,
				container, false)

		val layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)

		adapter = NoteAdapter(context)

		binding.recyclerView.layoutManager = layoutManager
		binding.recyclerView.adapter = adapter

		return binding.root
	}

	override fun setPresenter(presenter: NoteContract.Presenter?) {
		this.presenter = presenter!!
	}

	override fun showNote(note: Note) {
		val propertySpreader = PropertySpreader(note.properties)
		val adapterItems = createAdapterItemsFromProperties(propertySpreader.visibleProperties)

		adapter.setItems(adapterItems)
		adapter.notifyDataSetChanged()

		adapter.onCopyButtonClickListener = {
			position -> onCopyButtonClicked(propertySpreader.visibleProperties[position]) }

		binding.modifiedTextView.text = formatModifiedDate(note.modified)
	}

	private fun formatModifiedDate(edited: Date): String {
		return getString(R.string.edited_at, edited.formatAccordingSystemLocale(context))
	}

	private fun createAdapterItemsFromProperties(properties: List<Property>): List<NoteAdapter.Item> {
		val items = mutableListOf<NoteAdapter.Item>()

		for (property in properties) {
			val isVisibilityButtonVisible = (property.type == PropertyType.PASSWORD)

			items.add(NoteAdapter.NotePropertyItem(property.name,
					property.value,
					isVisibilityButtonVisible,
					true,
					isVisibilityButtonVisible))
		}

		return items
	}

	private fun onCopyButtonClicked(property: Property) {
		presenter.onCopyToClipboardClicked(property.value)
	}

	override fun showError(message: String) {
		setErrorText(message)
		state = FragmentState.ERROR
	}
}