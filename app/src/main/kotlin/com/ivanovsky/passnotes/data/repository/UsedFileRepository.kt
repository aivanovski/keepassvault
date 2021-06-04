package com.ivanovsky.passnotes.data.repository

import com.ivanovsky.passnotes.data.ObserverBus
import com.ivanovsky.passnotes.data.entity.FSAuthority
import com.ivanovsky.passnotes.data.entity.UsedFile
import com.ivanovsky.passnotes.data.repository.db.dao.UsedFileDao

class UsedFileRepository(
    private val dao: UsedFileDao,
    private val bus: ObserverBus
) {

    fun getAll(): List<UsedFile> {
        return dao.all
    }

    fun findByUid(fileUid: String, fsAuthority: FSAuthority): UsedFile? {
        return dao.all
            .firstOrNull { file: UsedFile? -> fileUid == file!!.fileUid && fsAuthority == file.fsAuthority }
    }

    fun insert(file: UsedFile): UsedFile {
        val id = dao.insert(file)

        bus.notifyUsedFileDataSetChanged()

        return file.copy(id = id.toInt())
    }

    fun update(file: UsedFile) {
        if (file.id == null) return

        dao.update(file)

        bus.notifyUsedFileContentChanged(file.id)
    }

    fun remove(id: Int) {
        dao.remove(id)

        bus.notifyUsedFileDataSetChanged()
    }
}