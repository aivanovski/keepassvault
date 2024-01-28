package com.ivanovsky.passnotes.presentation.settings.app

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.core.content.FileProvider
import androidx.core.view.isVisible
import androidx.preference.Preference
import androidx.preference.SwitchPreferenceCompat
import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.data.repository.settings.Settings
import com.ivanovsky.passnotes.data.repository.settings.SettingsImpl.Pref.AUTO_CLEAR_CLIPBOARD_DELAY_IN_MS
import com.ivanovsky.passnotes.data.repository.settings.SettingsImpl.Pref.AUTO_LOCK_DELAY_IN_MS
import com.ivanovsky.passnotes.data.repository.settings.SettingsImpl.Pref.IS_BIOMETRIC_UNLOCK_ENABLED
import com.ivanovsky.passnotes.data.repository.settings.SettingsImpl.Pref.IS_FILE_LOG_ENABLED
import com.ivanovsky.passnotes.data.repository.settings.SettingsImpl.Pref.IS_LOCK_NOTIFICATION_VISIBLE
import com.ivanovsky.passnotes.data.repository.settings.SettingsImpl.Pref.IS_POSTPONED_SYNC_ENABLED
import com.ivanovsky.passnotes.data.repository.settings.SettingsImpl.Pref.SEARCH_TYPE
import com.ivanovsky.passnotes.domain.PermissionHelper
import com.ivanovsky.passnotes.domain.entity.SystemPermission
import com.ivanovsky.passnotes.injection.GlobalInjector.inject
import com.ivanovsky.passnotes.presentation.core.BasePreferenceFragment
import com.ivanovsky.passnotes.presentation.core.extensions.requestSystemPermission
import com.ivanovsky.passnotes.presentation.core.extensions.setupActionBar
import com.ivanovsky.passnotes.presentation.core.extensions.showErrorDialog
import com.ivanovsky.passnotes.presentation.core.extensions.showToastMessage
import com.ivanovsky.passnotes.presentation.core.extensions.throwPreferenceNotFound
import com.ivanovsky.passnotes.presentation.core.permission.PermissionRequestResultReceiver
import com.ivanovsky.passnotes.presentation.core.preference.CustomDialogPreference
import com.ivanovsky.passnotes.util.getChildViews
import java.io.File
import org.koin.androidx.viewmodel.ext.android.viewModel

class AppSettingsFragment : BasePreferenceFragment(), PermissionRequestResultReceiver {

    private val settings: Settings by inject()
    private val permissionHelper: PermissionHelper by inject()
    private val viewModel: AppSettingsViewModel by viewModel()

    private lateinit var isFileLogEnabledPref: SwitchPreferenceCompat
    private lateinit var isPostponedSyncEnabledPref: SwitchPreferenceCompat
    private lateinit var isBiometricUnlockEnabledPref: SwitchPreferenceCompat
    private lateinit var sendLogFilePref: Preference
    private lateinit var removeLogFilesPref: Preference
    private lateinit var enableNotificationPermissionPref: Preference
    private lateinit var isLockNotificationVisiblePref: Preference
    private lateinit var categoryGeneral: Preference
    private lateinit var categoryBiometric: Preference
    private lateinit var categoryNotifications: Preference
    private lateinit var categoryLogging: Preference
    private lateinit var contentView: ViewGroup

    override fun onPermissionRequestResult(permission: SystemPermission, isGranted: Boolean) {
        when (permission) {
            SystemPermission.NOTIFICATION -> viewModel.onNotificationPermissionResult(isGranted)
            else -> throw IllegalArgumentException("Invalid permission received: $permission")
        }
    }

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
        // Init default values to make it visible for UI components
        listOf(
            IS_LOCK_NOTIFICATION_VISIBLE,
            AUTO_LOCK_DELAY_IN_MS,
            AUTO_CLEAR_CLIPBOARD_DELAY_IN_MS,
            IS_FILE_LOG_ENABLED,
            IS_POSTPONED_SYNC_ENABLED,
            IS_BIOMETRIC_UNLOCK_ENABLED,
            SEARCH_TYPE
        )
            .forEach { settings.initDefaultIfNeed(it) }

        setPreferencesFromResource(R.xml.application_settings, rootKey)

        sendLogFilePref = findPreference(getString(R.string.pref_send_log_file))
            ?: throwPreferenceNotFound(R.string.pref_send_log_file)

