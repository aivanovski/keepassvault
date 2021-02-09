package com.ivanovsky.passnotes.presentation.note

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.data.entity.Note
import com.ivanovsky.passnotes.presentation.core_mvvm.extensions.initActionBar
import com.ivanovsky.passnotes.presentation.core_mvvm.extensions.requireExtraValue
import java.util.*

class NoteActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.core_base_activity)

        val noteUid = intent.extras?.getSerializable(EXTRA_NOTE_UID) as? UUID
            ?: requireExtraValue(EXTRA_NOTE_TITLE)
        val noteTitle = intent.extras?.getString(EXTRA_NOTE_TITLE)
            ?: requireExtraValue(EXTRA_NOTE_TITLE)

        initActionBar(R.id.tool_bar)

        val fragment = NoteFragment.newInstance(noteUid, noteTitle)
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
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

        private const val EXTRA_NOTE_UID = "noteUid"
        private const val EXTRA_NOTE_TITLE = "noteTitle"

        fun createStartIntent(context: Context, note: Note): Intent {
            val result = Intent(context, NoteActivity::class.java)

            result.putExtra(EXTRA_NOTE_UID, note.uid)
            result.putExtra(EXTRA_NOTE_TITLE, note.title)

            return result
        }
    }
}