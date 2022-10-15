package com.ivanovsky.passnotes.presentation.unlock

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.observe
import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.data.crypto.biometric.BiometricDecoderImpl
import com.ivanovsky.passnotes.data.crypto.biometric.BiometricEncoderImpl
import com.ivanovsky.passnotes.data.entity.ConflictResolutionStrategy.RESOLVE_WITH_LOCAL_FILE
import com.ivanovsky.passnotes.data.entity.ConflictResolutionStrategy.RESOLVE_WITH_REMOTE_FILE
import com.ivanovsky.passnotes.data.entity.SyncConflictInfo
import com.ivanovsky.passnotes.databinding.UnlockFragmentBinding
import com.ivanovsky.passnotes.domain.DateFormatProvider
import com.ivanovsky.passnotes.injection.GlobalInjector.inject
import com.ivanovsky.passnotes.presentation.core.BaseFragment
import com.ivanovsky.passnotes.presentation.core.BiometricPromptHelper
import com.ivanovsky.passnotes.presentation.core.dialog.ThreeButtonDialog
import com.ivanovsky.passnotes.presentation.core.extensions.finishActivity
import com.ivanovsky.passnotes.presentation.core.extensions.getMandatoryArgument
import com.ivanovsky.passnotes.presentation.core.extensions.hideKeyboard
import com.ivanovsky.passnotes.presentation.core.extensions.sendAutofillResult
import com.ivanovsky.passnotes.presentation.core.extensions.setupActionBar
import com.ivanovsky.passnotes.presentation.core.extensions.showSnackbarMessage
import com.ivanovsky.passnotes.presentation.core.extensions.updateMenuItemVisibility
import com.ivanovsky.passnotes.presentation.core.extensions.withArguments
import com.ivanovsky.passnotes.presentation.main.navigation.NavigationMenuViewModel
import com.ivanovsky.passnotes.presentation.unlock.UnlockViewModel.UnlockMenuItem
import org.apache.commons.lang3.StringUtils.EMPTY
import java.util.Date

class UnlockFragment : BaseFragment() {

    private lateinit var binding: UnlockFragmentBinding
    private var menu: Menu? = null
    private val dateFormatProvider: DateFormatProvider by inject()
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        this.menu = menu

        inflater.inflate(R.menu.unlock, menu)

        viewModel.visibleMenuItems.value?.let {
            updateMenuItemVisibility(
                menu = menu,
                visibleItems = it,
                allScreenItems = UnlockMenuItem.values().toList()
            )
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
        subscribeToLiveData()
        subscribeToLiveEvents()

        viewModel.loadData(resetSelection = false)
    }

    private fun subscribeToLiveData() {
        viewModel.visibleMenuItems.observe(viewLifecycleOwner) { visibleItems ->
            menu?.let { menu ->
                updateMenuItemVisibility(
                    menu = menu,
                    visibleItems = visibleItems,
                    allScreenItems = UnlockMenuItem.values().toList()
                )
            }
        }
    }

    private fun subscribeToLiveEvents() {
        viewModel.hideKeyboardEvent.observe(viewLifecycleOwner) {
            hideKeyboard()
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
        viewModel.showBiometricSetupDialog.observe(viewLifecycleOwner) { cipher ->
            BiometricPromptHelper.authenticateForSetup(
                activity = requireActivity(),
                cipher = cipher,
                onSuccess = { result ->
                    result.cryptoObject?.cipher?.let {
                        viewModel.onBiometricSetupSuccess(BiometricEncoderImpl(it))
                    }
                }
            )
        }
        viewModel.showBiometricUnlockDialog.observe(viewLifecycleOwner) { cipher ->
            BiometricPromptHelper.authenticateForUnlock(
                activity = requireActivity(),
                cipher = cipher,
                onSuccess = { result ->
                    result.cryptoObject?.cipher?.let {
                        viewModel.onBiometricUnlockSuccess(BiometricDecoderImpl(it))
                    }
                }
            )
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

    companion object {

        private const val ARGUMENTS = "arguments"

        fun newInstance(args: UnlockScreenArgs): UnlockFragment = UnlockFragment()
            .withArguments {
                putParcelable(ARGUMENTS, args)
            }
    }
}