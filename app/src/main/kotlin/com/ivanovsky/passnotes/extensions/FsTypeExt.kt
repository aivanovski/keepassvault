package com.ivanovsky.passnotes.extensions

import com.ivanovsky.passnotes.data.entity.FSType
import com.ivanovsky.passnotes.presentation.serverLogin.model.LoginType

fun FSType.isRequireSynchronization(): Boolean {
    return this.getLoginType() != null
}

fun FSType.getLoginType(): LoginType? {
    return when (this) {
        FSType.WEBDAV -> LoginType.USERNAME_PASSWORD
        FSType.GIT -> LoginType.GIT
        FSType.FAKE -> LoginType.USERNAME_PASSWORD
        else -> null
    }
}