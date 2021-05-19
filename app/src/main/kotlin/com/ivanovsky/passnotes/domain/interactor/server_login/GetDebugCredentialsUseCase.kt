package com.ivanovsky.passnotes.domain.interactor.server_login

import com.ivanovsky.passnotes.BuildConfig
import com.ivanovsky.passnotes.data.entity.ServerCredentials

class GetDebugCredentialsUseCase {

    fun getDebugWebDavCredentials(): ServerCredentials? {
        if (!BuildConfig.DEBUG) return null

        val url = BuildConfig.DEBUG_WEB_DAV_URL
        val username = BuildConfig.DEBUG_WEB_DAV_USERNAME
        val password = BuildConfig.DEBUG_WEB_DAV_PASSWORD

        return if (url.isNotEmpty() || username.isNotEmpty() || password.isNotEmpty()) {
            ServerCredentials(url, username, password)
        } else {
            null
        }
    }
}