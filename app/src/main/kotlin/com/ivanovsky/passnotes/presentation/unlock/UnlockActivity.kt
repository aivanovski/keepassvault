package com.ivanovsky.passnotes.presentation.unlock

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.navigation.NavigationView
import com.ivanovsky.passnotes.BuildConfig
import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.presentation.core.BaseActivity

class UnlockActivity : BaseActivity() {

    private lateinit var drawer: DrawerLayout
    private lateinit var navigationView: NavigationView
    private lateinit var presenter: UnlockPresenter

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

        presenter = UnlockPresenter(fragment)
        fragment.presenter = presenter

        navigationView.setNavigationItemSelectedListener { item -> onNavigationItemSelected(item) }

        if (!BuildConfig.DEBUG) {
            navigationView.menu.findItem(R.id.menu_debug_menu).isVisible = false
        }
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

            R.id.menu_debug_menu -> {
                drawer.closeDrawer(GravityCompat.START)
                presenter.onDebugMenuClicked()
                true
            }

            else -> false
        }
    }

    companion object {

        fun createStartIntent(context: Context): Intent {
            return Intent(context, UnlockActivity::class.java)
        }
    }
}
