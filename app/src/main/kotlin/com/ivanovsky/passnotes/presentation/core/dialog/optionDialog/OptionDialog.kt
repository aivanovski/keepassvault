package com.ivanovsky.passnotes.presentation.core.dialog.optionDialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.databinding.CoreBaseCellDialogBinding
import com.ivanovsky.passnotes.extensions.cloneInContext
import com.ivanovsky.passnotes.presentation.core.ViewModelTypes
import com.ivanovsky.passnotes.presentation.core.adapter.ViewModelsAdapter
import com.ivanovsky.passnotes.presentation.core.extensions.getMandatoryArgument
import com.ivanovsky.passnotes.presentation.core.extensions.setViewModels
import com.ivanovsky.passnotes.presentation.core.extensions.withArguments
import com.ivanovsky.passnotes.presentation.core.viewmodel.DividerCellViewModel
import com.ivanovsky.passnotes.presentation.core.viewmodel.OneLineTextCellViewModel
import com.ivanovsky.passnotes.presentation.core.viewmodel.TwoLineTextCellViewModel

class OptionDialog : DialogFragment() {

    private val args: OptionDialogArgs by lazy {
        getMandatoryArgument(ARGUMENTS)
    }

    private val viewModel: OptionDialogViewModel by lazy {
        ViewModelProvider(
            owner = this,
            factory = OptionDialogViewModel.Factory(
                args = args
            )
        )[OptionDialogViewModel::class.java]
    }

    private lateinit var binding: CoreBaseCellDialogBinding
    private var onItemClick: ((index: Int) -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (savedInstanceState != null) {
            dismiss()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val themedInflater = inflater.cloneInContext(R.style.AppDialogTheme)
        val binding = CoreBaseCellDialogBinding.inflate(themedInflater, container, false)
            .also {
                binding = it
            }

        binding.recyclerView.adapter = ViewModelsAdapter(
            lifecycleOwner = viewLifecycleOwner,
            viewTypes = ViewModelTypes()
                .add(OneLineTextCellViewModel::class, R.layout.cell_option_one_line)
                .add(TwoLineTextCellViewModel::class, R.layout.cell_option_two_line)
                .add(DividerCellViewModel::class, R.layout.cell_divider)
        )

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        subscribeToLiveData()
        subscribeToEvents()
    }

    private fun subscribeToLiveData() {
        viewModel.cellViewModels.observe(viewLifecycleOwner) { viewModels ->
            binding.recyclerView.setViewModels(viewModels)
        }
    }

    private fun subscribeToEvents() {
        viewModel.selectItemEvent.observe(viewLifecycleOwner) { index ->
            onItemClick?.invoke(index)
            dismiss()
        }
    }

    companion object {

        val TAG = OptionDialog::class.java.simpleName

        private const val ARGUMENTS = "arguments"

        fun newInstance(
            args: OptionDialogArgs,
            onItemClick: (index: Int) -> Unit
        ): OptionDialog =
            OptionDialog()
                .withArguments {
                    putParcelable(ARGUMENTS, args)
                }
                .apply {
                    this.onItemClick = onItemClick
                }
    }
}