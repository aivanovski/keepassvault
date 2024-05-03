package com.ivanovsky.passnotes.presentation.unlock

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.data.entity.FileDescriptor
import com.ivanovsky.passnotes.data.entity.UsedFile
import com.ivanovsky.passnotes.databinding.UnlockFragmentBinding
import com.ivanovsky.passnotes.domain.biometric.BiometricAuthenticator
import com.ivanovsky.passnotes.injection.GlobalInjector.inject
import com.ivanovsky.passnotes.presentation.core.BaseFragment
import com.ivanovsky.passnotes.presentation.core.adapter.ViewModelsAdapter
import com.ivanovsky.passnotes.presentation.core.dialog.resolveConflict.ResolveConflictDialog
import com.ivanovsky.passnotes.presentation.core.dialog.resolveConflict.ResolveConflictDialogArgs
import com.ivanovsky.passnotes.presentation.core.extensions.finishActivity
import com.ivanovsky.passnotes.presentation.core.extensions.getMandatoryArgument
import com.ivanovsky.passnotes.presentation.core.extensions.sendAutofillResult
import com.ivanovsky.passnotes.presentation.core.extensions.setViewModels
import com.ivanovsky.passnotes.presentation.core.extensions.setupActionBar
import com.ivanovsky.passnotes.presentation.core.extensions.showMessageDialog
import com.ivanovsky.passnotes.presentation.core.extensions.showSnackbarMessage
import com.ivanovsky.passnotes.presentation.core.extensions.withArguments
import com.ivanovsky.passnotes.presentation.groups.dialog.ChooseOptionDialog
import com.ivanovsky.passnotes.presentation.unlock.UnlockViewModel.UnlockOption

class UnlockFragment : BaseFragment() {

    private lateinit var binding: UnlockFragmentBinding
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
            setHomeAsUpIndicator(R.drawable.ic_menu_24dp)
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

        binding.recyclerView.adapter = ViewModelsAdapter(
            lifecycleOwner = viewLifecycleOwner,
            viewTypes = viewModel.fileCellTypes
        )

        return binding.root
    }

    override fun onStart() {
        super.onStart()
        viewModel.onScreenStart()
        navigationViewModel.setNavigationEnabled(true)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        subscribeToLiveData()
        subscribeToLiveEvents()

        viewModel.loadData(
            isResetSelection = false,
            isShowKeyboard = true
        )
    }

    private fun subscribeToLiveData() {
        viewModel.fileCellViewModels.observe(viewLifecycleOwner) { viewModels ->
            binding.recyclerView.setViewModels(viewModels)
        }
    }

    private fun subscribeToLiveEvents() {
        viewModel.isKeyboardVisibleEvent.observe(viewLifecycleOwner) { isVisible ->
            if (isVisible) {
                binding.unlockView.requestSoftInput()
            } else {
                binding.unlockView.hideSoftInput()
            }
        }
        viewModel.showSnackbarMessage.observe(viewLifecycleOwner) { message ->
            showSnackbarMessage(message)
        }
        viewModel.showMessageDialog.observe(viewLifecycleOwner) { message ->
            showMessageDialog(message)
        }
        viewModel.showResolveConflictDialog.observe(viewLifecycleOwner) { file ->
            showResolveConflictDialog(file)
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
        viewModel.showAddMenuDialog.observe(viewLifecycleOwner) {
            showAddMenuDialog()
        }
        viewModel.showUnlockOptionsDialog.observe(viewLifecycleOwner) { options ->
            showUnlockOptionsDialog(options)
        }
    }

    private fun showResolveConflictDialog(file: FileDescriptor) {
        val dialog = ResolveConflictDialog.newInstance(
            args = ResolveConflictDialogArgs(file)
        )
        dialog.show(childFragmentManager, ResolveConflictDialog.TAG)
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

    private fun showAddMenuDialog() {
        val entries = listOf(
            resources.getString(R.string.new_file),
            resources.getString(R.string.open_file)
        )

        val dialog = ChooseOptionDialog.newInstance(null, entries)
            .apply {
                onItemClickListener = { itemIdx ->
                    when (itemIdx) {
                        0 -> viewModel.onNewFileClicked()
                        1 -> viewModel.onOpenFileClicked()
                    }
                }
            }
        dialog.show(childFragmentManager, ChooseOptionDialog.TAG)
    }

    private fun showUnlockOptionsDialog(options: List<UnlockOption>) {
        val entries = options.map { option -> getString(option.nameResId) }

        val dialog = ChooseOptionDialog.newInstance(getString(R.string.unlock_with), entries)
            .apply {
                onItemClickListener = { itemIndex ->
                    viewModel.onUnlockOptionSelected(options[itemIndex])
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