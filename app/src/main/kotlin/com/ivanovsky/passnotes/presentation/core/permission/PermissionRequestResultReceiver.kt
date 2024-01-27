package com.ivanovsky.passnotes.presentation.core.permission

import com.ivanovsky.passnotes.domain.entity.SystemPermission

interface PermissionRequestResultReceiver {
    fun onPermissionRequestResult(
        permission: SystemPermission,
        isGranted: Boolean
    )
}