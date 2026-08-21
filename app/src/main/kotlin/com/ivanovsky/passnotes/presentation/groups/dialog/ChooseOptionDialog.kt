package com.ivanovsky.passnotes.presentation.groups.dialog

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.presentation.core.dialog.BaseDialogFragment

@Deprecated("Use OptionDialog instead")
class ChooseOptionDialog : BaseDialogFragment(), DialogInterface.OnClickListener {

    lateinit var onItemClickListener: (itemIndex: Int) -> Unit

    private var title: String? = null
    private lateinit var entries: List<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (savedInstanceState != null) {
            dismiss()
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = MaterialAlertDialogBuilder(requireContext(), R.style.AppDialogTheme)
            .setItems(entries.toTypedArray(), this)

        if (title != null) {
            builder.setTitle(title)
        }

        return builder.create()
    }

    override fun onClick(dialog: DialogInterface?, which: Int) {
        onItemClickListener.invoke(which)
    }

    companion object {
        val TAG = ChooseOptionDialog::class.java.simpleName

        fun newInstance(title: String?, entries: List<String>): ChooseOptionDialog {
            val dialog = ChooseOptionDialog()
            dialog.title = title
            dialog.entries = entries
            return dialog
        }
    }
}