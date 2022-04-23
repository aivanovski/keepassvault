package com.ivanovsky.passnotes.extensions

import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.data.entity.FSType
import com.ivanovsky.passnotes.data.entity.FileDescriptor
import com.ivanovsky.passnotes.data.entity.KeyType
import com.ivanovsky.passnotes.data.entity.UsedFile
import com.ivanovsky.passnotes.domain.ResourceProvider

fun FileDescriptor.toUsedFile(
    addedTime: Long,
    lastAccessTime: Long? = null,
    keyType: KeyType = KeyType.PASSWORD
): UsedFile =
    UsedFile(
        fsAuthority = fsAuthority,
        filePath = path,
        fileUid = uid,
        fileName = name,
        addedTime = addedTime,
        lastAccessTime = lastAccessTime,
        keyType = keyType
    )

fun FileDescriptor.formatReadablePath(resourceProvider: ResourceProvider): String {
    return when (fsAuthority.type) {
        FSType.REGULAR_FS -> {
            path
        }
        FSType.DROPBOX -> {
            resourceProvider.getString(R.string.dropbox) + ":/" + path
        }
        FSType.WEBDAV -> {
            val url = fsAuthority.credentials?.serverUrl ?: ""
            url + path
        }
        FSType.SAF -> {
            path
        }
    }
}