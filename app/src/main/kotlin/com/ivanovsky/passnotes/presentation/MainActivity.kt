package com.ivanovsky.passnotes.presentation

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.ViewModelProvider
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.github.terrakok.cicerone.NavigatorHolder
import com.github.terrakok.cicerone.Router
import com.github.terrakok.cicerone.androidx.AppNavigator
import com.google.android.material.navigation.NavigationView
import com.ivanovsky.passnotes.BuildConfig
import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.injection.GlobalInjector.inject
import com.ivanovsky.passnotes.presentation.Screens.UnlockScreen
import com.ivanovsky.passnotes.presentation.core.extensions.initActionBar
import com.ivanovsky.passnotes.presentation.settings.SettingsRouter
import com.ivanovsky.passnotes.presentation.unlock.UnlockViewModel

class MainActivity :
    AppCompatActivity(),
    PreferenceFragmentCompat.OnPreferenceStartFragmentCallback {

    private lateinit var drawer: DrawerLayout
    private lateinit var navigationView: NavigationView

    private val navigatorHolder: NavigatorHolder by inject()
    private val router: Router by inject()
    private val settingsRouter: SettingsRouter by inject()
    private val navigator = AppNavigator(this, R.id.fragment_container)

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

        router.newRootScreen(UnlockScreen())

        navigationView.setNavigationItemSelectedListener { item -> onNavigationItemSelected(item) }

        if (!BuildConfig.DEBUG) {
            navigationView.menu.findItem(R.id.menu_debug_menu).isVisible = false
        }
    }

    override fun onPause() {
        navigatorHolder.removeNavigator()
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        navigatorHolder.setNavigator(navigator)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val handledByFragment = supportFragmentManager.fragments.any { it.onOptionsItemSelected(item) }
        if (handledByFragment) {
            return true
        }

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
                viewModel.navigateToFilePicker()
                true
            }

            R.id.menu_settings -> {
                drawer.closeDrawer(GravityCompat.START)
                viewModel.onSettingsMenuClicked()
                true
            }

            R.id.menu_about -> {
                drawer.closeDrawer(GravityCompat.START)
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

    override fun onPreferenceStartFragment(
        caller: PreferenceFragmentCompat?,
        pref: Preference?
    ): Boolean {
        val settingsFragmentName = pref?.fragment ?: throw IllegalStateException()
        settingsRouter.navigateTo(settingsFragmentName)
        return true
    }

    companion object {

        fun createStartIntent(context: Context): Intent {
            return Intent(context, MainActivity::class.java)
        }
    }
}
