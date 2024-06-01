package com.ivanovsky.passnotes.extensions

import com.ivanovsky.passnotes.data.entity.FileDescriptor
import com.ivanovsky.passnotes.data.entity.FileId

fun FileId.toFileDescriptor(
    isDirectory: Boolean = false,
    isRoot: Boolean = false
): FileDescriptor {
    return FileDescriptor(
        fsAuthority = fsAuthority,
        path = path,
        uid = uid,
        name = name,
        isDirectory = isDirectory,
        isRoot = isRoot
    )
}