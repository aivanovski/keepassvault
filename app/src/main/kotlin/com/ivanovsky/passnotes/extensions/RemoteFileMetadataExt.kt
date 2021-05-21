package com.ivanovsky.passnotes.extensions

import com.ivanovsky.passnotes.data.entity.FSAuthority
import com.ivanovsky.passnotes.data.entity.FileDescriptor
import com.ivanovsky.passnotes.data.entity.RemoteFileMetadata
import com.ivanovsky.passnotes.util.DateUtils
import com.ivanovsky.passnotes.util.FileUtils

fun RemoteFileMetadata.toFileDescriptor(fsAuthority: FSAuthority): FileDescriptor =
    FileDescriptor(
        fsAuthority = fsAuthority,
        path = path,
        uid = uid,
        isDirectory = false,
        isRoot = false,
        modified = DateUtils.anyLastTimestamp(serverModified, clientModified)
    )