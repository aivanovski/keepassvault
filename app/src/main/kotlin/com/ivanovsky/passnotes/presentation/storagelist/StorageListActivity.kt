package com.ivanovsky.passnotes.presentation.storagelist

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.presentation.core.BaseActivity

class StorageListActivity : BaseActivity() {

	companion object {

		fun createStartIntent(context: Context): Intent {
			return Intent(context, StorageListActivity::class.java)
		}
	}

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.core_base_activity)

		setSupportActionBar(findViewById(R.id.tool_bar))
		currentActionBar.title = getString(R.string.select_storage)
		currentActionBar.setDisplayHomeAsUpEnabled(true)

		val fragment = StorageListFragment.newInstance()
		supportFragmentManager.beginTransaction()
				.replace(R.id.fragment_container, fragment)
				.commit()

		val presenter = StorageListPresenter(fragment)
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