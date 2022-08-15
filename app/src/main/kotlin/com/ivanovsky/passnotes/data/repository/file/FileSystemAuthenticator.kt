package com.ivanovsky.passnotes.data.repository.file

import android.content.Context
import com.ivanovsky.passnotes.data.entity.FSAuthority
import com.ivanovsky.passnotes.data.entity.FSCredentials

interface FileSystemAuthenticator {
    fun getAuthType(): AuthType
    fun getFsAuthority(): FSAuthority
    fun isAuthenticationRequired(): Boolean
    fun startAuthActivity(context: Context)
    fun setCredentials(credentials: FSCredentials?)
}