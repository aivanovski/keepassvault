package com.ivanovsky.passnotes.data.repository.settings

import com.ivanovsky.passnotes.data.repository.settings.SettingsImpl.Pref

interface Settings {
    var isExternalStorageCacheEnabled: Boolean
    var autoLockDelayInMs: Int
    var autoClearClipboardDelayInMs: Int
    var isLockNotificationVisible: Boolean
    var dropboxAuthToken: String?
    fun initDefaultIfNeed(pref: Pref)
}