        removeLogFilesPref = findPreference(getString(R.string.pref_remove_log_files))
            ?: throwPreferenceNotFound(R.string.pref_remove_log_files)

        isFileLogEnabledPref = findPreference(getString(R.string.pref_is_file_log_enabled))
            ?: throwPreferenceNotFound(R.string.pref_is_file_log_enabled)

        isPostponedSyncEnabledPref = findPreference(
            getString(R.string.pref_is_postponed_sync_enabled)
        )
            ?: throwPreferenceNotFound(R.string.pref_is_postponed_sync_enabled)

        isBiometricUnlockEnabledPref = findPreference(
            getString(R.string.pref_is_biometric_unlock_enabled)
        )
            ?: throwPreferenceNotFound(R.string.pref_is_biometric_unlock_enabled)

        enableNotificationPermissionPref = findPreference(
            getString(R.string.pref_enable_notification_permission)
        )
            ?: throwPreferenceNotFound(R.string.pref_enable_notification_permission)

        isLockNotificationVisiblePref = findPreference(
            getString(R.string.pref_is_lock_notification_visible)
        )
            ?: throwPreferenceNotFound(R.string.pref_is_lock_notification_visible)

        categoryGeneral = findPreference(getString(R.string.pref_category_general))
            ?: throwPreferenceNotFound(R.string.pref_category_general)

        categoryBiometric = findPreference(getString(R.string.pref_category_biometrics))
            ?: throwPreferenceNotFound(R.string.pref_category_biometrics)

        categoryNotifications = findPreference(getString(R.string.pref_category_notifications))
            ?: throwPreferenceNotFound(R.string.pref_category_notifications)

        categoryLogging = findPreference(getString(R.string.pref_category_logging))
            ?: throwPreferenceNotFound(R.string.pref_category_logging)

        isFileLogEnabledPref.setOnPreferenceChangeListener { _, newValue ->
            viewModel.onFileLogEnabledChanged(newValue as Boolean)
            true
        }

        isPostponedSyncEnabledPref.setOnPreferenceChangeListener { _, newValue ->
            viewModel.onPostponedSyncEnabledChanged(newValue as Boolean)
            true
        }

        isBiometricUnlockEnabledPref.setOnPreferenceChangeListener { _, newValue ->
            viewModel.onBiometricUnlockEnabledChanged(newValue as Boolean)
            true
        }

        categoryBiometric.isVisible = viewModel.isBiometricUnlockAvailable()
        enableNotificationPermissionPref.isVisible =
            viewModel.isEnableNotificationPermissionVisible.value ?: false
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = super.onCreateView(inflater, container, savedInstanceState)

        contentView = view.findViewById(android.R.id.list_container)

        inflater.inflate(R.layout.core_progress_bar, contentView, true)

        return view
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
            sendLogFilePref -> {
                viewModel.onSendLongFileClicked()
            }

            removeLogFilesPref -> {
                viewModel.onRemoveLogFilesClicked()
            }

            enableNotificationPermissionPref -> {
                viewModel.onRequestNotificationPermissionClicked()
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

    override fun onResume() {
        super.onResume()
        viewModel.updateNotificationPermissionData()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == SystemPermission.NOTIFICATION.requestCode) {
            viewModel.onNotificationPermissionResult(
                isGranted = permissionHelper.isAllGranted(grantResults)
            )
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
        viewModel.lockNotificationSummary.observe(viewLifecycleOwner) { summary ->
            isLockNotificationVisiblePref.summary = summary
        }
        viewModel.isLockNotificationEnabled.observe(viewLifecycleOwner) { isEnabled ->
            isLockNotificationVisiblePref.isEnabled = isEnabled
        }
        viewModel.isEnableNotificationPermissionVisible.observe(viewLifecycleOwner) { isVisible ->
            enableNotificationPermissionPref.isVisible = isVisible
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
        viewModel.requestPermissionEvent.observe(viewLifecycleOwner) { permission ->
            requestSystemPermission(permission)
        }
    }

    private fun setProgressVisibility(isProgressVisible: Boolean) {
        for (childView in contentView.getChildViews()) {
            childView.isVisible = if (childView.id == R.id.progressBarLayout) {
                isProgressVisible
            } else {
                !isProgressVisible
            }
        }
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