package com.ivanovsky.passnotes.presentation.settings.database.changePassword

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.databinding.DialogChangePasswordBinding
import com.ivanovsky.passnotes.extensions.cloneInContext
import com.ivanovsky.passnotes.presentation.core.dialog.BaseDialogFragment
import org.koin.androidx.viewmodel.ext.android.viewModel

class ChangePasswordDialog : BaseDialogFragment() {

    private val viewModel: ChangePasswordDialogViewModel by viewModel()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return DialogChangePasswordBinding.inflate(inflater.cloneInContext(R.style.AppDialogTheme))
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