package com.ivanovsky.passnotes.data.entity

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Represent credentials for file systems that requires authentication
 * (for example WebDav server, Git repository)
 */
sealed class FSCredentials : Parcelable {

    @Parcelize
    data class BasicCredentials(
        val url: String,
        val username: String,
        val password: String
    ) : FSCredentials()

    /**
     * Represents credentials for Git repository via HTTPS protocol
     * @param url git server url
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

    /**
     * Represents credentials for Git repository via SSH protocol
     * @param url git server url
     * @param isSecretUrl allows to hide url
     * @param keyFile SSH key file
     * @param salt random string, allows to address to specific repository
     * @param password for [keyFile]
     */
    @Parcelize
    data class SshCredentials(
        val url: String,
        val isSecretUrl: Boolean,
        val keyFile: FileId,
        val salt: String,
        val password: String
    ) : FSCredentials()
}