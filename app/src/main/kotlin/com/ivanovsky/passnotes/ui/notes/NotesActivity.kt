package com.ivanovsky.passnotes.ui.notes

import android.content.Context
import android.content.Intent
import android.databinding.DataBindingUtil
import android.os.Bundle
import android.view.MenuItem
import com.ivanovsky.passnotes.App
import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.data.safedb.model.Group
import com.ivanovsky.passnotes.databinding.CoreBaseActivityBinding
import com.ivanovsky.passnotes.ui.core.BaseActivity
import java.util.*

class NotesActivity : BaseActivity() {

	private lateinit var groupTitle: String
	private lateinit var groupUid: UUID

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

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		App.getDaggerComponent().inject(this)

		val binding = DataBindingUtil.setContentView<CoreBaseActivityBinding>(this, R.layout.core_base_activity)

		groupUid = intent.extras.getSerializable(EXTRA_GROUP_UID) as UUID
		groupTitle = intent.extras.getString(EXTRA_GROUP_TITLE)

		setSupportActionBar(binding.toolBar)
		currentActionBar.title = groupTitle
		currentActionBar.setDisplayHomeAsUpEnabled(true)

		val fragment = NotesFragment.newInstance()
		supportFragmentManager.beginTransaction()
				.replace(R.id.fragment_container, fragment)
				.commit()

		val presenter = NotesPresenter(groupUid, this, fragment)
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