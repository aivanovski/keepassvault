package com.ivanovsky.passnotes.presentation.groups.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.ivanovsky.passnotes.databinding.DialogSortAndViewBinding
import org.koin.androidx.viewmodel.ext.android.viewModel

class SortAndViewDialog : DialogFragment() {

    private val viewModel: SortAndViewDialogViewModel by viewModel()

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

        val TAG = SortAndViewDialog::class.java.simpleName

        fun newInstance() = SortAndViewDialog()
    }
}