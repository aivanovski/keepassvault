package com.ivanovsky.passnotes.presentation.unlock

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.navigation.NavigationView
import com.ivanovsky.passnotes.BuildConfig
import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.presentation.core.extensions.initActionBar

class UnlockActivity : AppCompatActivity() {

    private lateinit var drawer: DrawerLayout
    private lateinit var navigationView: NavigationView

    private val viewModel: UnlockViewModel by lazy {
        ViewModelProvider(this, UnlockViewModel.FACTORY)
            .get(UnlockViewModel::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.core_base_activity_with_side_menu)

        initActionBar(R.id.tool_bar)

        drawer = findViewById(R.id.drawer_layout)
        navigationView = findViewById(R.id.navigation_view)

        val fragment = UnlockFragment()
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()

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
                viewModel.onOpenFileMenuClicked()
                true
            }

            R.id.menu_settings -> {
                viewModel.onSettingsMenuClicked()
                true
            }

            R.id.menu_about -> {
                viewModel.onAboutMenuClicked()
                true
            }

            R.id.menu_debug_menu -> {
                drawer.closeDrawer(GravityCompat.START)
                viewModel.onDebugMenuClicked()
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
