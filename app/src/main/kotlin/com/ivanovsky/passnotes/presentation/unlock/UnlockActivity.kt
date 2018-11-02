package com.ivanovsky.passnotes.presentation.unlock

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.design.widget.NavigationView
import android.support.v4.view.GravityCompat
import android.support.v4.widget.DrawerLayout
import android.view.MenuItem

import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.presentation.core.BaseActivity

class UnlockActivity : BaseActivity() {

	private lateinit var drawer: DrawerLayout
	private lateinit var navigationView: NavigationView
	private lateinit var presenter: UnlockPresenter

	companion object {

		fun createStartIntent(context: Context): Intent {
			return Intent(context, UnlockActivity::class.java)
		}
	}

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		setContentView(R.layout.core_base_activity_with_side_menu)

		setSupportActionBar(findViewById(R.id.tool_bar))
		currentActionBar.title = getString(R.string.app_name)
		currentActionBar.setDisplayHomeAsUpEnabled(true)
		currentActionBar.setHomeAsUpIndicator(R.drawable.ic_menu_white_24dp)

		drawer = findViewById(R.id.drawer_layout)
		navigationView = findViewById(R.id.navigation_view)

		val fragment = UnlockFragment.newInstance()
		supportFragmentManager.beginTransaction()
				.replace(R.id.fragment_container, fragment)
				.commit()

		presenter = UnlockPresenter(this, fragment)
		fragment.setPresenter(presenter)

		navigationView.setNavigationItemSelectedListener { item -> onNavigationItemSelected(item)}
	}

	override fun onOptionsItemSelected(item: MenuItem): Boolean {
		return when (item.itemId) {
			android.R.id.home -> {
				drawer.openDrawer(GravityCompat.START)
				true
			}

			else -> super.onOptionsItemSelected(item)
		}
	}

	private fun onNavigationItemSelected(item: MenuItem): Boolean {
		return when (item.itemId) {
			R.id.menu_open_file -> {
				drawer.closeDrawer(GravityCompat.START)
				presenter.onOpenFileMenuClicked()
				true
			}

			R.id.menu_settings -> {
				presenter.onSettingsMenuClicked()
				true
			}

			R.id.menu_about -> {
				presenter.onAboutMenuClicked()
				true
			}

			else -> false
		}
	}
}
