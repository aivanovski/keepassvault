package com.ivanovsky.passnotes.domain

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.fragment.app.Fragment
import com.ivanovsky.passnotes.domain.entity.StoragePermissionType

class PermissionHelper(private val context: Context) {

    fun isPermissionGranted(permission: String): Boolean {
        var result = true

        if (Build.VERSION.SDK_INT >= 23) {
            result = context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED
        }

        return result
    }

    fun isAllFilesPermissionGranted(): Boolean {
        return if (Build.VERSION.SDK_INT >= 30) {
            Environment.isExternalStorageManager()
        } else {
            true
        }
    }

    fun requestPermission(activity: Activity, permission: String, requestCode: Int) {
        if (Build.VERSION.SDK_INT >= 23) {
            activity.requestPermissions(arrayOf(permission), requestCode)
        }
    }

    fun requestPermission(fragment: Fragment, permission: String, requestCode: Int) {
        if (Build.VERSION.SDK_INT >= 23) {
            fragment.requestPermissions(arrayOf(permission), requestCode)
        }
    }

    fun requestManageAllFilesPermission(fragment: Fragment, requestCode: Int) {
        if (Build.VERSION.SDK_INT >= 30) {
            fragment.startActivityForResult(
                Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION),
                requestCode
            )
        }
    }

    fun isAllGranted(grantResults: IntArray): Boolean {
        return grantResults.all { result -> result == PackageManager.PERMISSION_GRANTED }
    }

    fun hasFileAccessPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= 30) {
            isAllFilesPermissionGranted()
        } else {
            isPermissionGranted(SDCARD_PERMISSION)
        }
    }

    fun getRequiredFilePermissionType(): StoragePermissionType? {
        return when {
            hasFileAccessPermission() -> null
            Build.VERSION.SDK_INT >= 30 -> StoragePermissionType.ALL_FILES_ACCESS
            else -> StoragePermissionType.SDCARD_PERMISSION
        }
    }

    companion object {
        const val SDCARD_PERMISSION = Manifest.permission.WRITE_EXTERNAL_STORAGE
    }
}