package com.ivanovsky.passnotes.presentation.groups.dialog

import android.app.AlertDialog
import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import com.ivanovsky.passnotes.domain.ResourceHelper
import com.ivanovsky.passnotes.injection.Injector
import javax.inject.Inject

class ChooseOptionDialog : DialogFragment(), DialogInterface.OnClickListener {

    @Inject
    lateinit var resourceHelper: ResourceHelper

    lateinit var onItemClickListener: (itemIndex: Int) -> Unit

    private var title: String? = null
    private lateinit var entries: List<String>

    init {
        Injector.getInstance().appComponent.inject(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (savedInstanceState != null) {
            dismiss()
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder =  AlertDialog.Builder(context)
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