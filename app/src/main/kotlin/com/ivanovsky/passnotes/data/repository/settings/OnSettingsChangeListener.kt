package com.ivanovsky.passnotes.data.repository.settings

interface OnSettingsChangeListener {
    fun onSettingsChanged(pref: SettingsImpl.Pref)
}