package com.ivanovsky.passnotes.data.repository.file.dropbox

import android.content.Context
import android.content.Intent
import com.dropbox.core.android.Auth
import com.ivanovsky.passnotes.BuildConfig
import com.ivanovsky.passnotes.data.entity.FSAuthority
import com.ivanovsky.passnotes.data.entity.ServerCredentials
import com.ivanovsky.passnotes.data.repository.SettingsRepository
import com.ivanovsky.passnotes.data.repository.file.AuthType
import com.ivanovsky.passnotes.data.repository.file.FileSystemAuthenticator
import com.ivanovsky.passnotes.data.repository.file.exception.IncorrectUseException

class DropboxAuthenticator(
    private val settings: SettingsRepository
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

    override fun getAuthIntent(context: Context): Intent? {
        throw IncorrectUseException()
    }

    override fun getAuthorityFromResult(intent: Intent): FSAuthority? {
        throw IncorrectUseException()
    }

    override fun setCredentials(credentials: ServerCredentials?) {
        throw IncorrectUseException()
    }
}