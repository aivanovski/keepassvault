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
import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.injection.GlobalInjector.inject
import com.ivanovsky.passnotes.presentation.Screens.UnlockScreen
import com.ivanovsky.passnotes.presentation.core.extensions.initActionBar
import com.ivanovsky.passnotes.presentation.navigation.NavigationMenuViewModel
import com.ivanovsky.passnotes.presentation.navigation.mode.NavigationItem
import com.ivanovsky.passnotes.presentation.settings.SettingsRouter

class MainActivity :
    AppCompatActivity(),
    PreferenceFragmentCompat.OnPreferenceStartFragmentCallback {

    private lateinit var drawer: DrawerLayout
    private lateinit var navigationView: NavigationView

    private val navigatorHolder: NavigatorHolder by inject()
    private val router: Router by inject()
    private val settingsRouter: SettingsRouter by inject()
    private val navigator = AppNavigator(this, R.id.fragment_container)

    private val navigationViewModel: NavigationMenuViewModel by lazy {
        ViewModelProvider(this, NavigationMenuViewModel.FACTORY)
            .get(NavigationMenuViewModel::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.core_base_activity_with_side_menu)

        initActionBar(R.id.tool_bar)

        drawer = findViewById(R.id.drawer_layout)
        navigationView = findViewById(R.id.navigation_view)

        router.newRootScreen(UnlockScreen())

        navigationView.setNavigationItemSelectedListener { item -> onNavigationItemSelected(item) }

        subscribeToLiveData()
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

    private fun subscribeToLiveData() {
        navigationViewModel.isNavigationMenuEnabled.observe(this) {
            setDrawerEnabled(it)
        }
        navigationViewModel.visibleItems.observe(this) {
            setVisibleNavigationItems(it)
        }
    }

    private fun setDrawerEnabled(isEnabled: Boolean) {
        if (isEnabled) {
            drawer.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)
        } else {
            drawer.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
        }
    }

    private fun setVisibleNavigationItems(visibleItems: List<NavigationItem>) {
        for (item in NavigationItem.values()) {
            val isVisible = visibleItems.contains(item)
            navigationView.menu.findItem(item.menuId).isVisible = isVisible
        }
    }

    private fun onNavigationItemSelected(item: MenuItem): Boolean {
        val navigationItem = NavigationItem.findByMenuId(item.itemId)
            ?: throw IllegalStateException()

        navigationViewModel.onMenuItemSelected(navigationItem)
        drawer.closeDrawer(GravityCompat.START)

        return true
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
