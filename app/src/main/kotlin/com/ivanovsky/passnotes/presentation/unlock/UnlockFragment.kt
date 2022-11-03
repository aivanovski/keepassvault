package com.ivanovsky.passnotes.presentation.unlock

import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.observe
import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.data.entity.ConflictResolutionStrategy.RESOLVE_WITH_LOCAL_FILE
import com.ivanovsky.passnotes.data.entity.ConflictResolutionStrategy.RESOLVE_WITH_REMOTE_FILE
import com.ivanovsky.passnotes.data.entity.SyncConflictInfo
import com.ivanovsky.passnotes.data.entity.UsedFile
import com.ivanovsky.passnotes.databinding.UnlockFragmentBinding
import com.ivanovsky.passnotes.domain.DateFormatProvider
import com.ivanovsky.passnotes.domain.biometric.BiometricAuthenticator
import com.ivanovsky.passnotes.injection.GlobalInjector.inject
import com.ivanovsky.passnotes.presentation.core.BaseFragment
import com.ivanovsky.passnotes.presentation.core.dialog.ThreeButtonDialog
import com.ivanovsky.passnotes.presentation.core.extensions.finishActivity
import com.ivanovsky.passnotes.presentation.core.extensions.getMandatoryArgument
import com.ivanovsky.passnotes.presentation.core.extensions.hideKeyboard
import com.ivanovsky.passnotes.presentation.core.extensions.sendAutofillResult
import com.ivanovsky.passnotes.presentation.core.extensions.setupActionBar
import com.ivanovsky.passnotes.presentation.core.extensions.showKeyboard
import com.ivanovsky.passnotes.presentation.core.extensions.showSnackbarMessage
import com.ivanovsky.passnotes.presentation.core.extensions.withArguments
import com.ivanovsky.passnotes.presentation.groups.dialog.ChooseOptionDialog
import com.ivanovsky.passnotes.presentation.main.navigation.NavigationMenuViewModel
import org.apache.commons.lang3.StringUtils.EMPTY
import java.util.Date

class UnlockFragment : BaseFragment() {

    private lateinit var binding: UnlockFragmentBinding
    private val dateFormatProvider: DateFormatProvider by inject()
    private val biometricAuthenticator: BiometricAuthenticator by inject()
    private val viewModel: UnlockViewModel by lazy {
        ViewModelProvider(
            this,
            UnlockViewModel.Factory(
                args = getMandatoryArgument(ARGUMENTS)
            )
        )
            .get(UnlockViewModel::class.java)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        setupActionBar {
            title = getString(R.string.app_name)
            setDisplayHomeAsUpEnabled(true)
            setHomeAsUpIndicator(R.drawable.ic_menu_white_24dp)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = UnlockFragmentBinding.inflate(inflater, container, false)
            .also {
                it.lifecycleOwner = viewLifecycleOwner
                it.viewModel = viewModel
            }

        return binding.root
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_refresh -> {
                viewModel.onRefreshButtonClicked()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onStart() {
        super.onStart()
        viewModel.onScreenStart()
        navigationViewModel.setVisibleItems(
            NavigationMenuViewModel.createNavigationItemsForBasicScreens()
        )
        navigationViewModel.setNavigationEnabled(true)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        subscribeToLiveEvents()

        viewModel.loadData(
            isResetSelection = false,
            isShowKeyboard = true
        )
    }

    private fun subscribeToLiveEvents() {
        viewModel.isKeyboardVisibleEvent.observe(viewLifecycleOwner) { isVisible ->
            if (isVisible) {
                binding.password.getEditText().requestFocus()
                showKeyboard(binding.password.getEditText())
            } else {
                hideKeyboard()
            }
        }
        viewModel.showSnackbarMessage.observe(viewLifecycleOwner) { message ->
            showSnackbarMessage(message)
        }
        viewModel.showResolveConflictDialog.observe(viewLifecycleOwner) { info ->
            showResolveConflictDialog(info)
        }
        viewModel.sendAutofillResponseEvent.observe(viewLifecycleOwner) { (note, structure) ->
            sendAutofillResult(note, structure)
            finishActivity()
        }
        viewModel.showBiometricUnlockDialog.observe(viewLifecycleOwner) { cipher ->
            biometricAuthenticator.authenticateForUnlock(
                activity = requireActivity(),
                cipher = cipher,
                onSuccess = { decoder -> viewModel.onBiometricUnlockSuccess(decoder) }
            )
        }
        viewModel.showFileActionsDialog.observe(viewLifecycleOwner) { file ->
            showFileActionsDialog(file)
        }
    }

    private fun showResolveConflictDialog(info: SyncConflictInfo) {
        val localDate = info.localFile.modified?.let { Date(it) }
        val remoteDate = info.remoteFile.modified?.let { Date(it) }
        val dateFormat = dateFormatProvider.getLongDateFormat()
        val timeFormat = dateFormatProvider.getTimeFormat()

        val localDateText = localDate
            ?.let { dateFormat.format(it) + " " + timeFormat.format(it) }
            ?: EMPTY

        val remoteDateText = remoteDate
            ?.let { dateFormat.format(it) + " " + timeFormat.format(it) }
            ?: EMPTY

        val message = getString(
            R.string.resolve_conflict_dialog_message,
            localDateText,
            remoteDateText
        )

        val dialog = ThreeButtonDialog.newInstance(
            message = message,
            positiveButtonText = getString(R.string.remote_database),
            negativeButtonText = getString(R.string.local_database),
            neutralButtonText = getString(R.string.cancel)
        )
            .apply {
                onPositiveClicked = {
                    viewModel.onResolveConflictConfirmed(RESOLVE_WITH_REMOTE_FILE)
                }
                onNegativeClicked = {
                    viewModel.onResolveConflictConfirmed(RESOLVE_WITH_LOCAL_FILE)
                }
            }

        dialog.show(childFragmentManager, ThreeButtonDialog.TAG)
    }

    private fun showFileActionsDialog(file: UsedFile) {
        val entries = listOf(resources.getString(R.string.remove))

        val dialog = ChooseOptionDialog.newInstance(null, entries.toList())
        dialog.onItemClickListener = { itemIdx ->
            when (itemIdx) {
                0 -> viewModel.onRemoveFileClicked(file)
            }
        }
        dialog.show(childFragmentManager, ChooseOptionDialog.TAG)
    }

    companion object {

        private const val ARGUMENTS = "arguments"

        fun newInstance(args: UnlockScreenArgs): UnlockFragment = UnlockFragment()
            .withArguments {
                putParcelable(ARGUMENTS, args)
            }
    }
}