package com.ivanovsky.passnotes.domain

import android.content.Context
import androidx.annotation.StringRes

class ResourceHelper(private val context: Context) {

	fun getString(@StringRes resId: Int): String {
		return context.getString(resId)
	}
}