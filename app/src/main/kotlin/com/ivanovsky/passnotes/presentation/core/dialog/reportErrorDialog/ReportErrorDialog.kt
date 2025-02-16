package com.ivanovsky.passnotes.presentation.core.dialog.reportErrorDialog

import android.os.Bundle
import android.view.View
import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModelProvider
import com.ivanovsky.passnotes.presentation.core.ViewModelFactory
import com.ivanovsky.passnotes.presentation.core.dialog.BaseComposeDialog
import com.ivanovsky.passnotes.presentation.core.extensions.getSerializableCompat
import com.ivanovsky.passnotes.presentation.core.extensions.openUrl
import com.ivanovsky.passnotes.presentation.core.extensions.requireArgument
import com.ivanovsky.passnotes.presentation.core.extensions.withArguments

class ReportErrorDialog : BaseComposeDialog<ReportErrorDialogViewModel>() {

    private val args: ReportErrorDialogArgs by lazy {
        arguments?.getSerializableCompat(ARGUMENTS, ReportErrorDialogArgs::class)
            ?: requireArgument(ARGUMENTS)
    }

    override val viewModel: ReportErrorDialogViewModel by lazy {
        ViewModelProvider(
            owner = this,
            factory = ViewModelFactory(args)
        )[ReportErrorDialogViewModel::class]
    }

    @Composable
    override fun RenderDialog() {
        ReportErrorDialogScreen(viewModel)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        subscribeToEvents()
    }

    private fun subscribeToEvents() {
        viewModel.openIssuesUrlEvent.observe(viewLifecycleOwner) { url ->
            openUrl(url)
        }
    }

    companion object {
        val TAG = ReportErrorDialog::class.java.simpleName

        private const val ARGUMENTS = "arguments"

        fun newInstance(args: ReportErrorDialogArgs) =
            ReportErrorDialog()
                .withArguments {
                    putSerializable(ARGUMENTS, args)
                }
    }
}