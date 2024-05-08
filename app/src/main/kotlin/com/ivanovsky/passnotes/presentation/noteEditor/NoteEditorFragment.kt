package com.ivanovsky.passnotes.presentation.noteEditor

import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.activity.addCallback
import com.github.terrakok.cicerone.Router
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.databinding.NoteEditorFragmentBinding
import com.ivanovsky.passnotes.domain.DateFormatProvider
import com.ivanovsky.passnotes.domain.entity.DateData
import com.ivanovsky.passnotes.domain.entity.TimeData
import com.ivanovsky.passnotes.injection.GlobalInjector.inject
import com.ivanovsky.passnotes.presentation.ApplicationLaunchMode
import com.ivanovsky.passnotes.presentation.Screens
import com.ivanovsky.passnotes.presentation.core.DatabaseInteractionWatcher
import com.ivanovsky.passnotes.presentation.core.FragmentWithDoneButton
import com.ivanovsky.passnotes.presentation.core.adapter.ViewModelsAdapter
import com.ivanovsky.passnotes.presentation.core.dialog.ConfirmationDialog
import com.ivanovsky.passnotes.presentation.core.extensions.getMandatoryArgument
import com.ivanovsky.passnotes.presentation.core.extensions.hideKeyboard
import com.ivanovsky.passnotes.presentation.core.extensions.setViewModels
import com.ivanovsky.passnotes.presentation.core.extensions.setupActionBar
import com.ivanovsky.passnotes.presentation.core.extensions.showToastMessage
import com.ivanovsky.passnotes.presentation.core.extensions.withArguments
import com.ivanovsky.passnotes.presentation.groups.dialog.ChooseOptionDialog
import com.ivanovsky.passnotes.presentation.noteEditor.NoteEditorViewModel.CellType
import com.ivanovsky.passnotes.presentation.unlock.UnlockScreenArgs
import com.ivanovsky.passnotes.util.TimeUtils.toDate
import com.ivanovsky.passnotes.util.TimeUtils.toTimestamp
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf

class NoteEditorFragment : FragmentWithDoneButton() {

    private val args: NoteEditorArgs by lazy {
        getMandatoryArgument(ARGUMENTS)
    }
    private val viewModel: NoteEditorViewModel by viewModel(
        parameters = { parametersOf(args) }
    )
    private val router: Router by inject()
    private val dateFormatProvider: DateFormatProvider by inject()
    private var backCallback: OnBackPressedCallback? = null
    private lateinit var binding: NoteEditorFragmentBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = NoteEditorFragmentBinding.inflate(inflater, container, false)
            .also {
                it.lifecycleOwner = viewLifecycleOwner
                it.viewModel = viewModel
            }

        binding.recyclerView.adapter = ViewModelsAdapter(
            lifecycleOwner = viewLifecycleOwner,
            viewTypes = viewModel.viewTypes
        )

        return binding.root
    }

    override fun onDoneMenuClicked() {
        viewModel.onDoneMenuClicked()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                viewModel.onBackClicked()
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onStart() {
        super.onStart()
        backCallback = requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            viewModel.onBackClicked()
        }
    }

    override fun onStop() {
        super.onStop()
        backCallback?.remove()
        backCallback = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewLifecycleOwner.lifecycle.addObserver(DatabaseInteractionWatcher(this))

        setupActionBar {
            args.title?.let {
                title = it
            }
            setHomeAsUpIndicator(null)
            setDisplayHomeAsUpEnabled(true)
        }

        subscribeToLiveData()
        subscribeToEvents()

        viewModel.start()
    }

    private fun subscribeToLiveData() {
        viewModel.cellViewModels.observe(viewLifecycleOwner) { viewModels ->
            binding.recyclerView.setViewModels(viewModels)
        }
        viewModel.isDoneButtonVisible.observe(viewLifecycleOwner) { isVisible ->
            setDoneButtonVisibility(isVisible)
        }
    }

    private fun subscribeToEvents() {
        viewModel.showDiscardDialogEvent.observe(viewLifecycleOwner) { message ->
            showDiscardDialog(message)
        }
        viewModel.showAddDialogEvent.observe(viewLifecycleOwner) { items ->
            showAddItemDialog(items)
        }
        viewModel.showToastEvent.observe(viewLifecycleOwner) { message ->
            showToastMessage(message)
        }
        viewModel.hideKeyboardEvent.observe(viewLifecycleOwner) {
            hideKeyboard()
        }
        viewModel.lockScreenEvent.observe(viewLifecycleOwner) {
            router.backTo(
                Screens.UnlockScreen(
                    args = UnlockScreenArgs(ApplicationLaunchMode.NORMAL)
                )
            )
        }
        viewModel.showDatePickerEvent.observe(viewLifecycleOwner) { date ->
            showDatePicker(date)
        }
        viewModel.showTimePickerEvent.observe(viewLifecycleOwner) { time ->
            showTimePicker(time)
        }
    }

    private fun showDiscardDialog(message: String) {
        val dialog = ConfirmationDialog.newInstance(
            message,
            getString(R.string.discard),
            getString(R.string.cancel)
        )

        dialog.onConfirmed = {
            viewModel.onDiscardConfirmed()
        }

        dialog.show(childFragmentManager, ConfirmationDialog.TAG)
    }

    private fun showAddItemDialog(items: List<Pair<CellType, String>>) {
        val entries = items.map { it.second }

        val dialog = ChooseOptionDialog.newInstance(
            getString(
                R.string.text_with_colon,
                getString(R.string.select_item_to_add)
            ),
            entries
        )
        dialog.onItemClickListener = { itemIdx ->
            viewModel.onAddDialogItemSelected(items[itemIdx].first)
        }
        dialog.show(childFragmentManager, ChooseOptionDialog.TAG)
    }

    private fun showDatePicker(date: DateData) {
        val picker = MaterialDatePicker.Builder.datePicker()
            .setSelection(date.toTimestamp().timeInMillis)
            .build()

        picker.addOnPositiveButtonClickListener { timestamp ->
            viewModel.onExpirationDateChanged(timestamp.toTimestamp().toDate())
        }

        picker.show(childFragmentManager, MaterialDatePicker::class.simpleName)
    }

    private fun showTimePicker(time: TimeData) {
        val timeFormat = if (dateFormatProvider.is24HourFormat()) {
            TimeFormat.CLOCK_24H
        } else {
            TimeFormat.CLOCK_12H
        }

        val picker = MaterialTimePicker.Builder()
            .setHour(time.hour)
            .setMinute(time.minute)
            .setTimeFormat(timeFormat)
            .setInputMode(MaterialTimePicker.INPUT_MODE_CLOCK)
            .build()

        picker.addOnPositiveButtonClickListener {
            val newTime = TimeData(
                hour = picker.hour,
                minute = picker.minute,
                second = 0
            )

            viewModel.onExpirationTimeChanged(newTime)
        }

        picker.show(childFragmentManager, MaterialTimePicker::class.simpleName)
    }

    companion object {

        private const val ARGUMENTS = "arguments"

        fun newInstance(args: NoteEditorArgs) = NoteEditorFragment().withArguments {
            putParcelable(ARGUMENTS, args)
        }
    }
}