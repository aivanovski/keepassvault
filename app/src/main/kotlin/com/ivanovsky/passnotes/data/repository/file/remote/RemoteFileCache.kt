package com.ivanovsky.passnotes.data.repository.file.remote

import com.ivanovsky.passnotes.data.entity.RemoteFile
import com.ivanovsky.passnotes.data.repository.RemoteFileRepository
import com.ivanovsky.passnotes.data.repository.file.FSType

class RemoteFileCache(
    private val repository: RemoteFileRepository,
    private val fsType: FSType
) {

    fun getByRemotePath(remotePath: String): RemoteFile? {
        return repository.findByRemotePathAndFsType(remotePath, fsType)
    }

    fun getByUid(uid: String): RemoteFile? {
        return repository.findByUidAndFsType(uid, fsType)
    }

    fun put(file: RemoteFile) {
        repository.insert(file)
    }

    fun update(file: RemoteFile) {
        repository.update(file)
    }

    fun getLocallyModifiedFiles(): List<RemoteFile> {
        return repository.getAllByFsType(fsType)
            .filter { it.isLocallyModified }
    }
}