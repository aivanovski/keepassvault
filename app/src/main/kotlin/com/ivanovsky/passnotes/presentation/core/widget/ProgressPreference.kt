package com.ivanovsky.passnotes.presentation.core.widget

import android.content.Context
import android.util.AttributeSet
import androidx.preference.Preference
import com.ivanovsky.passnotes.R

class ProgressPreference(
    context: Context,
    attrs: AttributeSet
) : Preference(context, attrs) {

    init {
        layoutResource = R.layout.widget_progress_preference
    }
}