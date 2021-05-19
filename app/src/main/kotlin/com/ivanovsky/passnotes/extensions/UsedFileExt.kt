package com.ivanovsky.passnotes.extensions

import com.ivanovsky.passnotes.data.entity.FileDescriptor
import com.ivanovsky.passnotes.data.entity.UsedFile

fun UsedFile.toFileDescriptor(): FileDescriptor =
    FileDescriptor(
        fsAuthority = fsAuthority,
        path = filePath,
        uid = fileUid,
        isDirectory = false,
        isRoot = false,
        modified = null
    )