package com.ivanovsky.passnotes.domain.interactor.server_login

import com.ivanovsky.passnotes.BuildConfig
import com.ivanovsky.passnotes.data.entity.FSCredentials
import java.util.UUID

class GetDebugCredentialsUseCase {

    fun getDebugWebDavCredentials(): FSCredentials.BasicCredentials? {
        if (!BuildConfig.DEBUG) return null

        val url = BuildConfig.DEBUG_WEB_DAV_URL
        val username = BuildConfig.DEBUG_WEB_DAV_USERNAME
        val password = BuildConfig.DEBUG_WEB_DAV_PASSWORD

        return if (url.isNotEmpty() || username.isNotEmpty() || password.isNotEmpty()) {
            FSCredentials.BasicCredentials(url, username, password)
        } else {
            null
        }
    }

    fun getDebugGitCredentials(): FSCredentials.GitCredentials? {
        if (!BuildConfig.DEBUG) return null

        val url = BuildConfig.DEBUG_GIT_URL
        return if (url.isNotEmpty()) {
            FSCredentials.GitCredentials(
                url = url,
                isSecretUrl = false,
                salt = UUID.randomUUID().toString()
            )
        } else {
            null
        }
    }
}