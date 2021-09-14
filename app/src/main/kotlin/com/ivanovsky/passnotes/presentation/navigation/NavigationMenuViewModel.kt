package com.ivanovsky.passnotes.presentation.navigation

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.github.terrakok.cicerone.Router
import com.ivanovsky.passnotes.BuildConfig
import com.ivanovsky.passnotes.injection.GlobalInjector
import com.ivanovsky.passnotes.presentation.Screens.AboutScreen
import com.ivanovsky.passnotes.presentation.Screens.DebugMenuScreen
import com.ivanovsky.passnotes.presentation.Screens.MainSettingsScreen
import com.ivanovsky.passnotes.presentation.Screens.UnlockScreen
import com.ivanovsky.passnotes.presentation.navigation.mode.NavigationItem
import com.ivanovsky.passnotes.presentation.navigation.mode.NavigationItem.ABOUT
import com.ivanovsky.passnotes.presentation.navigation.mode.NavigationItem.DEBUG_MENU
import com.ivanovsky.passnotes.presentation.navigation.mode.NavigationItem.LOCK
import com.ivanovsky.passnotes.presentation.navigation.mode.NavigationItem.SETTINGS
import com.ivanovsky.passnotes.presentation.navigation.mode.NavigationItem.SELECT_FILE

class NavigationMenuViewModel(
    private val router: Router
) : ViewModel() {

    val isNavigationMenuEnabled = MutableLiveData(false)
    val visibleItems = MutableLiveData<List<NavigationItem>>()

    fun setNavigationEnabled(isEnabled: Boolean) {
        isNavigationMenuEnabled.value = isEnabled
    }

    fun setVisibleItems(items: List<NavigationItem>) {
        visibleItems.value = items
    }

    fun onMenuItemSelected(item: NavigationItem) {
        when (item) {
            SELECT_FILE -> {
                router.backTo(UnlockScreen())
            }
            LOCK -> {
                router.backTo(UnlockScreen())
            }
            SETTINGS -> {
                router.navigateTo(MainSettingsScreen())
            }
            ABOUT -> {
                router.navigateTo(AboutScreen())
            }
            DEBUG_MENU -> {
                router.navigateTo(DebugMenuScreen())
            }
        }
    }

    companion object {
        @Suppress("UNCHECKED_CAST")
        val FACTORY = object : ViewModelProvider.Factory {
            override fun <T : ViewModel?> create(modelClass: Class<T>): T {
                return GlobalInjector.get<NavigationMenuViewModel>() as T
            }
        }

        fun createNavigationItemsForBasicScreens(): List<NavigationItem> {
            return mutableListOf<NavigationItem>().apply {
                add(SELECT_FILE)
                add(SETTINGS)
                if (BuildConfig.DEBUG) {
                    add(DEBUG_MENU)
                }
                add(ABOUT)
            }
        }

        fun createNavigationItemsForDbScreens(): List<NavigationItem> {
            return mutableListOf<NavigationItem>().apply {
                add(LOCK)
                add(SETTINGS)
                if (BuildConfig.DEBUG) {
                    add(DEBUG_MENU)
                }
                add(ABOUT)
            }
        }
    }
}