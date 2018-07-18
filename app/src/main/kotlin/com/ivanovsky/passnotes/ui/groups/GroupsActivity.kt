package com.ivanovsky.passnotes.ui.groups

import android.content.Context
import android.content.Intent
import android.databinding.DataBindingUtil
import android.os.Bundle
import android.view.MenuItem
import com.ivanovsky.passnotes.App
import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.data.safedb.EncryptedDatabaseProvider
import com.ivanovsky.passnotes.databinding.CoreBaseActivityBinding
import com.ivanovsky.passnotes.ui.core.BaseActivity
import javax.inject.Inject

class GroupsActivity : BaseActivity() {

	@Inject
	lateinit var dbProvider: EncryptedDatabaseProvider

	companion object {

		fun createStartIntent(context: Context): Intent {
			return Intent(context, GroupsActivity::class.java)
		}
	}

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		App.getDaggerComponent().inject(this)

		val binding = DataBindingUtil.setContentView<CoreBaseActivityBinding>(this,
				R.layout.core_base_activity)

		setSupportActionBar(binding.toolBar)
		currentActionBar.title = getString(R.string.groups)
		currentActionBar.setDisplayHomeAsUpEnabled(true)

		val fragment = GroupsFragment.newInstance()

		supportFragmentManager.beginTransaction()
				.replace(R.id.fragment_container, fragment)
				.commit()

		val presenter = GroupsPresenter(this, fragment)
		fragment.setPresenter(presenter)
	}

	override fun onOptionsItemSelected(item: MenuItem?): Boolean {
		if (item?.itemId == android.R.id.home) {

			if (dbProvider.isOpened) {
				dbProvider.close()
			}

			finish()

			return true
		} else {
			return super.onOptionsItemSelected(item)
		}
	}
}
