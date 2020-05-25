package com.ivanovsky.passnotes.presentation.group

import android.os.Bundle
import android.view.*
import android.widget.EditText
import androidx.lifecycle.Observer
import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.presentation.Screen
import com.ivanovsky.passnotes.presentation.core.BaseFragment
import com.ivanovsky.passnotes.util.InputMethodUtils.hideSoftInput

class GroupFragment : BaseFragment(), GroupContract.View {

    override lateinit var presenter: GroupContract.Presenter
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
        val view = inflater.inflate(R.layout.new_group_fragment, container, false)

        titleEditText = view.findViewById(R.id.group_title)

        presenter.doneButtonVisibility.observe(this,
            Observer { visibility -> setDoneButtonVisibility(visibility!!) })
        presenter.titleEditTextError.observe(this,
            Observer { error -> setTitleEditTextError(error) })
        presenter.finishScreenEvent.observe(this,
            Observer { finishScreen() })
        presenter.hideKeyboardEvent.observe(this,
            Observer { hideKeyboard() })
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