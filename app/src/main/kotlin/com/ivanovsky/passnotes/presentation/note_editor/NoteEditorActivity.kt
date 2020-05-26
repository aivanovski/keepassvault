package com.ivanovsky.passnotes.presentation.note_editor

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.presentation.core.BaseActivity
import com.ivanovsky.passnotes.presentation.note_editor.NoteEditorContract.LaunchMode
import java.util.*

class NoteEditorActivity : BaseActivity() {

    private var noteUid: UUID? = null
    private var groupUid: UUID? = null
    private lateinit var noteTitle: String
    private lateinit var launchMode: LaunchMode

    object ExtraKeys {
        const val GROUP_UID = "groupUid"
        const val NOTE_UID = "noteUid"
        const val NOTE_TITLE = "noteTitle"
        const val LAUNCH_MODE = "launchMode"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.core_base_activity)

        readExtraArgs()

        setSupportActionBar(findViewById(R.id.tool_bar))
        currentActionBar.title = noteTitle
        currentActionBar.setDisplayHomeAsUpEnabled(true)

        val fragment = NoteEditorFragment()
        val presenter = NoteEditorPresenter(fragment, launchMode, groupUid, noteUid)
        fragment.presenter = presenter

        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }

    private fun readExtraArgs() {
        val extras = intent.extras ?: return

        val launchModeValue = extras.getString(ExtraKeys.LAUNCH_MODE)
            ?: throw IllegalArgumentException("Incorrect launchMode value")

        noteTitle = extras.getString(ExtraKeys.NOTE_TITLE)
            ?: throw IllegalArgumentException("Incorrect noteTitle value")

        noteUid = extras.getSerializable(ExtraKeys.NOTE_UID) as? UUID
        groupUid = extras.getSerializable(ExtraKeys.GROUP_UID) as? UUID
        launchMode = LaunchMode.valueOf(launchModeValue)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return if (item.itemId == android.R.id.home) {
            finish()
            true
        } else {
            super.onOptionsItemSelected(item)
        }
    }

    companion object {

        fun intentForEditNote(context: Context, noteUid: UUID, noteTitle: String?): Intent {
            val intent = Intent(context, NoteEditorActivity::class.java)
            intent.putExtra(ExtraKeys.NOTE_TITLE, noteTitle ?: "")
            intent.putExtra(ExtraKeys.NOTE_UID, noteUid)
            intent.putExtra(ExtraKeys.LAUNCH_MODE, LaunchMode.EDIT.name)
            return intent
        }

        fun intentForNewNote(context: Context, groupUid: UUID): Intent {
            val intent = Intent(context, NoteEditorActivity::class.java)
            intent.putExtra(ExtraKeys.NOTE_TITLE, context.getString(R.string.new_note))
            intent.putExtra(ExtraKeys.GROUP_UID, groupUid)
            intent.putExtra(ExtraKeys.LAUNCH_MODE, LaunchMode.NEW.name)
            return intent
        }
    }
}