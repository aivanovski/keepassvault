package com.ivanovsky.passnotes.presentation.settings.app

import android.os.Bundle
import android.view.MenuItem
import androidx.preference.PreferenceFragmentCompat
import com.github.terrakok.cicerone.Router
import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.data.repository.settings.Settings
import com.ivanovsky.passnotes.data.repository.settings.SettingsImpl.Pref.AUTO_CLEAR_CLIPBOARD_DELAY_IN_MS
import com.ivanovsky.passnotes.data.repository.settings.SettingsImpl.Pref.AUTO_LOCK_DELAY_IN_MS
import com.ivanovsky.passnotes.data.repository.settings.SettingsImpl.Pref.IS_LOCK_NOTIFICATION_VISIBLE
import com.ivanovsky.passnotes.injection.GlobalInjector.inject
import com.ivanovsky.passnotes.presentation.core.extensions.setupActionBar

class AppSettingsFragment : PreferenceFragmentCompat() {

    private val settings: Settings by inject()
    private val router: Router by inject()

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        setupActionBar {
            title = getString(R.string.application_settings)
            setDisplayHomeAsUpEnabled(true)
            setHomeAsUpIndicator(null)
        }
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        settings.initDefaultIfNeed(IS_LOCK_NOTIFICATION_VISIBLE)
        settings.initDefaultIfNeed(AUTO_LOCK_DELAY_IN_MS)
        settings.initDefaultIfNeed(AUTO_CLEAR_CLIPBOARD_DELAY_IN_MS)

        setPreferencesFromResource(R.xml.application_settings, rootKey)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                router.exit()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    companion object {
        fun newInstance() = AppSettingsFragment()
    }
}