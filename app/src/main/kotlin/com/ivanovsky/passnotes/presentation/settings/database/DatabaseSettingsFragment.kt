package com.ivanovsky.passnotes.presentation.settings.database

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.lifecycle.observe
import androidx.preference.CheckBoxPreference
import androidx.preference.Preference
import com.github.terrakok.cicerone.Router
import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.injection.GlobalInjector.inject
import com.ivanovsky.passnotes.presentation.ApplicationLaunchMode
import com.ivanovsky.passnotes.presentation.Screens
import com.ivanovsky.passnotes.presentation.core.BasePreferenceFragment
import com.ivanovsky.passnotes.presentation.core.DatabaseInteractionWatcher
import com.ivanovsky.passnotes.presentation.core.extensions.setupActionBar
import com.ivanovsky.passnotes.presentation.core.extensions.showErrorDialog
import com.ivanovsky.passnotes.presentation.core.extensions.throwPreferenceNotFound
import com.ivanovsky.passnotes.presentation.core.preference.CustomDialogPreference
import com.ivanovsky.passnotes.presentation.settings.database.change_password.ChangePasswordDialog
import com.ivanovsky.passnotes.presentation.unlock.UnlockScreenArgs
import org.koin.androidx.viewmodel.ext.android.viewModel

class DatabaseSettingsFragment : BasePreferenceFragment() {

    private val router: Router by inject()
    private val viewModel: DatabaseSettingsViewModel by viewModel()

    private lateinit var isRecycleBinEnabledPref: CheckBoxPreference
    private lateinit var progressPref: Preference
    private lateinit var changePasswordPref: Preference

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
        changePasswordPref = findPreference(getString(R.string.pref_change_password))
            ?: throwPreferenceNotFound(R.string.pref_change_password)

        isRecycleBinEnabledPref.setOnPreferenceChangeListener { _, newValue ->
            val isEnabled = (newValue as? Boolean) ?: false

            viewModel.onRecycleBinEnabledChanged(isEnabled)
            true
        }
    }

    override fun onDisplayPreferenceDialog(preference: Preference?) {
        if (preference is CustomDialogPreference &&
            preference.key == getString(R.string.pref_change_password)
        ) {
            val dialog = ChangePasswordDialog.newInstance()
            dialog.show(childFragmentManager, ChangePasswordDialog.TAG)
        } else {
            super.onDisplayPreferenceDialog(preference)
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
        subscribeToEvents()

        viewModel.start()
    }

    private fun subscribeToLiveData() {
        viewModel.isLoading.observe(viewLifecycleOwner) {
            setProgressVisible(it)
        }
        viewModel.isRecycleBinEnabled.observe(viewLifecycleOwner) {
            isRecycleBinEnabledPref.isChecked = it
        }
    }

    private fun subscribeToEvents() {
        viewModel.showErrorDialogEvent.observe(viewLifecycleOwner) {
            showErrorDialog(it)
        }
        viewModel.lockScreenEvent.observe(viewLifecycleOwner) {
            router.backTo(
                Screens.UnlockScreen(
                    args = UnlockScreenArgs(ApplicationLaunchMode.NORMAL)
                )
            )
        }
    }

    private fun setProgressVisible(isVisible: Boolean) {
        progressPref.isVisible = isVisible
        isRecycleBinEnabledPref.isVisible = !isVisible
        changePasswordPref.isVisible = !isVisible
    }

    companion object {
        fun newInstance() = DatabaseSettingsFragment()
    }
}