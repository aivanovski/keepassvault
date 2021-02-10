package com.ivanovsky.passnotes.presentation.note

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.observe
import com.ivanovsky.passnotes.data.entity.Note
import com.ivanovsky.passnotes.databinding.NoteFragmentBinding
import com.ivanovsky.passnotes.presentation.core_mvvm.extensions.requireArgument
import com.ivanovsky.passnotes.presentation.core_mvvm.extensions.setupActionBar
import com.ivanovsky.passnotes.presentation.core_mvvm.extensions.showSnackbarMessage
import com.ivanovsky.passnotes.presentation.core_mvvm.extensions.withArguments
import com.ivanovsky.passnotes.presentation.note_editor.NoteEditorActivity
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.util.*

class NoteFragment : Fragment() {

    private val viewModel: NoteViewModel by viewModel()

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        setupActionBar {
            title = arguments?.getString(ARG_NOTE_TITLE) ?: requireArgument(ARG_NOTE_TITLE)
            setDisplayHomeAsUpEnabled(true)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return NoteFragmentBinding.inflate(inflater, container, false)
            .also {
                it.lifecycleOwner = viewLifecycleOwner
                it.viewModel = viewModel
            }
            .root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        viewModel.actionBarTitle.observe(viewLifecycleOwner) { actionBarTitle ->
            setupActionBar { 
                title = actionBarTitle
            }
        }
        viewModel.showEditNoteScreenEvent.observe(viewLifecycleOwner) { note ->
            showEditNoteScreen(note)
        }
        viewModel.showSnackbarMessageEvent.observe(viewLifecycleOwner) { message ->
            showSnackbarMessage(message)
        }


        val noteUid = arguments?.getSerializable(ARG_NOTE_UID) as? UUID
            ?: requireArgument(ARG_NOTE_UID)

        viewModel.start(noteUid)
    }

    private fun showEditNoteScreen(note: Note) {
        val noteUid = note.uid ?: return

        val intent = NoteEditorActivity.intentForEditNote(requireContext(), noteUid, note.title)
        startActivity(intent)
    }

    companion object {

        private const val ARG_NOTE_UID = "noteUid"
        private const val ARG_NOTE_TITLE = "noteTitle"

        fun newInstance(noteUid: UUID, noteTitle: String) = NoteFragment().withArguments {
            putSerializable(ARG_NOTE_UID, noteUid)
            putString(ARG_NOTE_TITLE, noteTitle)
        }
    }
}