package com.ivanovsky.passnotes.data.repository.settings

import com.ivanovsky.passnotes.data.repository.settings.SettingsImpl.Pref
import com.ivanovsky.passnotes.domain.entity.LoggingType

interface Settings {
    var isExternalStorageCacheEnabled: Boolean
    var autoLockDelayInMs: Int
    var autoClearClipboardDelayInMs: Int
    var isLockNotificationVisible: Boolean
    var dropboxAuthToken: String?
    var loggingType: LoggingType
    fun initDefaultIfNeed(pref: Pref)
}