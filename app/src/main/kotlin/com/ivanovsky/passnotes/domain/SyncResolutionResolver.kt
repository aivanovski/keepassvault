package com.ivanovsky.passnotes.domain

import com.ivanovsky.passnotes.data.entity.RequestedSyncResolution
import com.ivanovsky.passnotes.data.entity.SyncResolution
import com.ivanovsky.passnotes.util.isNewerThan

class SyncResolutionResolver {

    fun resolve(
        isLocalModified: Boolean,
        isRemoteModified: Boolean,
        requestedResolution: RequestedSyncResolution
    ): SyncResolution {
        if (requestedResolution != RequestedSyncResolution.NOT_SPECIFIED) {
            return when (requestedResolution) {
                RequestedSyncResolution.UPLOAD_LOCAL_FILE -> SyncResolution.UPLOAD_LOCAL
                RequestedSyncResolution.DOWNLOAD_REMOTE_FILE -> SyncResolution.DOWNLOAD_REMOTE
                else -> throw IllegalStateException()
            }
        }

        return when {
            isLocalModified && isRemoteModified -> SyncResolution.CONFLICT
            isLocalModified -> SyncResolution.UPLOAD_LOCAL
            isRemoteModified -> SyncResolution.DOWNLOAD_REMOTE
            else -> SyncResolution.NO_CHANGES
        }
    }

    fun isRemoteModified(
        lastDownloadTimestamp: Long?,
        remoteModifiedTimestamp: Long?
    ): Boolean {
        return remoteModifiedTimestamp.isNewerThan(lastDownloadTimestamp)
    }
}