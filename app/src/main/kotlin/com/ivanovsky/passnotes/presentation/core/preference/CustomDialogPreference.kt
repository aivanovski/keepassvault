package com.ivanovsky.passnotes.presentation.core.preference

import android.content.Context
import android.util.AttributeSet
import androidx.preference.DialogPreference

class CustomDialogPreference(
    context: Context,
    attrs: AttributeSet
) : DialogPreference(context, attrs) {

    init {
        isPersistent = false
    }
}