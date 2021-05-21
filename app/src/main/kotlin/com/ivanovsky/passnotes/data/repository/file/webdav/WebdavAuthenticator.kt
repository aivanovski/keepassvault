package com.ivanovsky.passnotes.data.repository.file.webdav

import android.content.Context
import android.content.Intent
import com.ivanovsky.passnotes.data.entity.FSAuthority
import com.ivanovsky.passnotes.data.entity.ServerCredentials
import com.ivanovsky.passnotes.data.repository.file.AuthType
import com.ivanovsky.passnotes.data.repository.file.FileSystemAuthenticator
import com.ivanovsky.passnotes.data.repository.file.exception.IncorrectUseException
import com.ivanovsky.passnotes.presentation.server_login.ServerLoginActivity
import com.ivanovsky.passnotes.presentation.server_login.ServerLoginArgs
import java.util.concurrent.atomic.AtomicReference

class WebdavAuthenticator(
    private val initialAuthority: FSAuthority
) : FileSystemAuthenticator {

    private val fsAuthority = AtomicReference<FSAuthority>(initialAuthority)

    override fun getAuthType() = AuthType.INTERNAL

    override fun getFsAuthority(): FSAuthority = fsAuthority.get()

    override fun isAuthenticationRequired(): Boolean {
        return initialAuthority.isRequireCredentials
    }

    override fun startAuthActivity(context: Context) {
        throw IncorrectUseException()
    }

    override fun getAuthIntent(context: Context): Intent? {
        return ServerLoginActivity.createStartIntent(
            context,
            args = ServerLoginArgs(
                fsAuthority = initialAuthority
            )
        )
    }

    override fun getAuthorityFromResult(intent: Intent): FSAuthority? {
        return intent.extras?.getParcelable(ServerLoginActivity.EXTRA_RESULT)
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