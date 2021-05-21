package com.ivanovsky.passnotes.extensions

import com.ivanovsky.passnotes.data.entity.FileDescriptor
import com.ivanovsky.passnotes.data.entity.UsedFile

fun FileDescriptor.toUsedFile(
    addedTime: Long,
    lastAccessTime: Long? = null
): UsedFile =
    UsedFile(
        fsAuthority = fsAuthority,
        filePath = path,
        fileUid = uid,
        addedTime = addedTime,
        lastAccessTime = lastAccessTime
    )