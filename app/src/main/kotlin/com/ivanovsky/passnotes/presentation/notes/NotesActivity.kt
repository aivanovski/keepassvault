package com.ivanovsky.passnotes.presentation.notes

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.data.entity.Group
import com.ivanovsky.passnotes.presentation.core.BaseActivity
import java.util.*

class NotesActivity : BaseActivity() {

	private lateinit var groupTitle: String
	private lateinit var groupUid: UUID

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		setContentView(R.layout.core_base_activity)

		groupUid = intent.extras?.getSerializable(EXTRA_GROUP_UID) as UUID
		groupTitle = intent.extras?.getString(EXTRA_GROUP_TITLE) ?: ""

		setSupportActionBar(findViewById(R.id.tool_bar))
		currentActionBar.title = groupTitle
		currentActionBar.setDisplayHomeAsUpEnabled(true)

		val fragment = NotesFragment.newInstance()
		supportFragmentManager.beginTransaction()
				.replace(R.id.fragment_container, fragment)
				.commit()

		val presenter = NotesPresenter(groupUid, fragment)
		fragment.presenter = presenter
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

		private const val EXTRA_GROUP_UID = "groupUid"
		private const val EXTRA_GROUP_TITLE = "groupTitle"

		fun createStartIntent(context: Context, group: Group): Intent {
			val result = Intent(context, NotesActivity::class.java)

			result.putExtra(EXTRA_GROUP_UID, group.uid)
			result.putExtra(EXTRA_GROUP_TITLE, group.title)

			return result
		}
	}
}