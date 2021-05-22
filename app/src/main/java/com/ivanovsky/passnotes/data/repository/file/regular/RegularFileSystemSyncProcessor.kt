package com.ivanovsky.passnotes.data.repository.file.regular

import com.ivanovsky.passnotes.data.entity.FileDescriptor
import com.ivanovsky.passnotes.data.entity.OperationResult
import com.ivanovsky.passnotes.data.repository.file.FileSystemSyncProcessor
import com.ivanovsky.passnotes.data.repository.file.OnConflictStrategy
import com.ivanovsky.passnotes.data.repository.file.SyncStrategy
import com.ivanovsky.passnotes.data.repository.file.exception.IncorrectUseException

class RegularFileSystemSyncProcessor : FileSystemSyncProcessor {

    override fun getLocallyModifiedFiles(): MutableList<FileDescriptor> {
        return mutableListOf()
    }

    override fun process(
        file: FileDescriptor?,
        syncStrategy: SyncStrategy?,
        onConflictStrategy: OnConflictStrategy?
    ): OperationResult<FileDescriptor> {
        throw IncorrectUseException()
    }
}