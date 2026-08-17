package com.ivanovsky.passnotes.data.repository.file.regular

import android.content.Context
import com.ivanovsky.passnotes.data.entity.FSAuthority
import com.ivanovsky.passnotes.data.entity.FSCredentials
import com.ivanovsky.passnotes.data.repository.file.AuthType
import com.ivanovsky.passnotes.data.repository.file.FileSystemAuthenticator
import com.ivanovsky.passnotes.data.repository.file.exception.IncorrectUseException

class InternalStorageAuthenticator(
    private val fsAuthority: FSAuthority
) : FileSystemAuthenticator {

    override fun getAuthType(): AuthType = AuthType.NO_AUTH

    override fun getFsAuthority(): FSAuthority = fsAuthority

    override fun isAuthenticationRequired(): Boolean = false

    override fun startAuthActivity(context: Context) {
        throw IncorrectUseException()
    }

    override fun setCredentials(credentials: FSCredentials?) {
        throw IncorrectUseException()
    }
}