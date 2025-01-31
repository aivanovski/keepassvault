package com.ivanovsky.passnotes.data.repository.settings

fun interface OnSettingsChangeListener {
    fun onSettingsChanged(pref: SettingsImpl.Pref)
}