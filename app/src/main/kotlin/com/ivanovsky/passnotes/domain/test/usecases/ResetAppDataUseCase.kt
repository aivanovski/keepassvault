package com.ivanovsky.passnotes.domain.test.usecases

import android.app.ActivityManager
import android.content.Context
import com.ivanovsky.passnotes.data.entity.OperationResult

class ResetAppDataUseCase(
    private val context: Context
) {
    fun resetApplicationData(): OperationResult<Unit> {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        activityManager.clearApplicationUserData()
        return OperationResult.success(Unit)
    }
}