package com.ivanovsky.passnotes.extensions

import com.ivanovsky.passnotes.data.entity.FSType
import com.ivanovsky.passnotes.data.entity.FileDescriptor
import com.ivanovsky.passnotes.data.entity.KeyType
import com.ivanovsky.passnotes.data.entity.UsedFile
import com.ivanovsky.passnotes.domain.ResourceProvider
import com.ivanovsky.passnotes.util.StringUtils.EMPTY

fun FileDescriptor.isSameFile(other: FileDescriptor): Boolean {
    return fsAuthority == other.fsAuthority && uid == other.uid
}

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
        isRoot = isRoot,
        addedTime = addedTime,
        lastAccessTime = lastAccessTime,
        keyType = keyType
    )

fun FileDescriptor.formatReadablePath(resourceProvider: ResourceProvider): String {
    return when (fsAuthority.type) {
        FSType.UNDEFINED -> {
            path
        }
        FSType.INTERNAL_STORAGE, FSType.EXTERNAL_STORAGE -> {
            path
        }
        FSType.WEBDAV -> {
            val url = fsAuthority.credentials?.formatReadableUrl() ?: EMPTY
            if (!isRoot) {
                url + path
            } else {
                url
            }
        }
        FSType.SAF -> {
            path
        }
        FSType.GIT -> {
            val url = fsAuthority.credentials?.formatReadableUrl() ?: EMPTY
            url + path
        }
        FSType.FAKE -> {
            val url = fsAuthority.credentials?.formatReadableUrl() ?: EMPTY
            url + path
        }
    }
}