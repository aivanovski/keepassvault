package com.ivanovsky.passnotes.domain

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import com.ivanovsky.passnotes.domain.entity.SystemPermission

class PermissionHelper(private val context: Context) {

    fun isPermissionGranted(permission: SystemPermission): Boolean {
        if (permission.minSdk != null && Build.VERSION.SDK_INT < permission.minSdk) {
            return true
        }

        return when (permission) {
            SystemPermission.ALL_FILES_PERMISSION -> isAllFilesPermissionGranted()

            else -> {
                val result = context.checkSelfPermission(permission.permission)
                result == PackageManager.PERMISSION_GRANTED
            }
        }
    }

    fun isAllGranted(grantResults: IntArray): Boolean {
        return grantResults.all { result -> result == PackageManager.PERMISSION_GRANTED }
    }

    fun hasFileAccessPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= 30) {
            isPermissionGranted(SystemPermission.ALL_FILES_PERMISSION)
        } else {
            isPermissionGranted(SystemPermission.SDCARD_PERMISSION)
        }
    }

    fun getRequiredFilePermission(): SystemPermission? {
        return when {
            hasFileAccessPermission() -> null
            Build.VERSION.SDK_INT >= 30 -> SystemPermission.ALL_FILES_PERMISSION
            else -> SystemPermission.SDCARD_PERMISSION
        }
    }

    private fun isAllFilesPermissionGranted(): Boolean {
        return if (Build.VERSION.SDK_INT >= 30) {
            Environment.isExternalStorageManager()
        } else {
            true
        }
    }
}