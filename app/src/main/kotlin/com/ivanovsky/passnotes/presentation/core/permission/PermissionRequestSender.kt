package com.ivanovsky.passnotes.presentation.core.permission

import com.ivanovsky.passnotes.domain.entity.SystemPermission

interface PermissionRequestSender {
    fun requestPermission(permission: SystemPermission)
}