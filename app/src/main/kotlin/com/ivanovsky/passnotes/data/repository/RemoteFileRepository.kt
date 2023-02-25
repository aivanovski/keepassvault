package com.ivanovsky.passnotes.data.repository

import com.ivanovsky.passnotes.data.entity.FSAuthority
import com.ivanovsky.passnotes.data.entity.RemoteFile
import com.ivanovsky.passnotes.data.repository.db.dao.RemoteFileDao

class RemoteFileRepository(
    private val dao: RemoteFileDao
) {

    fun getAll(fsAuthority: FSAuthority): List<RemoteFile> {
        return dao.all
            .filter { file -> file.fsAuthority == fsAuthority }
    }

    fun findByUid(uid: String, fsAuthority: FSAuthority): RemoteFile? {
        return dao.all
            .firstOrNull { file -> file.uid == uid && file.fsAuthority == fsAuthority }
    }

    fun findByRemotePath(remotePath: String, fsAuthority: FSAuthority): RemoteFile? {
        return dao.all
            .firstOrNull { file ->
                file.remotePath == remotePath && file.fsAuthority == fsAuthority
            }
    }

    fun insert(file: RemoteFile) {
        val id = dao.insert(file)
        file.id = id
    }

    fun update(file: RemoteFile) {
        dao.update(file)
    }

    fun delete(id: Int) {
        dao.delete(id.toLong())
    }
}