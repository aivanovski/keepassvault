package com.ivanovsky.passnotes.data.repository.file.remote

import com.ivanovsky.passnotes.data.entity.FSAuthority
import com.ivanovsky.passnotes.data.entity.RemoteFile
import com.ivanovsky.passnotes.data.repository.RemoteFileRepository

class RemoteFileCache(
    private val repository: RemoteFileRepository,
    private val fsAuthority: FSAuthority
) {

    fun getByRemotePath(remotePath: String): RemoteFile? {
        return repository.findByRemotePath(remotePath, fsAuthority)
    }

    fun getByUid(uid: String): RemoteFile? {
        return repository.findByUid(uid, fsAuthority)
    }

    fun put(file: RemoteFile) {
        repository.insert(file)
    }

    fun update(file: RemoteFile) {
        repository.update(file)
    }

    fun getLocallyModifiedFiles(): List<RemoteFile> {
        return repository.getAll(fsAuthority)
            .filter { it.isLocallyModified }
    }
}