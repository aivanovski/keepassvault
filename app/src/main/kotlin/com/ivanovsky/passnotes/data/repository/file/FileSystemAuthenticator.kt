package com.ivanovsky.passnotes.data.repository.file

import android.content.Context
import android.content.Intent
import com.ivanovsky.passnotes.data.entity.FSAuthority
import com.ivanovsky.passnotes.data.entity.ServerCredentials

interface FileSystemAuthenticator {
    fun getAuthType(): AuthType
    fun getFsAuthority(): FSAuthority
    fun isAuthenticationRequired(): Boolean
    fun startAuthActivity(context: Context)
    fun setCredentials(credentials: ServerCredentials?)
    fun getAuthIntent(context: Context): Intent?
    fun getAuthorityFromResult(intent: Intent): FSAuthority?
}