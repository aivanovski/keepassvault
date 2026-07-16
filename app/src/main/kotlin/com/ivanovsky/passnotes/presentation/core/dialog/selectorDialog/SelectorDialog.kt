package com.ivanovsky.passnotes.presentation.core.dialog.selectorDialog

import android.os.Bundle
import android.view.View
import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModelProvider
import com.ivanovsky.passnotes.presentation.core.dialog.BaseComposeDialog
import com.ivanovsky.passnotes.presentation.core.extensions.getMandatoryArgument
import com.ivanovsky.passnotes.presentation.core.extensions.withArguments

class SelectorDialog : BaseComposeDialog<SelectorDialogViewModel>() {

    private val args: SelectorDialogArgs by lazy {
        getMandatoryArgument(ARGUMENTS)
    }

    override val viewModel: SelectorDialogViewModel by lazy {
        ViewModelProvider(
            owner = this,
            factory = SelectorDialogViewModel.Factory(args)
        )[SelectorDialogViewModel::class.java]
    }

    private var onItemSelected: ((index: Int) -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (savedInstanceState != null) {
            dismiss()
        }
    }

    @Composable
    override fun RenderDialog() {
        SelectorDialogScreen(viewModel)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.selectItemEvent.observe(viewLifecycleOwner) { index ->
            onItemSelected?.invoke(index)
            dismiss()
        }
        viewModel.dismissEvent.observe(viewLifecycleOwner) {
            dismiss()
        }
    }

    companion object {

        val TAG = SelectorDialog::class.java.simpleName

        private const val ARGUMENTS = "arguments"

        fun newInstance(
            args: SelectorDialogArgs,
            onItemSelected: (index: Int) -> Unit
        ): SelectorDialog =
            SelectorDialog()
                .withArguments {
                    putParcelable(ARGUMENTS, args)
                }
                .apply {
                    this.onItemSelected = onItemSelected
                }
    }
}