package com.ivanovsky.passnotes.data.repository.file.webdav

import android.content.Context
import com.ivanovsky.passnotes.data.entity.FSAuthority
import com.ivanovsky.passnotes.data.entity.ServerCredentials
import com.ivanovsky.passnotes.data.repository.file.AuthType
import com.ivanovsky.passnotes.data.repository.file.FileSystemAuthenticator
import com.ivanovsky.passnotes.data.repository.file.exception.IncorrectUseException
import java.util.concurrent.atomic.AtomicReference

class WebdavAuthenticator(
    private val initialAuthority: FSAuthority
) : FileSystemAuthenticator {

    private val fsAuthority = AtomicReference(initialAuthority)

    override fun getAuthType() = AuthType.CREDENTIALS

    override fun getFsAuthority(): FSAuthority = fsAuthority.get()

    override fun isAuthenticationRequired(): Boolean {
        return initialAuthority.isRequireCredentials
    }

    override fun startAuthActivity(context: Context) {
        throw IncorrectUseException()
    }

    override fun setCredentials(credentials: ServerCredentials?) {
        if (!initialAuthority.isRequireCredentials) {
            throw IllegalStateException()
        }

        fsAuthority.set(
            fsAuthority.get().copy(
                credentials = credentials
            )
        )
    }
}