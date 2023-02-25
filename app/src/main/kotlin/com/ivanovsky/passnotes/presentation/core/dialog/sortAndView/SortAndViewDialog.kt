package com.ivanovsky.passnotes.presentation.core.dialog.sortAndView

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import com.ivanovsky.passnotes.databinding.DialogSortAndViewBinding
import com.ivanovsky.passnotes.presentation.core.extensions.getMandatoryArgument
import com.ivanovsky.passnotes.presentation.core.extensions.withArguments

class SortAndViewDialog : DialogFragment() {

    private val viewModel: SortAndViewDialogViewModel by lazy {
        ViewModelProvider(
            this,
            SortAndViewDialogViewModel.Factory(
                args = getMandatoryArgument(ARGUMENTS)
            )
        )
            .get(SortAndViewDialogViewModel::class.java)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return DialogSortAndViewBinding.inflate(inflater)
            .also {
                it.lifecycleOwner = this
                it.viewModel = viewModel
            }
            .root
    }

    companion object {

        val TAG: String = SortAndViewDialog::class.java.simpleName
        private const val ARGUMENTS = "arguments"

        fun newInstance(args: SortAndViewDialogArgs): SortAndViewDialog {
            return SortAndViewDialog()
                .withArguments {
                    putParcelable(ARGUMENTS, args)
                }
        }
    }
}