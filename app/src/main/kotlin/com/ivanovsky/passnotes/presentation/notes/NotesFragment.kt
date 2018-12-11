package com.ivanovsky.passnotes.presentation.notes

import android.os.Bundle
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.data.entity.Note
import com.ivanovsky.passnotes.presentation.core.BaseFragment
import com.ivanovsky.passnotes.presentation.core.FragmentState
import com.ivanovsky.passnotes.presentation.core.adapter.SingleLineAdapter
import com.ivanovsky.passnotes.presentation.group.GroupActivity
import com.ivanovsky.passnotes.presentation.note.NoteActivity

class NotesFragment: BaseFragment(), NotesContract.View {

	private lateinit var presenter: NotesContract.Presenter
	private lateinit var adapter: SingleLineAdapter
	private lateinit var recyclerView: RecyclerView

	companion object {

		fun newInstance(): NotesFragment {
			return NotesFragment()
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

	override fun onCreateContentView(inflater: LayoutInflater, container: ViewGroup,
	                                 savedInstanceState: Bundle?): View {
		val view = inflater.inflate(R.layout.notes_fragment, container, false)

		recyclerView = view.findViewById(R.id.recycler_view)

		val layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
		val dividerDecorator = DividerItemDecoration(context, layoutManager.orientation)

		adapter = SingleLineAdapter(context)

		recyclerView.layoutManager = layoutManager
		recyclerView.addItemDecoration(dividerDecorator)
		recyclerView.adapter = adapter

		return view
	}

	override fun setPresenter(presenter: NotesContract.Presenter?) {
		this.presenter = presenter!!
	}

	override fun showNotes(notes: List<Note>) {
		adapter.setItems(createAdapterItems(notes))
		adapter.notifyDataSetChanged()
		adapter.onItemClickListener = { position -> showNoteScreen(notes[position]) }
		state = FragmentState.DISPLAYING_DATA
	}

	private fun createAdapterItems(notes: List<Note>): List<SingleLineAdapter.Item> {
		return notes.map { note -> SingleLineAdapter.Item(note.title) }
	}

	override fun showNotItems() {
		setEmptyText(getString(R.string.no_items_to_show))
		state = FragmentState.EMPTY
	}

	override fun showUnlockScreenAndFinish() {
		startActivity(GroupActivity.createStartIntent(context))

		activity.finish()
	}

	override fun showError(message: String) {
		setErrorText(message)
		state = FragmentState.ERROR
	}

	override fun showNoteScreen(note: Note) {
		startActivity(NoteActivity.createStartIntent(activity, note))
	}
}