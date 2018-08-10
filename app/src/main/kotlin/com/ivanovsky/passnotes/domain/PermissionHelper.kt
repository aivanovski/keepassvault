package com.ivanovsky.passnotes.domain

import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.support.v4.app.Fragment

class PermissionHelper(private val context: Context) {

	fun isPermissionGranted(permission: String): Boolean {
		var result = true

		if (Build.VERSION.SDK_INT >= 23) {
			result = context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED
		}

		return result
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

	fun isAllGranted(grantResults: IntArray): Boolean {
		return grantResults.all { result -> result == PackageManager.PERMISSION_GRANTED }
	}
}
