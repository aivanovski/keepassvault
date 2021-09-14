package com.ivanovsky.passnotes.presentation.note

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.observe
import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.databinding.NoteFragmentBinding
import com.ivanovsky.passnotes.extensions.setItemVisibility
import com.ivanovsky.passnotes.presentation.core.BaseFragment
import com.ivanovsky.passnotes.presentation.core.DatabaseInteractionWatcher
import com.ivanovsky.passnotes.presentation.core.extensions.requireArgument
import com.ivanovsky.passnotes.presentation.core.extensions.setupActionBar
import com.ivanovsky.passnotes.presentation.core.extensions.showSnackbarMessage
import com.ivanovsky.passnotes.presentation.core.extensions.withArguments
import com.ivanovsky.passnotes.util.StringUtils
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.util.UUID

class NoteFragment : BaseFragment() {

    private val viewModel: NoteViewModel by viewModel()
    private var menu: Menu? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        setupActionBar {
            title = StringUtils.EMPTY
            setHomeAsUpIndicator(null)
            setDisplayHomeAsUpEnabled(true)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        this.menu = menu

        inflater.inflate(R.menu.note, menu)

        viewModel.isMenuVisible.value?.let {
            menu.setItemVisibility(R.id.menu_lock, it)
            menu.setItemVisibility(R.id.menu_search, it)
            menu.setItemVisibility(R.id.menu_more, it)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                viewModel.navigateBack()
                true
            }
            R.id.menu_lock -> {
                viewModel.onLockButtonClicked()
                true
            }
            R.id.menu_search -> {
                viewModel.onSearchButtonClicked()
                true
            }
            R.id.menu_settings -> {
                viewModel.onSettingsButtonClicked()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return NoteFragmentBinding.inflate(inflater, container, false)
            .also {
                it.lifecycleOwner = viewLifecycleOwner
                it.viewModel = viewModel
            }
            .root
    }

    override fun onStart() {
        super.onStart()
        navigationViewModel.setNavigationEnabled(true)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewLifecycleOwner.lifecycle.addObserver(DatabaseInteractionWatcher(this))

        subscribeToLiveData()

        val noteUid = arguments?.getSerializable(ARG_NOTE_UID) as? UUID
            ?: requireArgument(ARG_NOTE_UID)

        viewModel.start(noteUid)
    }

    private fun subscribeToLiveData() {
        viewModel.isMenuVisible.observe(viewLifecycleOwner) {
            menu?.setItemVisibility(R.id.menu_lock, it)
            menu?.setItemVisibility(R.id.menu_search, it)
            menu?.setItemVisibility(R.id.menu_more, it)
        }
        viewModel.actionBarTitle.observe(viewLifecycleOwner) { actionBarTitle ->
            setupActionBar {
                title = actionBarTitle
            }
        }
        viewModel.showSnackbarMessageEvent.observe(viewLifecycleOwner) { message ->
            showSnackbarMessage(message)
        }
    }

    companion object {

        private const val ARG_NOTE_UID = "noteUid"

        fun newInstance(noteUid: UUID) = NoteFragment().withArguments {
            putSerializable(ARG_NOTE_UID, noteUid)
        }
    }
}