package com.ivanovsky.passnotes.presentation.notes

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.data.entity.Note
import com.ivanovsky.passnotes.presentation.core.BaseFragment
import com.ivanovsky.passnotes.presentation.core.adapter.SingleLineAdapter
import com.ivanovsky.passnotes.presentation.group.GroupActivity
import com.ivanovsky.passnotes.presentation.note.NoteActivity

class NotesFragment: BaseFragment(), NotesContract.View {

	override lateinit var presenter: NotesContract.Presenter
	private lateinit var adapter: SingleLineAdapter
	private lateinit var recyclerView: RecyclerView

	companion object {

		fun newInstance(): NotesFragment {
			return NotesFragment()
		}
	}

	override fun onStart() {
		super.onStart()
		presenter.start()
	}

	override fun onDestroy() {
		super.onDestroy()
		presenter.destroy()
	}

	override fun onCreateContentView(inflater: LayoutInflater, container: ViewGroup?,
	                                 savedInstanceState: Bundle?): View {
		val view = inflater.inflate(R.layout.notes_fragment, container, false)

		recyclerView = view.findViewById(R.id.recycler_view)

		val layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
		val dividerDecorator = DividerItemDecoration(context, layoutManager.orientation)

		adapter = SingleLineAdapter(context!!)

		recyclerView.layoutManager = layoutManager
		recyclerView.addItemDecoration(dividerDecorator)
		recyclerView.adapter = adapter

		return view
	}

	override fun showNotes(notes: List<Note>) {
		adapter.setItems(createAdapterItems(notes))
		adapter.notifyDataSetChanged()
		adapter.onItemClickListener = { position -> showNoteScreen(notes[position]) }
	}

	private fun createAdapterItems(notes: List<Note>): List<SingleLineAdapter.Item> {
		return notes.map { note -> SingleLineAdapter.Item(note.title) }
	}

	override fun showUnlockScreenAndFinish() {
		startActivity(GroupActivity.createStartIntent(context!!))

		activity!!.finish()
	}

	override fun showNoteScreen(note: Note) {
		startActivity(NoteActivity.createStartIntent(activity!!, note))
	}
}