package com.ivanovsky.passnotes.presentation.group

import androidx.lifecycle.Observer
import android.os.Bundle
import android.view.*
import android.widget.EditText
import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.presentation.Screen
import com.ivanovsky.passnotes.presentation.core.BaseFragment
import com.ivanovsky.passnotes.presentation.core.SnackbarMessage
import com.ivanovsky.passnotes.util.InputMethodUtils.hideSoftInput

class GroupFragment: BaseFragment(), GroupContract.View {

	private lateinit var presenter: GroupContract.Presenter
	private lateinit var menu: Menu
	private lateinit var titleEditText: EditText

	companion object {

		fun newInstance(): GroupFragment {
			return GroupFragment()
		}
	}

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setHasOptionsMenu(true)
	}

	override fun onResume() {
		super.onResume()
		presenter.start()
	}

	override fun onPause() {
		super.onPause()
		presenter.stop()
	}

	override fun onCreateContentView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
		val view  = inflater.inflate(R.layout.new_group_fragment, container, false)

		titleEditText = view.findViewById(R.id.group_title)

		presenter.screenState.observe(this,
				Observer { state -> setScreenState(state) })
		presenter.doneButtonVisibility.observe(this,
				Observer { visibility -> setDoneButtonVisibility(visibility!!)})
		presenter.titleEditTextError.observe(this,
				Observer { error -> setTitleEditTextError(error)})
		presenter.finishScreenAction.observe(this,
				Observer { finishScreen() })
		presenter.hideKeyboardAction.observe(this,
				Observer { hideKeyboard() })
		presenter.snackbarMessageAction.observe(this,
				Observer { message -> showSnackbar(SnackbarMessage(message))})
		presenter.globalSnackbarMessageAction.observe(this, Screen.GROUP,
				Observer { message -> showSnackbar(message) })

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

	private fun onDoneMenuClicked() {
		val title = titleEditText.text.toString()
		presenter.createNewGroup(title)
	}

	override fun setPresenter(presenter: GroupContract.Presenter) {
		this.presenter = presenter
	}

	override fun setTitleEditTextError(error: String?) {
		titleEditText.error = error
	}

	override fun setDoneButtonVisibility(isVisible: Boolean) {
		val item = menu.findItem(R.id.menu_done)
		if (item != null) {
			item.isVisible = isVisible
		}
	}

	override fun finishScreen() {
		activity?.finish()
	}

	override fun hideKeyboard() {
		hideSoftInput(activity)
	}
}