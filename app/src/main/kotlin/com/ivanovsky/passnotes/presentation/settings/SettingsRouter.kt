package com.ivanovsky.passnotes.presentation.settings

import com.ivanovsky.passnotes.presentation.NewScreens
import com.ivanovsky.passnotes.presentation.core.navigation.Router
import com.ivanovsky.passnotes.presentation.settings.app.AppSettingsFragment
import com.ivanovsky.passnotes.presentation.settings.database.DatabaseSettingsFragment
import com.ivanovsky.passnotes.util.ReflectionUtils

class SettingsRouter(private val router: Router) {

    fun navigateTo(settingsFragmentName: String) {
        when (ReflectionUtils.getClassByName(settingsFragmentName)) {
            AppSettingsFragment::class.java -> {
                router.navigateTo(NewScreens.AppSettingsScreen())
            }

            DatabaseSettingsFragment::class.java -> {
                router.navigateTo(NewScreens.DatabaseSettingsScreen())
            }

            else -> throw IllegalArgumentException(
                "Unable to find screen for settings fragment with name: $settingsFragmentName"
            )
        }
    }
}
