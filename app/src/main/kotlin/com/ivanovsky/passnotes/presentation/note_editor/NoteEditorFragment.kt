package com.ivanovsky.passnotes.presentation.note_editor

import android.os.Bundle
import android.view.*
import androidx.activity.OnBackPressedCallback
import androidx.activity.addCallback
import androidx.fragment.app.Fragment
import androidx.lifecycle.observe
import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.databinding.NoteEditorFragmentBinding
import com.ivanovsky.passnotes.presentation.core.dialog.ConfirmationDialog
import com.ivanovsky.passnotes.presentation.core.extensions.*
import org.koin.androidx.viewmodel.ext.android.viewModel

class NoteEditorFragment : Fragment() {

    private val viewModel: NoteEditorViewModel by viewModel()
    private var menu: Menu? = null
    private var backCallback: OnBackPressedCallback? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        this.menu = menu

        inflater.inflate(R.menu.base_done, menu)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return NoteEditorFragmentBinding.inflate(inflater, container, false)
            .also {
                it.lifecycleOwner = viewLifecycleOwner
                it.viewModel = viewModel
            }
            .root
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_done -> {
                viewModel.onDoneMenuClicked()
                true
            }
            android.R.id.home -> {
                viewModel.onBackClicked()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onStart() {
        super.onStart()
        backCallback = requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            viewModel.onBackClicked()
        }
    }

    override fun onStop() {
        super.onStop()
        backCallback?.remove()
        backCallback = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val args = (arguments?.getParcelable(ARGUMENTS) as? NoteEditorArgs)
            ?: requireArgument(ARGUMENTS)

        setupActionBar {
            args.title?.let {
                title = it
            }
            setDisplayHomeAsUpEnabled(true)
        }

        subscribeToLiveData()

        viewModel.start(args)
    }

    private fun subscribeToLiveData() {
        viewModel.isDoneButtonVisible.observe(viewLifecycleOwner) { isVisible ->
            setDoneButtonVisibility(isVisible)
        }
        viewModel.showDiscardDialogEvent.observe(viewLifecycleOwner) { message ->
            showDiscardDialog(message)
        }
        viewModel.showToastEvent.observe(viewLifecycleOwner) { message ->
            showToastMessage(message)
        }
        viewModel.finishScreenEvent.observe(viewLifecycleOwner) {
            finishActivity()
        }
        viewModel.hideKeyboardEvent.observe(viewLifecycleOwner) {
            hideKeyboard()
        }
    }

    private fun setDoneButtonVisibility(isVisible: Boolean) {
        val item = menu?.findItem(R.id.menu_done) ?: return

        item.isVisible = isVisible
    }

    private fun showDiscardDialog(message: String) {
        val dialog = ConfirmationDialog.newInstance(
            message,
            getString(R.string.discard),
            getString(R.string.cancel)
        )

        dialog.onConfirmationLister = {
            viewModel.onDiscardConfirmed()
        }

        dialog.show(childFragmentManager, ConfirmationDialog.TAG)
    }

    companion object {

        private const val ARGUMENTS = "arguments"

        fun newInstance(args: NoteEditorArgs) = NoteEditorFragment().withArguments {
            putParcelable(ARGUMENTS, args)
        }
    }
}