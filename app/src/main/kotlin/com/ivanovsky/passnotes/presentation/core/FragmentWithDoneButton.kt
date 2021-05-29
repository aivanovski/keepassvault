package com.ivanovsky.passnotes.presentation.core

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.fragment.app.Fragment
import com.ivanovsky.passnotes.R

abstract class FragmentWithDoneButton : Fragment() {

    private var menu: Menu? = null
    private var isDoneButtonVisible: Boolean? = null

    abstract fun onDoneMenuClicked()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        this.menu = menu
        inflater.inflate(R.menu.base_done, menu)

        isDoneButtonVisible?.let {
            setDoneButtonVisibility(it)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return if (item.itemId == R.id.menu_done) {
            onDoneMenuClicked()
            true
        } else {
            super.onOptionsItemSelected(item)
        }
    }

    protected fun setDoneButtonVisibility(isVisible: Boolean) {
        isDoneButtonVisible = isVisible

        val menu = this.menu ?: return

        val item = menu.findItem(R.id.menu_done) ?: return

        item.isVisible = isVisible
    }
}