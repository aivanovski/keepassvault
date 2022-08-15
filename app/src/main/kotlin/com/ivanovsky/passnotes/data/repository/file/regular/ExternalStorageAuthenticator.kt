package com.ivanovsky.passnotes.data.repository.file.regular

import android.content.Context
import android.os.Build
import com.ivanovsky.passnotes.data.entity.FSAuthority
import com.ivanovsky.passnotes.data.entity.FSCredentials
import com.ivanovsky.passnotes.data.repository.file.AuthType
import com.ivanovsky.passnotes.data.repository.file.FileSystemAuthenticator
import com.ivanovsky.passnotes.data.repository.file.exception.IncorrectUseException
import com.ivanovsky.passnotes.domain.PermissionHelper

class ExternalStorageAuthenticator(
    private val permissionHelper: PermissionHelper
) : FileSystemAuthenticator {

    override fun getAuthType(): AuthType {
        return if (Build.VERSION.SDK_INT >= 30) {
            AuthType.ALL_FILES_PERMISSION
        } else {
            AuthType.SDCARD_PERMISSION
        }
    }

    override fun getFsAuthority() = FSAuthority.EXTERNAL_FS_AUTHORITY

    override fun isAuthenticationRequired(): Boolean {
        return !permissionHelper.hasFileAccessPermission()
    }

    override fun startAuthActivity(context: Context) {
        throw IncorrectUseException()
    }

    override fun setCredentials(credentials: FSCredentials?) {
        throw IncorrectUseException()
    }
}