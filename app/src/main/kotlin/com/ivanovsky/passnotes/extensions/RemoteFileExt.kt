package com.ivanovsky.passnotes.extensions

import com.ivanovsky.passnotes.data.entity.FileDescriptor
import com.ivanovsky.passnotes.data.entity.RemoteFile
import com.ivanovsky.passnotes.util.FileUtils

fun RemoteFile.toFileDescriptor(): FileDescriptor =
    FileDescriptor(
        fsAuthority = fsAuthority,
        path = remotePath,
        uid = uid,
        name = FileUtils.getFileNameFromPath(remotePath),
        isDirectory = false,
        isRoot = false,
        modified = lastModificationTimestamp
    )