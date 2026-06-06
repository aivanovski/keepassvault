package com.ivanovsky.passnotes.presentation.core.dialog

import androidx.fragment.app.DialogFragment
import com.ivanovsky.passnotes.R

abstract class BaseDialogFragment : DialogFragment() {

    override fun getTheme(): Int = R.style.AppDialogTheme
}