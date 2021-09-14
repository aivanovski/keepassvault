package com.ivanovsky.passnotes.presentation.navigation.mode

import androidx.annotation.IdRes
import com.ivanovsky.passnotes.R

enum class NavigationItem(@IdRes val menuId: Int) {

    SELECT_FILE(R.id.menu_select_file),
    SETTINGS(R.id.menu_settings),
    DEBUG_MENU(R.id.menu_debug_menu),
    ABOUT(R.id.menu_about),
    LOCK(R.id.menu_lock);

    companion object {
        fun findByMenuId(@IdRes menuId: Int): NavigationItem? {
            return values()
                .firstOrNull { menuId == it.menuId }
        }
    }
}