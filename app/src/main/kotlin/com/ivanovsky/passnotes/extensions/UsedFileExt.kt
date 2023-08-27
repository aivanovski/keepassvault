package com.ivanovsky.passnotes.extensions

import com.ivanovsky.passnotes.data.entity.FileDescriptor
import com.ivanovsky.passnotes.data.entity.UsedFile

fun UsedFile.getFileDescriptor(): FileDescriptor =
    FileDescriptor(
        fsAuthority = fsAuthority,
        path = filePath,
        uid = fileUid,
        name = fileName,
        isDirectory = false,
        isRoot = isRoot,
        modified = null
    )

fun UsedFile.getKeyFileDescriptor(): FileDescriptor? {
    return if (keyFileFsAuthority != null &&
        keyFilePath != null &&
        keyFileUid != null &&
        keyFileName != null
    ) {
        FileDescriptor(
            fsAuthority = keyFileFsAuthority,
            path = keyFilePath,
            uid = keyFileUid,
            name = keyFileName,
            isDirectory = false,
            isRoot = false
        )
    } else {
        null
    }
}