package com.ivanovsky.passnotes.extensions

import com.ivanovsky.passnotes.data.entity.FileDescriptor
import com.ivanovsky.passnotes.data.entity.OperationResult
import com.ivanovsky.passnotes.data.repository.file.FileSystemProvider
import java.util.LinkedList

fun FileSystemProvider.listAllFiles(): OperationResult<List<FileDescriptor>> {
    val getRootResult = rootFile
    if (getRootResult.isFailed) {
        return getRootResult.mapError()
    }

    val root = getRootResult.getOrThrow()

    val queue = LinkedList<FileDescriptor>()
        .apply {
            add(root)
        }
    val allFiles = mutableListOf(root)

    while (queue.isNotEmpty()) {
        val dir = queue.poll() ?: continue

        val listFilesResult = listFiles(dir)
        if (listFilesResult.isFailed) {
            return listFilesResult.mapError()
        }

        val files = listFilesResult.getOrThrow()
        allFiles.addAll(files)

        val dirs = files.filter { file -> file.isDirectory }
        queue.addAll(dirs)
    }

    return OperationResult.success(allFiles)
}