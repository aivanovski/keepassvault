package com.ivanovsky.passnotes.extensions

import com.ivanovsky.passnotes.data.entity.FSCredentials
import com.ivanovsky.passnotes.util.UrlUtils

fun FSCredentials.formatReadableUrl(): String {
    return when (this) {
        is FSCredentials.BasicCredentials -> url
        is FSCredentials.GitCredentials -> {
            if (isSecretUrl) {
                UrlUtils.formatSecretUrl(url)
            } else {
                url
            }
        }
    }
}

fun FSCredentials.getUrl(): String {
    return when (this) {
        is FSCredentials.BasicCredentials -> url
        is FSCredentials.GitCredentials -> url
    }
}