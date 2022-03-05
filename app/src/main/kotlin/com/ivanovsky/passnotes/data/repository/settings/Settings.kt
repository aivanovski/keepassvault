package com.ivanovsky.passnotes.data.repository.settings

import com.ivanovsky.passnotes.data.repository.settings.SettingsImpl.Pref
import com.ivanovsky.passnotes.domain.entity.SortDirection
import com.ivanovsky.passnotes.domain.entity.SortType

interface Settings {
    var isExternalStorageCacheEnabled: Boolean
    var isPostponedSyncEnabled: Boolean
    var autoLockDelayInMs: Int
    var autoClearClipboardDelayInMs: Int
    var isLockNotificationVisible: Boolean
    var dropboxAuthToken: String?
    var isFileLogEnabled: Boolean
    var sortType: SortType
    var sortDirection: SortDirection
    fun initDefaultIfNeed(pref: Pref)
    fun register(listener: OnSettingsChangeListener)
    fun unregister(listener: OnSettingsChangeListener)
}