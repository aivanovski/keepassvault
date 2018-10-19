package com.ivanovsky.passnotes.presentation.unlock

import android.content.Context
import android.content.Intent
import android.os.Bundle

import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.presentation.core.BaseActivity

class UnlockActivity : BaseActivity() {

	companion object {

		fun createStartIntent(context: Context): Intent {
			return Intent(context, UnlockActivity::class.java)
		}
	}

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		setContentView(R.layout.core_base_activity)

		setSupportActionBar(findViewById(R.id.tool_bar))
		currentActionBar.title = getString(R.string.app_name)

		val fragment = UnlockFragment.newInstance()
		supportFragmentManager.beginTransaction()
				.replace(R.id.fragment_container, fragment)
				.commit()

		val presenter = UnlockPresenter(this, fragment)
		fragment.setPresenter(presenter)
	}
}
