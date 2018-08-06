package com.ivanovsky.passnotes.presentation.note

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.data.entity.Note
import com.ivanovsky.passnotes.presentation.core.BaseActivity
import java.util.*

class NoteActivity : BaseActivity() {

	private var noteUid: UUID? = null
	private lateinit var noteTitle: String

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

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		setContentView(R.layout.core_base_activity)

		noteUid = intent.extras.getSerializable(EXTRA_NOTE_UID) as UUID
		noteTitle = intent.extras.getString(EXTRA_NOTE_TITLE)

		setSupportActionBar(findViewById(R.id.tool_bar))
		currentActionBar.title = noteTitle
		currentActionBar.setDisplayHomeAsUpEnabled(true)

		val fragment = NoteFragment.newInstance()
		supportFragmentManager.beginTransaction()
				.replace(R.id.fragment_container, fragment)
				.commit()

		val presenter = NotePresenter(this, noteUid, fragment)
		fragment.setPresenter(presenter)
	}

	override fun onOptionsItemSelected(item: MenuItem?): Boolean {
		if (item?.itemId == android.R.id.home) {
			finish()
			return true
		} else {
			return super.onOptionsItemSelected(item)
		}
	}
}