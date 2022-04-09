package com.ivanovsky.passnotes.presentation.selectdb

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.data.entity.ConflictResolutionStrategy.RESOLVE_WITH_LOCAL_FILE
import com.ivanovsky.passnotes.data.entity.ConflictResolutionStrategy.RESOLVE_WITH_REMOTE_FILE
import com.ivanovsky.passnotes.data.entity.FileDescriptor
import com.ivanovsky.passnotes.data.entity.SyncConflictInfo
import com.ivanovsky.passnotes.databinding.SelectdbFragmentBinding
import com.ivanovsky.passnotes.domain.DateFormatProvider
import com.ivanovsky.passnotes.injection.GlobalInjector.inject
import com.ivanovsky.passnotes.presentation.core.BaseFragment
import com.ivanovsky.passnotes.presentation.core.dialog.ConfirmationDialog
import com.ivanovsky.passnotes.presentation.core.dialog.ThreeButtonDialog
import com.ivanovsky.passnotes.presentation.core.extensions.getMandatoryArgument
import com.ivanovsky.passnotes.presentation.core.extensions.setupActionBar
import com.ivanovsky.passnotes.presentation.core.extensions.updateMenuItemVisibility
import com.ivanovsky.passnotes.presentation.core.extensions.withArguments
import com.ivanovsky.passnotes.presentation.selectdb.SelectDatabaseViewModel.SelectDatabaseMenuItem
import java.util.Date
import java.util.UUID
import org.apache.commons.lang3.StringUtils
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf

class SelectDatabaseFragment : BaseFragment() {

    private val args: SelectDatabaseArgs by lazy { getMandatoryArgument(ARGUMENTS)}
    private val dateFormatProvider: DateFormatProvider by inject()
    private var menu: Menu? = null

    private val viewModel: SelectDatabaseViewModel by viewModel {
        parametersOf(args)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        setHasOptionsMenu(true)
        setupActionBar {
            title = getString(R.string.select_database)
            setHomeAsUpIndicator(null)
            setDisplayHomeAsUpEnabled(true)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        this.menu = menu

        inflater.inflate(R.menu.select_database, menu)

        viewModel.visibleMenuItems.value?.let {
            updateMenuItemVisibility(
                menu = menu,
                visibleItems = it,
                allScreenItems = SelectDatabaseMenuItem.values().toList()
            )
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return SelectdbFragmentBinding.inflate(inflater, container, false).also {
            it.lifecycleOwner = viewLifecycleOwner
            it.viewModel = viewModel
        }
            .root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        subscribeToLiveData()

        viewModel.loadData()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                viewModel.navigateBack()
                true
            }
            R.id.menu_refresh -> {
                viewModel.onRefreshButtonClicked()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun subscribeToLiveData() {
        viewModel.visibleMenuItems.observe(viewLifecycleOwner) { visibleItems ->
            menu?.let { menu ->
                updateMenuItemVisibility(
                    menu = menu,
                    visibleItems = visibleItems,
                    allScreenItems = SelectDatabaseMenuItem.values().toList()
                )
            }
        }
        viewModel.showRemoveConfirmationDialogEvent.observe(viewLifecycleOwner) { (uid, file) ->
            showRemoveConfirmationDialog(uid, file)
        }
        viewModel.showResolveConflictDialog.observe(viewLifecycleOwner) { (uid, info) ->
            showResolveConflictDialog(uid, info)
        }
    }

    private fun showRemoveConfirmationDialog(uid: UUID, file: FileDescriptor) {
        val message = getString(
            R.string.remove_confirmation_message,
            getString(R.string.database_lower),
            file.name
        )

        val dialog = ConfirmationDialog.newInstance(
            message = message,
            positiveButtonText = getString(R.string.yes),
            negativeButtonText = getString(R.string.no)
        ).apply {
            onConfirmationLister = {
                viewModel.onRemoveConfirmed(uid)
            }
        }
        dialog.show(childFragmentManager, ConfirmationDialog.TAG)
    }

    private fun showResolveConflictDialog(uid: UUID, info: SyncConflictInfo) {
        val localDate = info.localFile.modified?.let { Date(it) }
        val remoteDate = info.remoteFile.modified?.let { Date(it) }
        val dateFormat = dateFormatProvider.getLongDateFormat()
        val timeFormat = dateFormatProvider.getTimeFormat()

        val localDateText = localDate
            ?.let { dateFormat.format(it) + " " + timeFormat.format(it) }
            ?: StringUtils.EMPTY

        val remoteDateText = remoteDate
            ?.let { dateFormat.format(it) + " " + timeFormat.format(it) }
            ?: StringUtils.EMPTY

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
                    viewModel.onResolveConflictConfirmed(uid, RESOLVE_WITH_REMOTE_FILE)
                }
                onNegativeClicked = {
                    viewModel.onResolveConflictConfirmed(uid, RESOLVE_WITH_LOCAL_FILE)
                }
            }

        dialog.show(childFragmentManager, ThreeButtonDialog.TAG)
    }

    companion object {

        private const val ARGUMENTS = "arguments"

        fun newInstance(args: SelectDatabaseArgs) = SelectDatabaseFragment().withArguments {
            putParcelable(ARGUMENTS, args)
        }
    }
}