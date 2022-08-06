package com.ivanovsky.passnotes.data.entity

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

sealed class FSCredentials : Parcelable {

    @Parcelize
    data class BasicCredentials(
        val url: String,
        val username: String,
        val password: String
    ) : FSCredentials()

    /**
     * Represents credentials for Git repository
     * @param url git server url (http/https)
     * @param isSecretUrl allows to hide access token inside url
     * @param salt random string, allows to address to specific repository
     * even if there are other repositories with the same url
     */
    @Parcelize
    data class GitCredentials(
        val url: String,
        val isSecretUrl: Boolean,
        val salt: String
    ) : FSCredentials()
}