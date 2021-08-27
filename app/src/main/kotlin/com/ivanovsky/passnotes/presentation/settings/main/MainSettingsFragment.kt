package com.ivanovsky.passnotes.presentation.settings.main

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.github.terrakok.cicerone.Router
import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.injection.GlobalInjector.inject
import com.ivanovsky.passnotes.presentation.core.extensions.setupActionBar
import org.koin.androidx.viewmodel.ext.android.viewModel

class MainSettingsFragment :
    PreferenceFragmentCompat(),
    PreferenceFragmentCompat.OnPreferenceStartFragmentCallback {

    private val router: Router by inject()
    private val viewModel: MainSettingsViewModel by viewModel()

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        setupActionBar {
            title = getString(R.string.settings)
            setDisplayHomeAsUpEnabled(true)
            setHomeAsUpIndicator(null)
        }
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.main_settings, rootKey)
    }

    override fun onPreferenceStartFragment(
        caller: PreferenceFragmentCompat?,
        pref: Preference?
    ): Boolean {
        return true
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        subscribeToLiveData()

        viewModel.start()
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

    private fun subscribeToLiveData() {
        viewModel.isDatabaseOpened.observe(viewLifecycleOwner) {
            setDatabaseSettingsEnabled(it)
        }
    }

    private fun setDatabaseSettingsEnabled(isEnabled: Boolean) {
        val key = getString(R.string.pref_database_settings_fragment)
        preferenceScreen.findPreference<Preference>(key)?.isEnabled = isEnabled
    }

    companion object {
        fun newInstance() = MainSettingsFragment()
    }
}