package com.ivanovsky.passnotes.data.repository.file.git

import android.content.Context
import com.ivanovsky.passnotes.data.entity.FSAuthority
import com.ivanovsky.passnotes.data.entity.FSCredentials
import com.ivanovsky.passnotes.data.repository.file.AuthType
import com.ivanovsky.passnotes.data.repository.file.FileSystemAuthenticator
import com.ivanovsky.passnotes.data.repository.file.exception.IncorrectUseException
import java.util.concurrent.atomic.AtomicReference

class GitFileSystemAuthenticator(
    private val initialAuthority: FSAuthority
) : FileSystemAuthenticator {

    private val fsAuthority = AtomicReference(initialAuthority)

    override fun getAuthType(): AuthType {
        return AuthType.CREDENTIALS
    }

    override fun getFsAuthority(): FSAuthority {
        return fsAuthority.get()
    }

    override fun isAuthenticationRequired(): Boolean {
        return initialAuthority.isRequireCredentials
    }

    override fun startAuthActivity(context: Context) {
        throw IncorrectUseException()
    }

    override fun setCredentials(credentials: FSCredentials?) {
        fsAuthority.set(
            fsAuthority.get().copy(
                credentials = credentials
            )
        )
    }
}