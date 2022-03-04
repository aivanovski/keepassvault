package com.ivanovsky.passnotes.presentation.settings.app

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.core.content.FileProvider
import androidx.preference.Preference
import androidx.preference.SwitchPreferenceCompat
import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.data.repository.settings.Settings
import com.ivanovsky.passnotes.data.repository.settings.SettingsImpl.Pref.AUTO_CLEAR_CLIPBOARD_DELAY_IN_MS
import com.ivanovsky.passnotes.data.repository.settings.SettingsImpl.Pref.AUTO_LOCK_DELAY_IN_MS
import com.ivanovsky.passnotes.data.repository.settings.SettingsImpl.Pref.IS_FILE_LOG_ENABLED
import com.ivanovsky.passnotes.data.repository.settings.SettingsImpl.Pref.IS_LOCK_NOTIFICATION_VISIBLE
import com.ivanovsky.passnotes.data.repository.settings.SettingsImpl.Pref.IS_POSTPONED_SYNC_ENABLED
import com.ivanovsky.passnotes.injection.GlobalInjector.inject
import com.ivanovsky.passnotes.presentation.core.BasePreferenceFragment
import com.ivanovsky.passnotes.presentation.core.extensions.setupActionBar
import com.ivanovsky.passnotes.presentation.core.extensions.showErrorDialog
import com.ivanovsky.passnotes.presentation.core.extensions.showToastMessage
import com.ivanovsky.passnotes.presentation.core.extensions.throwPreferenceNotFound
import com.ivanovsky.passnotes.presentation.core.preference.CustomDialogPreference
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.io.File

class AppSettingsFragment : BasePreferenceFragment() {

    private val settings: Settings by inject()
    private val viewModel: AppSettingsViewModel by viewModel()

    private lateinit var isFileLogEnabledPref: SwitchPreferenceCompat
    private lateinit var isPostponedSyncEnabledPref: SwitchPreferenceCompat
    private lateinit var sendLogFilePref: Preference
    private lateinit var removeLogFilesPref: Preference
    private lateinit var categoryGeneral: Preference
    private lateinit var categoryNotifications: Preference
    private lateinit var categoryLogging: Preference
    private lateinit var progressPref: Preference

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        setupActionBar {
            title = getString(R.string.application_settings)
            setDisplayHomeAsUpEnabled(true)
            setHomeAsUpIndicator(null)
        }
    }

    override fun onStart() {
        super.onStart()
        navigationViewModel.setNavigationEnabled(true)
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        // Init default values to make is visible for UI components
        settings.initDefaultIfNeed(IS_LOCK_NOTIFICATION_VISIBLE)
        settings.initDefaultIfNeed(AUTO_LOCK_DELAY_IN_MS)
        settings.initDefaultIfNeed(AUTO_CLEAR_CLIPBOARD_DELAY_IN_MS)
        settings.initDefaultIfNeed(IS_FILE_LOG_ENABLED)
        settings.initDefaultIfNeed(IS_POSTPONED_SYNC_ENABLED)

        setPreferencesFromResource(R.xml.application_settings, rootKey)

        progressPref = findPreference(getString(R.string.pref_progress))
            ?: throwPreferenceNotFound(R.string.pref_progress)

        sendLogFilePref = findPreference(getString(R.string.pref_send_log_file))
            ?: throwPreferenceNotFound(R.string.pref_send_log_file)

        removeLogFilesPref = findPreference(getString(R.string.pref_remove_log_files))
            ?: throwPreferenceNotFound(R.string.pref_remove_log_files)

        isFileLogEnabledPref = findPreference(getString(R.string.pref_is_file_log_enabled))
            ?: throwPreferenceNotFound(R.string.pref_is_file_log_enabled)

        isPostponedSyncEnabledPref = findPreference(getString(R.string.pref_is_postponed_sync_enabled))
            ?: throwPreferenceNotFound(R.string.pref_is_postponed_sync_enabled)

        categoryGeneral = findPreference(getString(R.string.pref_category_general))
            ?: throwPreferenceNotFound(R.string.pref_category_general)

        categoryNotifications = findPreference(getString(R.string.pref_category_notifications))
            ?: throwPreferenceNotFound(R.string.pref_category_notifications)

        categoryLogging = findPreference(getString(R.string.pref_category_logging))
            ?: throwPreferenceNotFound(R.string.pref_category_logging)

        isFileLogEnabledPref.setOnPreferenceChangeListener { _, newValue ->
            val isEnabled = (newValue as? Boolean) ?: false
            viewModel.onFileLogEnabledChanged(isEnabled)
            true
        }

        isPostponedSyncEnabledPref.setOnPreferenceChangeListener { _, newValue ->
            val isEnabled = (newValue as? Boolean) ?: false
            viewModel.onPostponedSyncEnabledChanged(isEnabled)
            true
        }
    }

    override fun onDisplayPreferenceDialog(preference: Preference?) {
        if (preference is CustomDialogPreference) {
            onCustomDialogPreferenceClicked(preference)
        } else {
            super.onDisplayPreferenceDialog(preference)
        }
    }

    private fun onCustomDialogPreferenceClicked(preference: Preference) {
        when (preference) {
            sendLogFilePref -> {
                viewModel.onSendLongFileClicked()
            }
            removeLogFilesPref -> {
                viewModel.onRemoveLogFilesClicked()
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                viewModel.navigateBack()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        subscribeToLiveData()
        subscribeToEvents()

        viewModel.start()
    }

    private fun subscribeToLiveData() {
        viewModel.isLoading.observe(viewLifecycleOwner) {
            setProgressVisibility(it)
        }
        viewModel.isSendLogFileEnabled.observe(viewLifecycleOwner) {
            sendLogFilePref.isEnabled = it
        }
        viewModel.isRemoveLogFilesEnabled.observe(viewLifecycleOwner) {
            removeLogFilesPref.isEnabled = it
        }
    }

    private fun subscribeToEvents() {
        viewModel.showErrorDialogEvent.observe(viewLifecycleOwner) {
            showErrorDialog(it)
        }
        viewModel.shareFileEvent.observe(viewLifecycleOwner) {
            shareFile(it)
        }
        viewModel.showToastEvent.observe(viewLifecycleOwner) {
            showToastMessage(it)
        }
    }

    private fun setProgressVisibility(isProgressVisible: Boolean) {
        progressPref.isVisible = isProgressVisible
        categoryGeneral.isVisible = !isProgressVisible
        categoryNotifications.isVisible = !isProgressVisible
        categoryLogging.isVisible = !isProgressVisible
    }

    private fun shareFile(file: File) {
        val uri = FileProvider.getUriForFile(requireContext(), requireContext().packageName, file)

        val intent = Intent()
            .apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_STREAM, uri)
                type = "text/plain"
            }

        startActivity(Intent.createChooser(intent, null))
    }

    companion object {
        fun newInstance() = AppSettingsFragment()
    }
}