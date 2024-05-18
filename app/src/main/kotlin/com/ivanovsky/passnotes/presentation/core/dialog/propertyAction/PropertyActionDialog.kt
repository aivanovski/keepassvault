package com.ivanovsky.passnotes.presentation.core.dialog.propertyAction

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.view.View
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.presentation.core.extensions.getMandatoryArgument
import com.ivanovsky.passnotes.presentation.core.extensions.openUrl
import com.ivanovsky.passnotes.presentation.core.extensions.withArguments

class PropertyActionDialog : DialogFragment() {

    lateinit var onActionClicked: (action: PropertyAction) -> Unit

    private val args: PropertyActionDialogArgs by lazy {
        getMandatoryArgument(ARGUMENTS)
    }

    private val viewModel: PropertyActionDialogViewModel by lazy {
        ViewModelProvider(
            this,
            PropertyActionDialogViewModel.Factory(args)
        )[PropertyActionDialogViewModel::class.java]
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (savedInstanceState != null) {
            dismiss()
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val actions = viewModel.actions
        val entries = actions.map { option -> option.title }

        val builder = AlertDialog.Builder(context, R.style.AppDialogTheme)
            .setItems(entries.toTypedArray()) { _, which ->
                onOptionClicked(actions[which])
            }

        return builder.create()
    }

    private fun onOptionClicked(action: PropertyAction) {
        if (!viewModel.processAction(action)) {
            onActionClicked.invoke(action)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        subscribeToEvents()
    }

    private fun subscribeToEvents() {
        viewModel.openUrlEvent.observe(viewLifecycleOwner) { url ->
            openUrl(url)
        }
    }

    companion object {

        val TAG = PropertyActionDialog::class.java.simpleName

        private const val ARGUMENTS = "arguments"

        fun newInstance(
            args: PropertyActionDialogArgs
        ): PropertyActionDialog {
            return PropertyActionDialog()
                .withArguments {
                    putParcelable(ARGUMENTS, args)
                }
        }
    }
}