package com.ivanovsky.passnotes.presentation.settings.database

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.preference.Preference
import androidx.preference.SwitchPreferenceCompat
import com.github.terrakok.cicerone.Router
import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.injection.GlobalInjector.inject
import com.ivanovsky.passnotes.presentation.ApplicationLaunchMode
import com.ivanovsky.passnotes.presentation.Screens
import com.ivanovsky.passnotes.presentation.core.BasePreferenceFragment
import com.ivanovsky.passnotes.presentation.core.DatabaseInteractionWatcher
import com.ivanovsky.passnotes.presentation.core.dialog.selectorDialog.SelectorDialog
import com.ivanovsky.passnotes.presentation.core.dialog.selectorDialog.SelectorDialogArgs
import com.ivanovsky.passnotes.presentation.core.extensions.setupActionBar
import com.ivanovsky.passnotes.presentation.core.extensions.showErrorDialog
import com.ivanovsky.passnotes.presentation.core.extensions.throwPreferenceNotFound
import com.ivanovsky.passnotes.presentation.core.preference.CustomDialogPreference
import com.ivanovsky.passnotes.presentation.settings.database.changePassword.ChangePasswordDialog
import com.ivanovsky.passnotes.presentation.unlock.UnlockScreenArgs
import org.koin.androidx.viewmodel.ext.android.viewModel

class DatabaseSettingsFragment : BasePreferenceFragment() {

    private val router: Router by inject()
    private val viewModel: DatabaseSettingsViewModel by viewModel()

    private lateinit var isRecycleBinEnabledPref: SwitchPreferenceCompat
    private lateinit var recycleBinGroupPref: Preference
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

        recycleBinGroupPref = findPreference(getString(R.string.pref_recycle_bin_group))
            ?: throwPreferenceNotFound(R.string.pref_recycle_bin_group)

        changePasswordPref = findPreference(getString(R.string.pref_change_password))
            ?: throwPreferenceNotFound(R.string.pref_change_password)

        isRecycleBinEnabledPref.setOnPreferenceChangeListener { _, newValue ->
            val isEnabled = (newValue as? Boolean) ?: false

            viewModel.onRecycleBinEnabledChanged(isEnabled)
            true
        }
    }

    override fun onDisplayPreferenceDialog(preference: Preference) {
        if (preference is CustomDialogPreference) {
            onCustomDialogPreferenceClicked(preference)
        } else {
            super.onDisplayPreferenceDialog(preference)
        }
    }

    private fun onCustomDialogPreferenceClicked(preference: Preference) {
        when (preference) {
            changePasswordPref -> {
                val dialog = ChangePasswordDialog.newInstance()
                dialog.show(childFragmentManager, ChangePasswordDialog.TAG)
            }

            recycleBinGroupPref -> {
                viewModel.onRecycleBinGroupClicked()
            }
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
            recycleBinGroupPref.isEnabled = it
        }
        viewModel.recycleBinSummary.observe(viewLifecycleOwner) {
            recycleBinGroupPref.summary = it
        }
    }

    private fun subscribeToEvents() {
        viewModel.showErrorDialogEvent.observe(viewLifecycleOwner) {
            showErrorDialog(it)
        }
        viewModel.showSelectRecycleBinGroupEvent.observe(viewLifecycleOwner) {
            showSelectRecycleBinGroupDialog(it)
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
        recycleBinGroupPref.isVisible = !isVisible
        changePasswordPref.isVisible = !isVisible
    }

    private fun showSelectRecycleBinGroupDialog(args: SelectorDialogArgs) {
        val dialog = SelectorDialog.newInstance(
            args = args,
            onItemSelected = { index ->
                viewModel.onRecycleBinGroupSelected(index)
            }
        )
        dialog.show(childFragmentManager, SelectorDialog.TAG)
    }

    companion object {
        fun newInstance() = DatabaseSettingsFragment()
    }
}