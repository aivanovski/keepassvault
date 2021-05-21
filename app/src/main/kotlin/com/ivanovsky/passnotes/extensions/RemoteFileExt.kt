package com.ivanovsky.passnotes.extensions

import com.ivanovsky.passnotes.data.entity.FileDescriptor
import com.ivanovsky.passnotes.data.entity.RemoteFile

fun RemoteFile.toFileDescriptor(): FileDescriptor =
    FileDescriptor(
        fsAuthority = fsAuthority,
        path = remotePath,
        uid = uid,
        isDirectory = false,
        isRoot = false,
        modified = lastModificationTimestamp
    )