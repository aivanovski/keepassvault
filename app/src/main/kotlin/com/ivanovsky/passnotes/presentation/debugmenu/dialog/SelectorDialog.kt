package com.ivanovsky.passnotes.presentation.debugmenu.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.databinding.DialogSelectorBinding
import com.ivanovsky.passnotes.presentation.core.ViewModelTypes
import com.ivanovsky.passnotes.presentation.core.adapter.ViewModelsAdapter
import com.ivanovsky.passnotes.presentation.core.extensions.getMandatoryArgument
import com.ivanovsky.passnotes.presentation.core.extensions.setViewModels
import com.ivanovsky.passnotes.presentation.core.extensions.withArguments
import com.ivanovsky.passnotes.presentation.debugmenu.dialog.cells.viewModel.CheckboxCellViewModel

class SelectorDialog : DialogFragment() {

    private val args: SelectorDialogArgs by lazy {
        getMandatoryArgument(ARGUMENTS)
    }

    private val viewModel: SelectorDialogViewModel by lazy {
        ViewModelProvider(
            owner = this,
            factory = SelectorDialogViewModel.Factory(
                args = args
            )
        )[SelectorDialogViewModel::class.java]
    }

    private var onItemsSelected: ((selectedIndices: List<Int>) -> Unit)? = null
    private lateinit var binding: DialogSelectorBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DialogSelectorBinding.inflate(inflater, container, false)
            .also {
                it.lifecycleOwner = viewLifecycleOwner
                it.viewModel = viewModel
            }

        binding.recyclerView.apply {
            adapter = ViewModelsAdapter(
                lifecycleOwner = viewLifecycleOwner,
                viewTypes = ViewModelTypes()
                    .add(CheckboxCellViewModel::class, R.layout.cell_switch)
            )
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        subscribeToLiveData()
    }

    private fun subscribeToLiveData() {
        viewModel.cellViewModels.observe(viewLifecycleOwner) { cellViewModels ->
            binding.recyclerView.setViewModels(cellViewModels)
        }
    }

    companion object {

        val TAG = SelectorDialog::class.java.simpleName

        private const val ARGUMENTS = "arguments"

        fun newInstance(
            args: SelectorDialogArgs,
            onItemsSelected: (selectedIndices: List<Int>) -> Unit
        ): SelectorDialog = SelectorDialog()
            .withArguments {
                putParcelable(ARGUMENTS, args)
            }
            .apply {
                this.onItemsSelected = onItemsSelected
            }
    }
}