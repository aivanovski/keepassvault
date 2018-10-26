package com.ivanovsky.passnotes.presentation.unlock

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v4.widget.DrawerLayout
import android.view.MenuItem

import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.presentation.core.BaseActivity

class UnlockActivity : BaseActivity() {

	companion object {

		fun createStartIntent(context: Context): Intent {
			return Intent(context, UnlockActivity::class.java)
		}
	}

	lateinit var drawer: DrawerLayout

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		setContentView(R.layout.core_base_activity_with_side_menu)

		setSupportActionBar(findViewById(R.id.tool_bar))
		currentActionBar.title = getString(R.string.app_name)
		currentActionBar.setDisplayHomeAsUpEnabled(true)
		currentActionBar.setHomeAsUpIndicator(R.drawable.ic_add_white_24dp)

		drawer = findViewById(R.id.drawer_layout)

		val fragment = UnlockFragment.newInstance()
		supportFragmentManager.beginTransaction()
				.replace(R.id.fragment_container, fragment)
				.commit()

		val presenter = UnlockPresenter(this, fragment)
		fragment.setPresenter(presenter)
	}

	override fun onOptionsItemSelected(item: MenuItem): Boolean {
		return when (item.itemId) {
			android.R.id.home -> {
				drawer.showContextMenu()
				true
			}
			else -> super.onOptionsItemSelected(item)
		}
	}
}
