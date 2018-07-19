package com.ivanovsky.passnotes.ui.notes

import android.databinding.DataBindingUtil
import android.os.Bundle
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.LinearLayoutManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.data.entity.Note
import com.ivanovsky.passnotes.databinding.NotesFragmentBinding
import com.ivanovsky.passnotes.presentation.core.BaseFragment
import com.ivanovsky.passnotes.presentation.core.FragmentState
import com.ivanovsky.passnotes.presentation.core.adapter.SingleLineAdapter
import com.ivanovsky.passnotes.presentation.newgroup.NewGroupActivity

class NotesFragment: BaseFragment(), NotesContract.View {

	private lateinit var binding: NotesFragmentBinding
	private lateinit var presenter: NotesContract.Presenter
	private lateinit var adapter: SingleLineAdapter

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

	override fun onCreateContentView(inflater: LayoutInflater?, container: ViewGroup?,
	                                 savedInstanceState: Bundle?): View {
		binding = DataBindingUtil.inflate(inflater, R.layout.notes_fragment, container, false)

		val layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
		val dividerDecorator = DividerItemDecoration(context, layoutManager.orientation)

		adapter = SingleLineAdapter(context)

		binding.recyclerView.layoutManager = layoutManager
		binding.recyclerView.addItemDecoration(dividerDecorator)
		binding.recyclerView.adapter = adapter

		return binding.root
	}

	override fun setPresenter(presenter: NotesContract.Presenter?) {
		this.presenter = presenter!!
	}

	override fun showNotes(notes: List<Note>) {
		adapter.setItems(createAdapterItems(notes))
		adapter.notifyDataSetChanged()
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
		startActivity(NewGroupActivity.createStartIntent(context))

		activity.finish()
	}
}