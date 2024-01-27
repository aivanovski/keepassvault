package com.ivanovsky.passnotes.domain.entity

import android.Manifest
import android.os.Build
import com.ivanovsky.passnotes.util.StringUtils.EMPTY

enum class SystemPermission(
    val permission: String,
    val requestCode: Int,
    val minSdk: Int?
) {
    NOTIFICATION(
        permission = if (Build.VERSION.SDK_INT >= 33) {
            Manifest.permission.POST_NOTIFICATIONS
        } else {
            EMPTY
        },
        requestCode = 1001,
        minSdk = 33
    ),
    SDCARD_PERMISSION(
        permission = Manifest.permission.WRITE_EXTERNAL_STORAGE,
        requestCode = 1002,
        minSdk = null
    ),
    ALL_FILES_PERMISSION(
        permission = EMPTY,
        requestCode = 1003,
        minSdk = 30
    );

    companion object {

    }
}