package com.ivanovsky.passnotes.presentation.settings

import com.github.terrakok.cicerone.Router
import com.ivanovsky.passnotes.presentation.Screens
import com.ivanovsky.passnotes.presentation.settings.app.AppSettingsFragment
import com.ivanovsky.passnotes.presentation.settings.database.DatabaseSettingsFragment
import com.ivanovsky.passnotes.util.ReflectionUtils

class SettingsRouter(private val router: Router) {

    fun navigateTo(settingsFragmentName: String) {
        when (ReflectionUtils.getClassByName(settingsFragmentName)) {
            AppSettingsFragment::class.java -> {
                router.navigateTo(Screens.AppSettingsScreen())
            }

            DatabaseSettingsFragment::class.java -> {
                router.navigateTo(Screens.DatabaseSettingsScreen())
            }

            else -> throw IllegalArgumentException(
                "Unable to find screen for settings fragment with name: $settingsFragmentName"
            )
        }
    }
}