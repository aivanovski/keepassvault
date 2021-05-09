package com.ivanovsky.passnotes.presentation.note_editor

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.data.entity.Template
import com.ivanovsky.passnotes.presentation.core_mvvm.extensions.initActionBar
import com.ivanovsky.passnotes.presentation.core_mvvm.extensions.requireExtraValue
import java.util.*

class NoteEditorActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.core_base_activity)

        val args = (intent.extras?.getParcelable(EXTRA_ARGUMENTS) as? NoteEditorArgs)
            ?: requireExtraValue(EXTRA_ARGUMENTS)

        initActionBar(R.id.tool_bar)

        val fragment = NoteEditorMVVMFragment.newInstance(args)

        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }

//    override fun onOptionsItemSelected(item: MenuItem): Boolean {
//        return if (item.itemId == android.R.id.home) {
////            presenter.onBackPressed() // TODO: implement
//            true
//        } else {
//            super.onOptionsItemSelected(item)
//        }
//    }

//    override fun onBackPressed() {
////        presenter.onBackPressed() // TODO: implement
//    }

    companion object {

        private const val EXTRA_ARGUMENTS = "argmunets"

        fun intentForEditNote(context: Context, noteUid: UUID, noteTitle: String?): Intent {
            return Intent(context, NoteEditorActivity::class.java).apply {
                putExtra(
                    EXTRA_ARGUMENTS,
                    NoteEditorArgs(
                        launchMode = LaunchMode.EDIT,
                        noteUid = noteUid,
                        title = noteTitle
                    )
                )
            }
        }

        fun intentForNewNote(context: Context, groupUid: UUID, template: Template?): Intent {
            return Intent(context, NoteEditorActivity::class.java).apply {
                putExtra(
                    EXTRA_ARGUMENTS,
                    NoteEditorArgs(
                        launchMode = LaunchMode.NEW,
                        groupUid = groupUid,
                        template = template,
                        title = context.getString(R.string.new_note)
                    )
                )
            }
        }
    }
}