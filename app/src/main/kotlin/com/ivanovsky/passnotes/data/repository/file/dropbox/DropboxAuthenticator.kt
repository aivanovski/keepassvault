package com.ivanovsky.passnotes.data.repository.file.dropbox

import android.content.Context
import com.dropbox.core.android.Auth
import com.ivanovsky.passnotes.BuildConfig
import com.ivanovsky.passnotes.data.entity.FSAuthority
import com.ivanovsky.passnotes.data.entity.ServerCredentials
import com.ivanovsky.passnotes.data.repository.file.AuthType
import com.ivanovsky.passnotes.data.repository.file.FileSystemAuthenticator
import com.ivanovsky.passnotes.data.repository.file.exception.IncorrectUseException
import com.ivanovsky.passnotes.data.repository.settings.Settings

class DropboxAuthenticator(
    private val settings: Settings
) : FileSystemAuthenticator {

    fun getAuthToken(): String? {
        var token = Auth.getOAuth2Token()
        if (token == null && settings.dropboxAuthToken != null) {
            token = settings.dropboxAuthToken
        } else if (token != null) {
            settings.dropboxAuthToken = token
        }
        return token
    }

    override fun getAuthType() = AuthType.EXTERNAL

    override fun getFsAuthority() = FSAuthority.DROPBOX_FS_AUTHORITY

    override fun isAuthenticationRequired(): Boolean {
        return Auth.getOAuth2Token() == null && settings.dropboxAuthToken == null
    }

    override fun startAuthActivity(context: Context) {
        Auth.startOAuth2Authentication(context, BuildConfig.DROPBOX_APP_KEY)
    }

    override fun setCredentials(credentials: ServerCredentials?) {
        throw IncorrectUseException()
    }
}