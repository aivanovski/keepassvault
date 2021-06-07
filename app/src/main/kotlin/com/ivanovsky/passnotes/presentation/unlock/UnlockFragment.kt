package com.ivanovsky.passnotes.presentation.unlock

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.observe
import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.data.entity.ConflictResolutionStrategy.RESOLVE_WITH_LOCAL_FILE
import com.ivanovsky.passnotes.data.entity.ConflictResolutionStrategy.RESOLVE_WITH_REMOTE_FILE
import com.ivanovsky.passnotes.data.entity.SyncConflictInfo
import com.ivanovsky.passnotes.databinding.UnlockFragmentBinding
import com.ivanovsky.passnotes.domain.DateFormatProvider
import com.ivanovsky.passnotes.injection.GlobalInjector.inject
import com.ivanovsky.passnotes.presentation.core.dialog.ThreeButtonDialog
import com.ivanovsky.passnotes.presentation.core.extensions.hideKeyboard
import com.ivanovsky.passnotes.presentation.core.extensions.setupActionBar
import com.ivanovsky.passnotes.presentation.core.extensions.showSnackbarMessage
import java.util.Date
import org.apache.commons.lang3.StringUtils.EMPTY

class UnlockFragment : Fragment() {

    private lateinit var binding: UnlockFragmentBinding
    private val dateFormatProvider: DateFormatProvider by inject()

    private val viewModel: UnlockViewModel by lazy {
        ViewModelProvider(requireActivity(), UnlockViewModel.FACTORY)
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

    override fun onStart() {
        super.onStart()
        viewModel.onScreenStart()
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

        // TODO: move fab initialization to @BindingAdapter
        val fabItems = resources.getStringArray(R.array.unlock_fab_actions).toList()
        binding.fab.inflate(fabItems)
        binding.fab.onItemClickListener = { position -> viewModel.onFabActionClicked(position) }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        subscribeToLiveEvents()

        viewModel.loadData(resetSelection = false)
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
        fun newInstance() = UnlockFragment()
    }
}