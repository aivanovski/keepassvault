package com.ivanovsky.passnotes.presentation.groups

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.injection.Injector
import com.ivanovsky.passnotes.presentation.core.BaseActivity

class GroupsActivity : BaseActivity() {

	companion object {

		fun createStartIntent(context: Context): Intent {
			return Intent(context, GroupsActivity::class.java)
		}
	}

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		Injector.getInstance().appComponent.inject(this)

		setContentView(R.layout.core_base_activity)

		setSupportActionBar(findViewById(R.id.tool_bar))
		currentActionBar.title = getString(R.string.groups)
		currentActionBar.setDisplayHomeAsUpEnabled(true)

		val fragment = GroupsFragment.newInstance()

		supportFragmentManager.beginTransaction()
				.replace(R.id.fragment_container, fragment)
				.commit()

		val presenter = GroupsPresenter(fragment)
		fragment.presenter = presenter
	}

	override fun onOptionsItemSelected(item: MenuItem?): Boolean {
		if (item?.itemId == android.R.id.home) {
			Injector.getInstance().releaseEncryptedDatabaseComponent()

			finish()

			return true
		} else {
			return super.onOptionsItemSelected(item)
		}
	}
}
