package com.ivanovsky.passnotes.presentation.core.dialog.resolveConflict

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.databinding.DialogResolveConflictBinding
import com.ivanovsky.passnotes.extensions.cloneInContext
import com.ivanovsky.passnotes.presentation.core.extensions.getMandatoryArgument
import com.ivanovsky.passnotes.presentation.core.extensions.withArguments

class ResolveConflictDialog : DialogFragment() {

    private val viewModel: ResolveConflictDialogViewModel by lazy {
        ViewModelProvider(
            this,
            ResolveConflictDialogViewModel.Factory(
                args = getMandatoryArgument(ARGUMENTS)
            )
        )
            .get(ResolveConflictDialogViewModel::class.java)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return DialogResolveConflictBinding.inflate(
            inflater.cloneInContext(R.style.AppDialogTheme)
        )
            .also {
                it.lifecycleOwner = viewLifecycleOwner
                it.viewModel = viewModel
            }
            .root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        subscribeToEvents()

        viewModel.start()
    }

    private fun subscribeToEvents() {
        viewModel.dismissEvent.observe(viewLifecycleOwner) {
            dismiss()
        }
    }

    companion object {

        val TAG: String = ResolveConflictDialog::class.java.simpleName
        private const val ARGUMENTS = "arguments"

        fun newInstance(
            args: ResolveConflictDialogArgs
        ): ResolveConflictDialog {
            return ResolveConflictDialog()
                .withArguments {
                    putParcelable(ARGUMENTS, args)
                }
        }
    }
}