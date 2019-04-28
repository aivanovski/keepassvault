package com.ivanovsky.passnotes.presentation.debugmenu

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.presentation.core.BaseActivity

class DebugMenuActivity : BaseActivity() {

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		setContentView(R.layout.core_base_activity)

		setSupportActionBar(findViewById(R.id.tool_bar))
		currentActionBar.title = getString(R.string.debug_menu)
		currentActionBar.setDisplayHomeAsUpEnabled(true)

		val fragment = DebugMenuFragment.newInstance()
		supportFragmentManager.beginTransaction()
				.replace(R.id.fragment_container, fragment)
				.commit()

		val presenter = DebugMenuPresenter(fragment)
		fragment.setPresenter(presenter)
	}

	override fun onOptionsItemSelected(item: MenuItem?): Boolean {
		return if (item?.itemId == android.R.id.home) {
			onBackPressed()
			true
		} else {
			super.onOptionsItemSelected(item)
		}
	}

	companion object {

		fun createStartIntent(context: Context): Intent {
			return Intent(context, DebugMenuActivity::class.java)
		}
	}
}