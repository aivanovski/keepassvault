package com.ivanovsky.passnotes.presentation.storagelist

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.presentation.core.BaseActivity

class StorageListActivity : BaseActivity() {

	private lateinit var action: Action

	companion object {

		const val EXTRA_RESULT: String = "result"

		private const val EXTRA_MODE = "action"

		fun createStartIntent(context: Context, action: Action): Intent {
			val intent = Intent(context, StorageListActivity::class.java)

			intent.putExtra(EXTRA_MODE, action)

			return intent
		}
	}

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.core_base_activity)

		action = intent.extras.getSerializable(EXTRA_MODE) as Action

		setSupportActionBar(findViewById(R.id.tool_bar))
		currentActionBar.title = getString(R.string.select_storage)
		currentActionBar.setDisplayHomeAsUpEnabled(true)

		val fragment = StorageListFragment.newInstance()
		supportFragmentManager.beginTransaction()
				.replace(R.id.fragment_container, fragment)
				.commit()

		val presenter = StorageListPresenter(action)
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