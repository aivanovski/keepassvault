package com.ivanovsky.passnotes.data.repository.file.remote

import java.util.UUID
import java.util.concurrent.CopyOnWriteArrayList

class StatusMap {

    private val entries: MutableList<ProcessingUnit> = CopyOnWriteArrayList()

    fun getByFileUid(fileUid: String): ProcessingUnit? {
        return entries.firstOrNull { it.fileUid == fileUid }
    }

    fun getByRemotePath(remotePath: String): ProcessingUnit? {
        return entries.firstOrNull { it.remotePath == remotePath }
    }

    fun put(unit: ProcessingUnit) {
        entries.add(unit)
    }

    fun remove(processingUid: UUID) {
        val idx = entries.indexOfFirst { it.processingUid == processingUid }
        if (idx >= 0) {
            entries.removeAt(idx)
        }
    }
}