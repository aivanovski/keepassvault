package com.ivanovsky.passnotes.data.repository

import com.ivanovsky.passnotes.data.entity.TemporaryFile
import com.ivanovsky.passnotes.data.repository.db.dao.TemporaryFileDao

class TemporaryFileRepository(
    private val dao: TemporaryFileDao
) {

    fun getAll() =
        dao.getAll()

    fun insert(file: TemporaryFile) =
        dao.insert(file)

    fun getByPath(path: String) =
        dao.getByPath(path)

    fun removeByPath(path: String) =
        dao.removeByPath(path)
}