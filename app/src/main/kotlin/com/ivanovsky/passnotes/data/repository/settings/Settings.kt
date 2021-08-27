package com.ivanovsky.passnotes.data.repository.settings

interface Settings {
    var isExternalStorageCacheEnabled: Boolean
    var autoLockDelayInMs: Int
    var isLockNotificationVisible: Boolean
    var dropboxAuthToken: String?
}