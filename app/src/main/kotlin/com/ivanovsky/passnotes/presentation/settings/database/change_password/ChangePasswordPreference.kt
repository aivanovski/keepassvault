package com.ivanovsky.passnotes.presentation.settings.database.change_password

import android.content.Context
import android.util.AttributeSet
import androidx.preference.DialogPreference

class ChangePasswordPreference(
    context: Context,
    attrs: AttributeSet
) : DialogPreference(context, attrs) {

    init {
        isPersistent = false
    }
}