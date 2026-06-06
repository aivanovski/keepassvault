package com.ivanovsky.passnotes.presentation.core.dialog.searchOptions

import android.os.Bundle
import android.view.View
import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModelProvider
import com.ivanovsky.passnotes.presentation.core.ViewModelFactory
import com.ivanovsky.passnotes.presentation.core.dialog.BaseComposeDialog

class SearchOptionsDialog : BaseComposeDialog<SearchOptionsDialogViewModel>() {

    override val viewModel: SearchOptionsDialogViewModel by lazy {
        ViewModelProvider(
            owner = this,
            factory = ViewModelFactory()
        )[SearchOptionsDialogViewModel::class.java]
    }

    @Composable
    override fun RenderDialog() {
        SearchOptionsDialogScreen(viewModel)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.dismissEvent.observe(viewLifecycleOwner) {
            dismiss()
        }
    }

    companion object Companion {
        val TAG = SearchOptionsDialog::class.java.simpleName

        fun newInstance() = SearchOptionsDialog()
    }
}