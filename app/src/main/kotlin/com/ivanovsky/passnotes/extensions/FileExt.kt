package com.ivanovsky.passnotes.extensions

import com.ivanovsky.passnotes.data.entity.FSAuthority
import com.ivanovsky.passnotes.data.entity.FileDescriptor
import com.ivanovsky.passnotes.util.FileUtils
import java.io.File

fun File.toFileDescriptor(fsAuthority: FSAuthority): FileDescriptor {
    return FileDescriptor(
        fsAuthority = fsAuthority,
        path = path,
        uid = path,
        name = FileUtils.getFileNameFromPath(path),
        isDirectory = isDirectory,
        isRoot = (path == FileUtils.ROOT_PATH),
        modified = lastModified()
    )
}