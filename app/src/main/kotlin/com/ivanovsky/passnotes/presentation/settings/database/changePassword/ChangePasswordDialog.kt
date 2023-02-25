package com.ivanovsky.passnotes.presentation.settings.database.changePassword

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.ivanovsky.passnotes.databinding.DialogChangePasswordBinding
import org.koin.androidx.viewmodel.ext.android.viewModel

class ChangePasswordDialog : DialogFragment() {

    private val viewModel: ChangePasswordDialogViewModel by viewModel()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return DialogChangePasswordBinding.inflate(inflater)
            .also {
                it.lifecycleOwner = this
                it.viewModel = viewModel
            }
            .root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        subscribeToLiveData()
    }

    private fun subscribeToLiveData() {
        viewModel.finishScreenEvent.observe(viewLifecycleOwner) {
            dismiss()
        }
    }

    companion object {

        val TAG = ChangePasswordDialog::class.java.simpleName

        fun newInstance() = ChangePasswordDialog()
    }
}