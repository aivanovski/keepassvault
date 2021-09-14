package com.ivanovsky.passnotes.presentation.settings.database

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.preference.CheckBoxPreference
import androidx.preference.Preference
import com.github.terrakok.cicerone.Router
import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.injection.GlobalInjector.inject
import com.ivanovsky.passnotes.presentation.core.BasePreferenceFragment
import com.ivanovsky.passnotes.presentation.core.DatabaseInteractionWatcher
import com.ivanovsky.passnotes.presentation.core.dialog.ErrorDialog
import com.ivanovsky.passnotes.presentation.core.extensions.setupActionBar
import com.ivanovsky.passnotes.presentation.core.extensions.throwPreferenceNotFound
import org.koin.androidx.viewmodel.ext.android.viewModel

class DatabaseSettingsFragment : BasePreferenceFragment() {

    private val router: Router by inject()
    private val viewModel: DatabaseSettingsViewModel by viewModel()

    private lateinit var isRecycleBinEnabledPref: CheckBoxPreference
    private lateinit var progressPref: Preference

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        setupActionBar {
            title = getString(R.string.database_settings)
            setDisplayHomeAsUpEnabled(true)
            setHomeAsUpIndicator(null)
        }
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.database_settings)

        progressPref = findPreference(getString(R.string.pref_progress))
            ?: throwPreferenceNotFound(R.string.pref_progress)
        isRecycleBinEnabledPref = findPreference(getString(R.string.pref_is_recycle_bin_enabled))
            ?: throwPreferenceNotFound(R.string.pref_is_recycle_bin_enabled)

        isRecycleBinEnabledPref.setOnPreferenceChangeListener { _, newValue ->
            val isEnabled = (newValue as? Boolean) ?: false

            viewModel.onRecycleBinEnabledChanged(isEnabled)
            true
        }
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

    override fun onStart() {
        super.onStart()
        navigationViewModel.setNavigationEnabled(true)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewLifecycleOwner.lifecycle.addObserver(DatabaseInteractionWatcher(this))

        subscribeToLiveData()

        viewModel.start()
    }

    private fun subscribeToLiveData() {
        viewModel.isLoading.observe(viewLifecycleOwner) {
            setProgressVisible(it)
        }
        viewModel.errorMessage.observe(viewLifecycleOwner) {
            showError(it)
        }
        viewModel.isRecycleBinEnabled.observe(viewLifecycleOwner) {
            isRecycleBinEnabledPref.isChecked = it
        }
    }

    private fun setProgressVisible(isVisible: Boolean) {
        progressPref.isVisible = isVisible
        isRecycleBinEnabledPref.isVisible = !isVisible
    }

    private fun showError(message: String) {
        val dialog = ErrorDialog.newInstance(message)
        dialog.show(childFragmentManager, ErrorDialog.TAG)
    }

    companion object {
        fun newInstance() = DatabaseSettingsFragment()
    }
}