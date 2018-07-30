package com.ivanovsky.passnotes.presentation.addeditnote

import android.databinding.DataBindingUtil
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.data.entity.Note
import com.ivanovsky.passnotes.databinding.AddEditNoteFragmentBinding
import com.ivanovsky.passnotes.presentation.core.BaseFragment
import com.ivanovsky.passnotes.presentation.core.FragmentState

class AddEditNoteFragment: BaseFragment(),
		AddEditNoteContract.View {

	private lateinit var binding: AddEditNoteFragmentBinding
	private lateinit var presenter: AddEditNoteContract.Presenter

	companion object {

		fun newInstance(): AddEditNoteFragment {
			return AddEditNoteFragment()
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
				R.layout.add_edit_note_fragment,
				container, false)

		return binding.root
	}

	override fun setPresenter(presenter: AddEditNoteContract.Presenter?) {
		this.presenter = presenter!!
	}

	override fun showNote(note: Note) {
	}

	override fun editNote(note: Note) {
	}

	override fun showError(message: String) {
		setErrorText(message)
		state = FragmentState.ERROR
	}
}