package com.ivanovsky.passnotes.presentation.core.extensions

import androidx.annotation.StringRes
import androidx.preference.PreferenceFragmentCompat

fun PreferenceFragmentCompat.throwPreferenceNotFound(@StringRes keyId: Int): Nothing {
    val key = getString(keyId)
    throw IllegalStateException("Unable to find preference by key: $key")
}